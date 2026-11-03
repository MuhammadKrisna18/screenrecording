package com.screenrecorder.app.ui.recording

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.screenrecorder.app.core.capability.DeviceCapabilityChecker
import com.screenrecorder.app.core.model.FpsOption
import com.screenrecorder.app.core.model.RecordingStatus
import com.screenrecorder.app.core.updater.AppUpdateManager
import com.screenrecorder.app.core.updater.UpdateState
import com.screenrecorder.app.recorder.service.RecordingStateHolder
import com.screenrecorder.app.recorder.service.ScreenRecordService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val capabilityChecker = DeviceCapabilityChecker(application)
    private val updateManager = AppUpdateManager(application)
    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    init {
        loadCapabilities()
        observeRecordingState()
        checkForUpdates()
    }

    fun loadCapabilities() {
        runCatching {
            val resolution = capabilityChecker.calculateResolution()
            val maxRefreshRate = capabilityChecker.getMaxDisplayRefreshRate()
            val capabilities = capabilityChecker.checkFpsCapabilities(resolution)

            val initialFps = if (capabilities.any { it.option == _uiState.value.selectedFps && it.isSupported }) {
                _uiState.value.selectedFps
            } else {
                capabilities.firstOrNull { it.isSupported }?.option ?: FpsOption.FPS_60
            }

            _uiState.update {
                it.copy(
                    resolution = resolution,
                    maxDisplayRefreshRate = maxRefreshRate,
                    fpsCapabilities = capabilities,
                    selectedFps = initialFps
                )
            }
        }
    }

    private fun observeRecordingState() {
        viewModelScope.launch {
            RecordingStateHolder.status.collect { status ->
                val durationFormatted = when (status) {
                    is RecordingStatus.Recording -> formatDuration(status.durationMs)
                    else -> "00:00"
                }

                val errorMsg = when (status) {
                    is RecordingStatus.Error -> status.message
                    else -> null
                }

                _uiState.update {
                    it.copy(
                        recordingStatus = status,
                        formattedDuration = durationFormatted,
                        errorMessage = errorMsg ?: it.errorMessage
                    )
                }
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(updateState = UpdateState.Checking) }
            val updateInfo = updateManager.checkForUpdate()
            if (updateInfo != null) {
                _uiState.update { it.copy(updateState = UpdateState.UpdateAvailable(updateInfo)) }
            } else {
                _uiState.update { it.copy(updateState = UpdateState.Idle) }
            }
        }
    }

    fun downloadAndInstallUpdate() {
        val currentState = _uiState.value.updateState
        if (currentState !is UpdateState.UpdateAvailable) return

        viewModelScope.launch {
            _uiState.update { it.copy(updateState = UpdateState.Downloading(0f)) }
            val downloadedFile = updateManager.downloadApk(currentState.updateInfo.downloadUrl) { progress ->
                _uiState.update { it.copy(updateState = UpdateState.Downloading(progress)) }
            }

            if (downloadedFile != null) {
                _uiState.update { it.copy(updateState = UpdateState.ReadyToInstall(downloadedFile)) }
                updateManager.promptInstall(downloadedFile)
            } else {
                _uiState.update { it.copy(updateState = UpdateState.Error("Gagal mengunduh file pembaruan")) }
            }
        }
    }

    fun installReadyUpdate(file: File) {
        updateManager.promptInstall(file)
    }

    fun dismissUpdate() {
        _uiState.update { it.copy(updateState = UpdateState.Idle) }
    }

    fun selectFps(option: FpsOption) {
        val capability = _uiState.value.fpsCapabilities.find { it.option == option }
        if (capability?.isSupported == true) {
            _uiState.update { it.copy(selectedFps = option) }
        }
    }

    fun startRecording(resultCode: Int, data: Intent, context: Context) {
        val state = _uiState.value
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordService.EXTRA_RESULT_DATA, data)
            putExtra(ScreenRecordService.EXTRA_FPS, state.selectedFps.fps)
            putExtra(ScreenRecordService.EXTRA_WIDTH, state.resolution.width)
            putExtra(ScreenRecordService.EXTRA_HEIGHT, state.resolution.height)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        RecordingStateHolder.updateStatus(RecordingStatus.Starting)
    }

    fun stopRecording(context: Context) {
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
}
