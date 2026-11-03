package com.screenrecorder.app.core.capability

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.view.Display
import android.view.WindowManager
import com.screenrecorder.app.core.model.FpsCapability
import com.screenrecorder.app.core.model.FpsOption
import com.screenrecorder.app.core.model.RecordingResolution
import kotlin.math.roundToInt

class DeviceCapabilityChecker(private val context: Context) {

    fun calculateResolution(): RecordingResolution {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val bounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            val displayMetrics = context.resources.displayMetrics
            android.graphics.Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
        }

        val rawWidth = bounds.width().coerceAtLeast(1)
        val rawHeight = bounds.height().coerceAtLeast(1)

        val isPortrait = rawWidth <= rawHeight
        val (finalWidth, finalHeight) = if (isPortrait) {
            val scaledHeight = (1080.0 * rawHeight / rawWidth).roundToInt()
            val alignedHeight = (scaledHeight / 16) * 16
            Pair(1080, alignedHeight.coerceAtLeast(1080))
        } else {
            val scaledWidth = (1080.0 * rawWidth / rawHeight).roundToInt()
            val alignedWidth = (scaledWidth / 16) * 16
            Pair(alignedWidth.coerceAtLeast(1080), 1080)
        }

        return RecordingResolution(width = finalWidth, height = finalHeight)
    }

    fun getMaxDisplayRefreshRate(): Float {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        }

        val supportedModes = display?.supportedModes ?: emptyArray()
        val maxFromModes = supportedModes.maxOfOrNull { it.refreshRate }
        return maxFromModes ?: display?.refreshRate ?: 60f
    }

    fun checkFpsCapabilities(resolution: RecordingResolution): List<FpsCapability> {
        val maxRefreshRate = getMaxDisplayRefreshRate()
        val encoderCapabilities = getPreferredVideoEncoderCapabilities(resolution.width, resolution.height)

        return FpsOption.entries.map { option ->
            val isDisplaySupported = maxRefreshRate >= (option.fps - 1.0f)
            val isEncoderSupported = encoderCapabilities?.areSizeAndRateSupported(
                resolution.width,
                resolution.height,
                option.fps.toDouble()
            ) ?: (option.fps == 60)

            val isSupported = isDisplaySupported && isEncoderSupported
            val reason = when {
                !isDisplaySupported -> "Layar perangkat maksimal ${maxRefreshRate.roundToInt()}Hz"
                !isEncoderSupported -> "Hardware encoder tidak mendukung ${option.fps} FPS"
                else -> null
            }

            FpsCapability(
                option = option,
                isSupported = isSupported,
                reason = reason
            )
        }
    }

    private fun getPreferredVideoEncoderCapabilities(
        width: Int,
        height: Int
    ): MediaCodecInfo.VideoCapabilities? {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val mimeTypes = listOf(MediaFormat.MIMETYPE_VIDEO_AVC, MediaFormat.MIMETYPE_VIDEO_HEVC)

        for (mime in mimeTypes) {
            for (info in codecList.codecInfos) {
                if (!info.isEncoder) continue
                val caps = runCatching { info.getCapabilitiesForType(mime) }.getOrNull() ?: continue
                val videoCaps = caps.videoCapabilities ?: continue
                if (videoCaps.isSizeSupported(width, height)) {
                    return videoCaps
                }
            }
        }
        return null
    }
}

