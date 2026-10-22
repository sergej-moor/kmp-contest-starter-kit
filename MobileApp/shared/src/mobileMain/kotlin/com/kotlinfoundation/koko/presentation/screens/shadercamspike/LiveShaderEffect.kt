package com.kotlinfoundation.koko.presentation.screens.shadercamspike

import androidx.compose.ui.Modifier

/**
 * Applies [CameraShaders.ORDERED_DITHER] to whatever this modifier's content renders.
 * Android: AGSL `RuntimeShader` (API 33+, no-op below that). iOS: not ported yet — passthrough.
 */
expect fun Modifier.liveDitherShaderEffect(): Modifier
