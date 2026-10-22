package com.kotlinfoundation.koko.presentation.screens.videoshaderspike

import com.kotlinfoundation.koko.util.shader.DitherEffect

/**
 * Real GLSL ES shader sources for the CameraX `CameraEffect` capture pipeline (step 3/4 of the
 * live-encoding spike) — distinct from `CameraShaders`' AGSL source used by the preview-only
 * spike screen. CameraX's `SurfaceProcessor` draws with plain OpenGL ES, not AGSL/RenderEffect,
 * so the camera texture here is a `samplerExternalOES`, not a `shader` uniform. The dither
 * effect's numeric parameters are shared with the AGSL version via [DitherEffect].
 */
internal object VideoShaders {
    const val VERTEX_SHADER = """
        attribute vec4 aPosition;
        attribute vec4 aTexCoord;
        uniform mat4 uTexMatrix;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = aPosition;
            vTexCoord = (uTexMatrix * aTexCoord).xy;
        }
    """

    /** Step 3: 1:1 passthrough — validates the EGL/OES wiring before any shader math is added. */
    const val FRAGMENT_SHADER_PASSTHROUGH = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTexCoord;
        uniform samplerExternalOES uTexture;
        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoord);
        }
    """

    /**
     * Step 4: the real effect — ordered (Bayer 4x4) black-and-white dither, baked into both the
     * live preview and the recorded video by CameraX's `CameraEffect` pipeline. `gl_FragCoord` is
     * the GLSL ES built-in for the output pixel position (AGSL's `fragCoord` parameter equivalent).
     *
     * [blockSize] groups that many real output pixels into one dither cell (1 = one dither cell
     * per pixel, the original fine-grained pattern). A coarser grid has far less high-frequency
     * detail for H.264's block-based compression to mangle — the harsh compression artifacts seen
     * on recorded dithered video are the direct motivation for making this adjustable.
     */
    fun ditherFragmentShader(blockSize: Int): String {
        require(blockSize >= 1) { "blockSize must be >= 1, was $blockSize" }
        return """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTexCoord;
            uniform samplerExternalOES uTexture;

            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                float luminance = dot(color.rgb, vec3(${DitherEffect.LUMINANCE_R}, ${DitherEffect.LUMINANCE_G}, ${DitherEffect.LUMINANCE_B}));

                int x = int(mod(floor(gl_FragCoord.x / $blockSize.0), 4.0));
                int y = int(mod(floor(gl_FragCoord.y / $blockSize.0), 4.0));
                int idx = y * 4 + x;

                float bayerValue;
                ${DitherEffect.buildBayerBranchChain(indexVar = "idx", outVar = "bayerValue")}

                float threshold = (bayerValue + 0.5) / 16.0;
                float dithered = luminance > threshold ? 1.0 : 0.0;

                gl_FragColor = vec4(dithered, dithered, dithered, color.a);
            }
        """.trimIndent()
    }
}
