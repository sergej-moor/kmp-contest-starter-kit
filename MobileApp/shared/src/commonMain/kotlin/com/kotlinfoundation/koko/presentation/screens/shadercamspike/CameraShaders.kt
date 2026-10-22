package com.kotlinfoundation.koko.presentation.screens.shadercamspike

import com.kotlinfoundation.koko.util.shader.DitherEffect

/**
 * Shader sources for the shader-camera feasibility spike, written in the SkSL dialect so
 * platform actuals can share one string: Android consumes it as AGSL (a strict SkSL subset,
 * via [android.graphics.RuntimeShader] on API 33+); a future iOS/Desktop actual would feed the
 * identical source to Skiko's RuntimeEffect. Keep any new shader in that common subset.
 *
 * The dither effect's numeric parameters (Bayer matrix, luminance weights) live once in
 * [DitherEffect] and are shared with [com.kotlinfoundation.koko.presentation.screens.videoshaderspike.VideoShaders]'
 * GLSL ES version — only the surrounding shader-language syntax differs per platform.
 */
internal object CameraShaders {
    /**
     * Uniform name the live camera content is bound under. Must match the second argument
     * passed to `RenderEffect.createRuntimeShaderEffect` on the Android actual.
     */
    const val CONTENT_UNIFORM = "content"

    /** Ordered (Bayer 4x4) black-and-white dither of whatever [CONTENT_UNIFORM] renders. */
    val ORDERED_DITHER = """
        uniform shader $CONTENT_UNIFORM;

        half4 main(float2 fragCoord) {
            half4 color = $CONTENT_UNIFORM.eval(fragCoord);
            half luminance = half(dot(color.rgb, half3(${DitherEffect.LUMINANCE_R}, ${DitherEffect.LUMINANCE_G}, ${DitherEffect.LUMINANCE_B})));

            int x = int(mod(fragCoord.x, 4.0));
            int y = int(mod(fragCoord.y, 4.0));
            int idx = y * 4 + x;

            // AGSL forbids both array-constructor literals and runtime-indexed array access
            // (array indices must be compile-time constants), so the 4x4 Bayer matrix is a
            // branch chain over the constant cell index instead of a lookup table.
            float bayerValue;
            ${DitherEffect.buildBayerBranchChain(indexVar = "idx", outVar = "bayerValue")}

            half threshold = half((bayerValue + 0.5) / 16.0);
            half dithered = luminance > threshold ? half(1.0) : half(0.0);

            return half4(dithered, dithered, dithered, color.a);
        }
    """.trimIndent()
}
