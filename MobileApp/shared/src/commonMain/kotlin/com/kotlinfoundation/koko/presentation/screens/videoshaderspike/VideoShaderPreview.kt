package com.kotlinfoundation.koko.presentation.screens.videoshaderspike

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Camera preview with start/stop video recording. Step 2 of the live-encoding spike: raw
 * capture only, no shader yet — isolates whether the CameraX binding itself works before the
 * GL effect is layered on top. Android-only for now (see the KotlinConf-Ideen doc for status);
 * other targets show a short explainer.
 */
@Composable
expect fun VideoShaderPreview(modifier: Modifier = Modifier)
