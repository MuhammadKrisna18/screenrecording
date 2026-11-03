package com.screenrecorder.app.recorder.engine

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.FileDescriptor
import java.nio.ByteBuffer

class MuxerCoordinator(fileDescriptor: FileDescriptor) {

    private val muxer = MediaMuxer(fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private val lock = Any()

    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var isStarted = false
    private var hasAudio = true

    fun setExpectAudio(expectAudio: Boolean) {
        synchronized(lock) {
            hasAudio = expectAudio
            checkStartCondition()
        }
    }

    fun addVideoTrack(format: MediaFormat) {
        synchronized(lock) {
            if (videoTrackIndex != -1) return
            videoTrackIndex = muxer.addTrack(format)
            checkStartCondition()
        }
    }

    fun addAudioTrack(format: MediaFormat) {
        synchronized(lock) {
            if (audioTrackIndex != -1) return
            audioTrackIndex = muxer.addTrack(format)
            checkStartCondition()
        }
    }

    private fun checkStartCondition() {
        val canStart = if (hasAudio) {
            videoTrackIndex != -1 && audioTrackIndex != -1
        } else {
            videoTrackIndex != -1
        }

        if (canStart && !isStarted) {
            muxer.start()
            isStarted = true
        }
    }

    fun writeVideoSample(buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        synchronized(lock) {
            if (!isStarted || videoTrackIndex == -1) return
            muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo)
        }
    }

    fun writeAudioSample(buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        synchronized(lock) {
            if (!isStarted || audioTrackIndex == -1) return
            muxer.writeSampleData(audioTrackIndex, buffer, bufferInfo)
        }
    }

    fun release() {
        synchronized(lock) {
            if (isStarted) {
                runCatching { muxer.stop() }
                isStarted = false
            }
            runCatching { muxer.release() }
        }
    }
}

