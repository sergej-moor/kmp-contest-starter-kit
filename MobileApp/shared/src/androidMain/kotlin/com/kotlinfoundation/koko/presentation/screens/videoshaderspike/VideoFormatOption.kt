package com.kotlinfoundation.koko.presentation.screens.videoshaderspike

import android.util.Size
import androidx.camera.core.CameraInfo
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector

/**
 * One recording format this specific device's camera actually supports — [quality] maps 1:1 to
 * a CameraX `Quality` preset (which also determines bitrate), [size] and [aspectLabel] are its
 * real resolution and reduced aspect ratio ("16:9", "4:3", ...), both read from the device, never
 * assumed. Two devices can report a different list; the UI only ever shows what this call returns.
 */
internal data class VideoFormatOption(
    val quality: Quality,
    val size: Size,
    val aspectLabel: String,
) {
    val aspectRatio: Float get() = size.width.toFloat() / size.height.toFloat()
    val qualityLabel: String get() = "${quality.label()} ${size.width}×${size.height}"
}

/** Queries this device's camera for every `Quality` it actually supports, with real resolutions. */
internal fun queryAvailableVideoFormats(cameraInfo: CameraInfo): List<VideoFormatOption> = QualitySelector.getSupportedQualities(cameraInfo).mapNotNull { quality ->
    val size = QualitySelector.getResolution(cameraInfo, quality) ?: return@mapNotNull null
    VideoFormatOption(quality, size, aspectLabel(size))
}

private fun aspectLabel(size: Size): String {
    val divisor = gcd(size.width, size.height)
    return "${size.width / divisor}:${size.height / divisor}"
}

private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

private fun Quality.label(): String = when (this) {
    Quality.UHD -> "UHD 4K"
    Quality.FHD -> "Full HD"
    Quality.HD -> "HD"
    Quality.SD -> "SD"
    else -> toString()
}
