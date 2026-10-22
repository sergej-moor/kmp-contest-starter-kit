package com.kotlinfoundation.koko.presentation.screens.videoshaderspike

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceFormat
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.CoreMedia.CMVideoFormatDescriptionGetDimensions

/**
 * One recording format this device's back camera actually supports — mirrors the Android
 * `VideoFormatOption`/`queryAvailableVideoFormats` pair: real width/height read from the device,
 * never assumed, with a GCD-reduced aspect ratio label (same idea as the Android side, kept
 * separate here since `AVCaptureDeviceFormat` isn't a KMP-shareable type).
 */
internal data class VideoFormatOption(
    val format: AVCaptureDeviceFormat,
    val width: Int,
    val height: Int,
    val aspectLabel: String,
) {
    val aspectRatio: Float get() = width.toFloat() / height.toFloat()
    val qualityLabel: String get() = "$width×$height"
}

@OptIn(ExperimentalForeignApi::class)
internal fun queryAvailableVideoFormats(): List<VideoFormatOption> {
    val device = AVCaptureDevice.defaultDeviceWithDeviceType(
        deviceType = AVCaptureDeviceTypeBuiltInWideAngleCamera,
        mediaType = AVMediaTypeVideo,
        position = AVCaptureDevicePositionBack,
    ) ?: return emptyList()

    @Suppress("UNCHECKED_CAST")
    val formats = device.formats as List<AVCaptureDeviceFormat>

    return formats.mapNotNull { format ->
        val formatDescription = format.formatDescription ?: return@mapNotNull null
        val dimensions = CMVideoFormatDescriptionGetDimensions(formatDescription)
        val width = dimensions.useContents { width }
        val height = dimensions.useContents { height }
        if (width <= 0 || height <= 0) return@mapNotNull null
        VideoFormatOption(format, width, height, aspectLabel(width, height))
    }.distinctBy { it.width to it.height }
}

private fun aspectLabel(width: Int, height: Int): String {
    val divisor = gcd(width, height)
    return "${width / divisor}:${height / divisor}"
}

private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
