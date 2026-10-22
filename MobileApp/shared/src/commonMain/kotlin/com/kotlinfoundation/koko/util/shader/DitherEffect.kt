package com.kotlinfoundation.koko.util.shader

/**
 * Shared definition of the ordered (Bayer 4x4) dither effect — the one source of truth for its
 * numeric parameters, consumed by both shader dialects this project uses: AGSL
 * ([com.kotlinfoundation.koko.presentation.screens.shadercamspike.CameraShaders], Compose
 * RenderEffect preview) and GLSL ES ([VideoShaders], CameraX `CameraEffect` capture pipeline).
 * Only the parameters are shared — AGSL and GLSL ES are different languages, so each shader's
 * surrounding syntax stays platform-specific.
 */
internal object DitherEffect {
    /** 4x4 Bayer threshold matrix, row-major (cell index = y * 4 + x). */
    val BAYER_4X4 = intArrayOf(
        0, 8, 2, 10,
        12, 4, 14, 6,
        3, 11, 1, 9,
        15, 7, 13, 5,
    )

    const val LUMINANCE_R = 0.299
    const val LUMINANCE_G = 0.587
    const val LUMINANCE_B = 0.114

    /**
     * Both AGSL and GLSL ES 1.00 lack an array-constructor literal (`float[16](...)`) and
     * disallow a runtime-computed array index — discovered by trial and error against a real
     * device (see the KotlinConf-Ideen doc). A branch chain over the constant cell index sidesteps
     * both restrictions and works in either dialect, so it's generated once from [BAYER_4X4]
     * instead of hand-duplicated per shader.
     */
    fun buildBayerBranchChain(indexVar: String, outVar: String): String {
        val branches = BAYER_4X4.dropLast(1).mapIndexed { idx, value ->
            val keyword = if (idx == 0) "if" else "else if"
            "$keyword ($indexVar == $idx) { $outVar = $value.0; }"
        }
        val fallback = "else { $outVar = ${BAYER_4X4.last()}.0; }"
        return (branches + fallback).joinToString(separator = "\n            ")
    }
}
