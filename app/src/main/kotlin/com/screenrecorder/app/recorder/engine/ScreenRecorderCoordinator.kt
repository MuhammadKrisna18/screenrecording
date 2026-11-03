package com.screenrecorder.app.recorder.engine

import android.content.Context
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.Build
import com.screenrecorder.app.core.model.FpsOption
import com.screenrecorder.app.core.model.RecordingResolution
import com.screenrecorder.app.core.storage.MediaStorageHelper
import java.util.concurrent.atomic.AtomicBoolean

class ScreenRecorderCoordinator(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val resolution: RecordingResolution,
    private val fpsOption: FpsOption,
    private val dpi: Int
) {
    private val storageHelper = MediaStorageHelper(context)
    private var muxerCoordinator: MuxerCoordinator? = null
    private var videoEngine: VideoEncoderEngine? = null
    private var audioEngine: AudioCaptureEngine? = null
    private var currentUri: Uri? = null
    private val isRecording = AtomicBoolean(false)

    fun start(): Boolean {
        val storagePair = storageHelper.createVideoOutputUri() ?: return false
        val (uri, pfd) = storagePair
        currentUri = uri

        val muxer = MuxerCoordinator(pfd.fileDescriptor)
        muxerCoordinator = muxer

        val video = VideoEncoderEngine(
            width = resolution.width,
            height = resolution.height,
            fps = fpsOption.fps,
            bitrate = fpsOption.bitrate,
            dpi = dpi,
            mediaProjection = mediaProjection,
            muxerCoordinator = muxer
        )
        videoEngine = video

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val audio = AudioCaptureEngine(
                mediaProjection = mediaProjection,
                muxerCoordinator = muxer
            )
            audioEngine = audio
            muxer.setExpectAudio(true)
            audio.start()
        } else {
            muxer.setExpectAudio(false)
        }

        video.start()
        isRecording.set(true)
        return true
    }

    fun stop(): Uri? {
        if (!isRecording.getAndSet(false)) return null

        runCatching { audioEngine?.stop() }
        audioEngine = null

        runCatching { videoEngine?.stop() }
        videoEngine = null

        runCatching { muxerCoordinator?.release() }
        muxerCoordinator = null

        runCatching { mediaProjection.stop() }

        val uri = currentUri
        if (uri != null) {
            storageHelper.finalizeVideo(uri)
        }
        currentUri = null
        return uri
    }
}

