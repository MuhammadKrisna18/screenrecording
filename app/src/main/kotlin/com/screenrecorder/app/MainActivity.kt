package com.screenrecorder.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.screenrecorder.app.ui.recording.RecordingScreen
import com.screenrecorder.app.ui.recording.RecordingViewModel
import com.screenrecorder.app.ui.theme.ScreenRecorderTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RecordingViewModel by viewModels()

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.startRecordingWithCountdown(result.resultCode, result.data!!, this)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (recordAudioGranted) {
            launchScreenCapture()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ScreenRecorderTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by viewModel.uiState.collectAsStateWithLifecycle()

                    RecordingScreen(
                        state = state,
                        onSelectFps = { viewModel.selectFps(it) },
                        onStartRecordingClick = { checkAndRequestPermissions() },
                        onStopRecordingClick = { viewModel.stopRecording(this) },
                        onCancelCountdownClick = { viewModel.cancelCountdown() },
                        onDownloadUpdateClick = { viewModel.downloadAndInstallUpdate() },
                        onInstallUpdateClick = { viewModel.installReadyUpdate(it) },
                        onDismissUpdateClick = { viewModel.dismissUpdate() },
                        onErrorDismiss = { viewModel.dismissError() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadCapabilities()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            launchScreenCapture()
        }
    }

    private fun launchScreenCapture() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = projectionManager.createScreenCaptureIntent()
        projectionLauncher.launch(intent)
    }
}
