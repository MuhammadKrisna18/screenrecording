package com.screenrecorder.app.core.capability

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.view.Display
import com.screenrecorder.app.core.model.FpsCapability
import com.screenrecorder.app.core.model.FpsOption
import com.screenrecorder.app.core.model.RecordingResolution
import kotlin.math.roundToInt

class DeviceCapabilityChecker(private val context: Context) {

    fun calculateResolution(): RecordingResolution {
        return runCatching {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)

            val rawWidth: Int
            val rawHeight: Int

            val mode = display?.mode
            if (mode != null && mode.physicalWidth > 0 && mode.physicalHeight > 0) {
                rawWidth = mode.physicalWidth
                rawHeight = mode.physicalHeight
            } else {
                val displayMetrics = context.resources.displayMetrics
                rawWidth = displayMetrics.widthPixels
                rawHeight = displayMetrics.heightPixels
            }

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

            RecordingResolution(width = finalWidth, height = finalHeight)
        }.getOrDefault(RecordingResolution(1080, 1920))
    }

    fun getMaxDisplayRefreshRate(): Float {
        return runCatching {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            val supportedModes = display?.supportedModes ?: emptyArray()
            val maxFromModes = supportedModes.maxOfOrNull { it.refreshRate }
            maxFromModes ?: display?.refreshRate ?: 60f
        }.getOrDefault(60f)
    }

    fun checkFpsCapabilities(resolution: RecordingResolution): List<FpsCapability> {
        val maxRefreshRate = getMaxDisplayRefreshRate()
        val encoderCapabilities = runCatching {
            getPreferredVideoEncoderCapabilities(resolution.width, resolution.height)
        }.getOrNull()

        return FpsOption.entries.map { option ->
            val isDisplaySupported = maxRefreshRate >= (option.fps - 1.0f)
            val isEncoderSupported = runCatching {
                encoderCapabilities?.areSizeAndRateSupported(
                    resolution.width,
                    resolution.height,
                    option.fps.toDouble()
                )
            }.getOrNull() ?: (option.fps == 60)

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
        return runCatching {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val mimeTypes = listOf(MediaFormat.MIMETYPE_VIDEO_AVC, MediaFormat.MIMETYPE_VIDEO_HEVC)

            for (mime in mimeTypes) {
                for (info in codecList.codecInfos) {
                    if (!info.isEncoder) continue
                    val caps = runCatching { info.getCapabilitiesForType(mime) }.getOrNull() ?: continue
                    val videoCaps = caps.videoCapabilities ?: continue
                    val isSizeSupported = runCatching { videoCaps.isSizeSupported(width, height) }.getOrDefault(false)
                    if (isSizeSupported) {
                        return@runCatching videoCaps
                    }
                }
            }
            null
        }.getOrNull()
    }
}
