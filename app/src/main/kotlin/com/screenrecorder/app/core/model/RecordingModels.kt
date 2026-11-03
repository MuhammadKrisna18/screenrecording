package com.screenrecorder.app.core.model

enum class FpsOption(val fps: Int, val label: String, val bitrate: Int) {
    FPS_60(60, "60 FPS", 18_000_000),
    FPS_90(90, "90 FPS", 28_000_000),
    FPS_120(120, "120 FPS", 40_000_000)
}

data class FpsCapability(
    val option: FpsOption,
    val isSupported: Boolean,
    val reason: String? = null
)

data class RecordingResolution(
    val width: Int,
    val height: Int
) {
    val displayLabel: String
        get() = "${width}x${height} (1080p)"
}

sealed interface RecordingStatus {
    data object Idle : RecordingStatus
    data object Starting : RecordingStatus
    data class Recording(val durationMs: Long) : RecordingStatus
    data class Stopped(val outputUri: String?) : RecordingStatus
    data class Error(val message: String) : RecordingStatus
}

