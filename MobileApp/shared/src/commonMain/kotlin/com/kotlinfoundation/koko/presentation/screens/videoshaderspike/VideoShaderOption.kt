package com.kotlinfoundation.koko.presentation.screens.videoshaderspike

/**
 * Which fragment shader the live-encoding pipeline applies. Resolution/aspect-ratio options are
 * NOT modeled here — they're read from the device's actual camera capabilities at runtime (the
 * Android actual queries `QualitySelector.getSupportedQualities`/`getResolution`, which aren't
 * KMP-shareable types), not from a fixed, possibly-unavailable-on-this-device list.
 */
internal enum class VideoShaderOption(val label: String) {
    NONE("Kein Filter"),
    DITHER("Dither"),
}
