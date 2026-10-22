package com.kotlinfoundation.koko.presentation.screens.shadercamspike

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

actual fun Modifier.liveDitherShaderEffect(): Modifier {
    // RuntimeShader/RenderEffect need Android 13+ (API 33) — older devices get the plain preview.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this

    val shader = RuntimeShader(CameraShaders.ORDERED_DITHER)
    val renderEffect = RenderEffect
        .createRuntimeShaderEffect(shader, CameraShaders.CONTENT_UNIFORM)
        .asComposeRenderEffect()

    return graphicsLayer { this.renderEffect = renderEffect }
}
