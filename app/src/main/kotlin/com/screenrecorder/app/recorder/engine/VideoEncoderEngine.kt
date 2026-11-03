package com.screenrecorder.app.recorder.engine

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.view.Surface
import java.util.concurrent.atomic.AtomicBoolean

class VideoEncoderEngine(
    private val width: Int,
    private val height: Int,
    private val fps: Int,
    private val bitrate: Int,
    private val dpi: Int,
    private val mediaProjection: MediaProjection,
    private val muxerCoordinator: MuxerCoordinator
) {
    private val isRunning = AtomicBoolean(false)
    private var videoCodec: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var drainThread: Thread? = null

    fun start() {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_CAPTURE_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        videoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = createInputSurface()
            start()
        }

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenRecorder-Display",
            width,
            height,
            dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            inputSurface,
            null,
            null
        )

        isRunning.set(true)
        drainThread = Thread {
            val bufferInfo = MediaCodec.BufferInfo()
            while (isRunning.get()) {
                val codec = videoCodec ?: break
                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                when {
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        muxerCoordinator.addVideoTrack(codec.outputFormat)
                    }
                    outputBufferIndex >= 0 -> {
                        val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                muxerCoordinator.writeVideoSample(outputBuffer, bufferInfo)
                            }
                        }
                        codec.releaseOutputBuffer(outputBufferIndex, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }
            }
        }.apply { start() }
    }

    fun stop() {
        isRunning.set(false)
        runCatching {
            videoCodec?.signalEndOfInputStream()
        }
        runCatching { drainThread?.join(1000) }

        runCatching {
            virtualDisplay?.release()
        }
        virtualDisplay = null

        runCatching {
            videoCodec?.stop()
            videoCodec?.release()
        }
        videoCodec = null

        runCatching {
            inputSurface?.release()
        }
        inputSurface = null
    }
}

