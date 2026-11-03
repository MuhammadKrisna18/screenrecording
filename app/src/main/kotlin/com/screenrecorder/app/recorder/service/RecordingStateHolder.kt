package com.screenrecorder.app.recorder.service

import com.screenrecorder.app.core.model.RecordingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RecordingStateHolder {
    private val _status = MutableStateFlow<RecordingStatus>(RecordingStatus.Idle)
    val status: StateFlow<RecordingStatus> = _status.asStateFlow()

    fun updateStatus(newStatus: RecordingStatus) {
        _status.value = newStatus
    }
}

