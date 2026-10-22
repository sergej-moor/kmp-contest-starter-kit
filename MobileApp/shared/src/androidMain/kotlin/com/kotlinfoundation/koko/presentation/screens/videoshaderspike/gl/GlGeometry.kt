package com.kotlinfoundation.koko.presentation.screens.videoshaderspike.gl

/**
 * Computes the quad geometry for a frame given three aspect ratios: the camera's native image,
 * the user-selected target (crop), and the output canvas (e.g. a 16:9 Quality-preset encoder
 * surface). CameraX's `Recorder` only offers fixed-resolution `Quality` presets, not arbitrary
 * aspect ratios, so "9:16"/"1:1" selection is implemented here instead: crop the sampled texture
 * region to the target ratio, then letterbox/pillarbox that content centered within whatever
 * shape the canvas actually is.
 */
internal object GlGeometry {

    /** Texture coordinates (matching [GlUtil]'s BL/BR/TL/TR triangle-strip order) cropped to [targetAspect]. */
    fun cropTexCoords(cameraAspect: Float, targetAspect: Float): FloatArray {
        var uMin = 0f
        var uMax = 1f
        var vMin = 0f
        var vMax = 1f
        when {
            cameraAspect > targetAspect -> {
                // Camera image is wider than the target crop — trim left/right.
                val scale = targetAspect / cameraAspect
                val margin = (1f - scale) / 2f
                uMin = margin
                uMax = 1f - margin
            }

            cameraAspect < targetAspect -> {
                // Camera image is narrower (taller) than the target crop — trim top/bottom.
                val scale = cameraAspect / targetAspect
                val margin = (1f - scale) / 2f
                vMin = margin
                vMax = 1f - margin
            }
        }
        return floatArrayOf(
            uMin,
            vMin,
            uMax,
            vMin,
            uMin,
            vMax,
            uMax,
            vMax,
        )
    }

    /** Clip-space quad positions, scaled so [targetAspect] content fits centered in [canvasAspect] (black bars for the rest). */
    fun letterboxPositions(targetAspect: Float, canvasAspect: Float): FloatArray {
        var xScale = 1f
        var yScale = 1f
        when {
            targetAspect > canvasAspect -> {
                // Content wider than the canvas — fit width, pad top/bottom.
                yScale = canvasAspect / targetAspect
            }

            targetAspect < canvasAspect -> {
                // Content narrower than the canvas — fit height, pad left/right.
                xScale = targetAspect / canvasAspect
            }
        }
        return floatArrayOf(
            -xScale,
            -yScale,
            xScale,
            -yScale,
            -xScale,
            yScale,
            xScale,
            yScale,
        )
    }
}
