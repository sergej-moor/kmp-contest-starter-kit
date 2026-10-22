package com.kotlinfoundation.koko.presentation.screens.videoshaderspike.ios

import platform.CoreImage.CIColor
import platform.CoreImage.CIFilter
import platform.CoreImage.CIImage
import platform.CoreImage.filterWithName
import platform.Foundation.setValue

/**
 * Grayscale + dither chain built entirely from CoreImage's built-in filters — no custom Metal
 * kernel, which keeps the whole iOS pipeline buildable/linkable by the Windows-based Kotlin/Native
 * compiler and the CI link-check alone (a hand-written `.ci.metal` kernel needs Xcode-specific
 * build rules the lightweight CI job doesn't run). `CIDither` only exposes an `inputIntensity`
 * knob, not the adjustable Bayer-grid size the Android GLSL shader has — the closest honest
 * equivalent given no Mac is available yet to tune a custom kernel visually.
 */
internal class DitherFilterChain {
    private val grayscaleFilter = requireNotNull(CIFilter.filterWithName("CIColorMonochrome")) {
        "CIColorMonochrome filter unavailable"
    }
    private val ditherFilter = requireNotNull(CIFilter.filterWithName("CIDither")) {
        "CIDither filter unavailable"
    }

    init {
        val white = CIColor.colorWithRed(red = 1.0, green = 1.0, blue = 1.0)
        grayscaleFilter.setValue(white, forKey = "inputColor")
        grayscaleFilter.setValue(1.0, forKey = "inputIntensity")
    }

    /** [intensity] mirrors the Android dither block-size picker conceptually — 0..1, CIDither's own range. */
    fun apply(image: CIImage, intensity: Double): CIImage {
        ditherFilter.setValue(intensity, forKey = "inputIntensity")

        grayscaleFilter.setValue(image, forKey = "inputImage")
        val grayscale = grayscaleFilter.outputImage ?: image

        ditherFilter.setValue(grayscale, forKey = "inputImage")
        return ditherFilter.outputImage ?: grayscale
    }
}
