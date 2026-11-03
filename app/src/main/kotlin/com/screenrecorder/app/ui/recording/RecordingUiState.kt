package com.screenrecorder.app.ui.recording

import com.screenrecorder.app.core.model.FpsCapability
import com.screenrecorder.app.core.model.FpsOption
import com.screenrecorder.app.core.model.RecordingResolution
import com.screenrecorder.app.core.model.RecordingStatus
import com.screenrecorder.app.core.updater.UpdateState

data class RecordingUiState(
    val resolution: RecordingResolution = RecordingResolution(1080, 1920),
    val maxDisplayRefreshRate: Float = 60f,
    val fpsCapabilities: List<FpsCapability> = emptyList(),
    val selectedFps: FpsOption = FpsOption.FPS_60,
    val recordingStatus: RecordingStatus = RecordingStatus.Idle,
    val formattedDuration: String = "00:00",
    val updateState: UpdateState = UpdateState.Idle,
    val errorMessage: String? = null
)
