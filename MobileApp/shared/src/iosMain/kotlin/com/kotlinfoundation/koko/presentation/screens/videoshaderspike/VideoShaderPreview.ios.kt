package com.kotlinfoundation.koko.presentation.screens.videoshaderspike

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// AVFoundation/Metal port follows once a Mac is available — see the KotlinConf-Ideen doc.
@Composable
actual fun VideoShaderPreview(modifier: Modifier) {
    Text(text = "Live-Video-Encoding läuft bisher nur auf Android.", modifier = modifier)
}
