package com.kotlinfoundation.koko.presentation.screens.shadercamspike

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun CameraShaderContent(modifier: Modifier) {
    Text(
        text = "Der Shader-Kamera-Spike läuft nur auf Android und iOS.",
        modifier = modifier,
    )
}
