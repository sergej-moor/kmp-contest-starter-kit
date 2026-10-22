package com.kotlinfoundation.koko.presentation.screens.shadercamspike

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Live camera preview with [CameraShaders.ORDERED_DITHER] applied in real time.
 * Mobile-only (Android/iOS, see the `mobile` actual) — other targets show a short explainer.
 */
@Composable
expect fun CameraShaderContent(modifier: Modifier = Modifier)
