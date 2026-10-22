package com.kotlinfoundation.koko.presentation.screens.videoshaderspike

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlinfoundation.koko.designsystem.components.ScreenWithToolbar

@Composable
fun VideoShaderSpikeScreen(
    modifier: Modifier = Modifier,
    viewModel: VideoShaderSpikeViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    VideoShaderSpikeScreen(
        modifier = modifier.fillMaxSize(),
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
    )
}

@Composable
fun VideoShaderSpikeScreen(
    modifier: Modifier = Modifier,
    uiState: VideoShaderSpikeUiState,
    onUiEvent: (VideoShaderSpikeUiEvent) -> Unit,
) {
    ScreenWithToolbar(
        modifier = modifier,
        isScrollableContent = false,
        title = "Video Shader Spike",
        includeBottomInsets = true, // Set to false if bottom nav is visible
    ) {
        VideoShaderPreview(modifier = Modifier.fillMaxSize())
    }
}
