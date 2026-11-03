package com.screenrecorder.app.recorder.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.screenrecorder.app.core.model.FpsOption
import com.screenrecorder.app.core.model.RecordingResolution
import com.screenrecorder.app.core.model.RecordingStatus
import com.screenrecorder.app.core.notification.RecordingNotificationManager
import com.screenrecorder.app.recorder.engine.ScreenRecorderCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class ScreenRecordService : Service() {

    private lateinit var notificationManager: RecordingNotificationManager
    private var coordinator: ScreenRecorderCoordinator? = null
    private var mediaProjection: MediaProjection? = null
    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var startTimeMs = 0L

    override fun onCreate() {
        super.onCreate()
        notificationManager = RecordingNotificationManager(this)
        notificationManager.createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_STOP -> handleStop()
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        val fpsValue = intent.getIntExtra(EXTRA_FPS, 60)
        val width = intent.getIntExtra(EXTRA_WIDTH, 1080)
        val height = intent.getIntExtra(EXTRA_HEIGHT, 1920)

        if (resultCode == 0 || resultData == null) {
            RecordingStateHolder.updateStatus(RecordingStatus.Error("Izin perekaman layar tidak valid"))
            stopSelf()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                RecordingNotificationManager.NOTIFICATION_ID,
                notificationManager.buildNotification("00:00"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(
                RecordingNotificationManager.NOTIFICATION_ID,
                notificationManager.buildNotification("00:00")
            )
        }

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, resultData)

        if (projection == null) {
            RecordingStateHolder.updateStatus(RecordingStatus.Error("Gagal menginisialisasi MediaProjection"))
            stopForegroundService()
            return
        }

        mediaProjection = projection

        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                handleStop()
            }
        }, Handler(Looper.getMainLooper()))

        val fpsOption = FpsOption.entries.firstOrNull { it.fps == fpsValue } ?: FpsOption.FPS_60
        val resolution = RecordingResolution(width, height)
        val dpi = resources.displayMetrics.densityDpi

        val recordCoordinator = ScreenRecorderCoordinator(
            context = this,
            mediaProjection = projection,
            resolution = resolution,
            fpsOption = fpsOption,
            dpi = dpi
        )
        coordinator = recordCoordinator

        val started = recordCoordinator.start()
        if (!started) {
            RecordingStateHolder.updateStatus(RecordingStatus.Error("Gagal memulai encoder rekaman layar"))
            stopForegroundService()
            return
        }

        startTimeMs = System.currentTimeMillis()
        RecordingStateHolder.updateStatus(RecordingStatus.Recording(0L))
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - startTimeMs
                val seconds = (elapsed / 1000) % 60
                val minutes = (elapsed / (1000 * 60)) % 60
                val hours = elapsed / (1000 * 60 * 60)

                val formatted = if (hours > 0) {
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                }

                notificationManager.updateNotification(formatted)
                RecordingStateHolder.updateStatus(RecordingStatus.Recording(elapsed))
            }
        }
    }

    private fun handleStop() {
        timerJob?.cancel()
        timerJob = null

        val outputUri = coordinator?.stop()
        coordinator = null
        mediaProjection = null

        RecordingStateHolder.updateStatus(RecordingStatus.Stopped(outputUri?.toString()))
        stopForegroundService()
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        timerJob?.cancel()
        coordinator?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.screenrecorder.app.action.START"
        const val ACTION_STOP = "com.screenrecorder.app.action.STOP"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_WIDTH = "extra_width"
        const val EXTRA_HEIGHT = "extra_height"
    }
}

