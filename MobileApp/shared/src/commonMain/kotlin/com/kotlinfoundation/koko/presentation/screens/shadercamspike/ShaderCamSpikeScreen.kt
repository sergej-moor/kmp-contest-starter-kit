package com.kotlinfoundation.koko.presentation.screens.shadercamspike

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlinfoundation.koko.designsystem.components.ScreenWithToolbar

@Composable
fun ShaderCamSpikeScreen(
    modifier: Modifier = Modifier,
    viewModel: ShaderCamSpikeViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ShaderCamSpikeScreen(
        modifier = modifier.fillMaxSize(),
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
    )
}

@Composable
fun ShaderCamSpikeScreen(
    modifier: Modifier = Modifier,
    uiState: ShaderCamSpikeUiState,
    onUiEvent: (ShaderCamSpikeUiEvent) -> Unit,
) {
    ScreenWithToolbar(
        modifier = modifier,
        isScrollableContent = false,
        title = "Shader Cam Spike",
        includeBottomInsets = true, // Set to false if bottom nav is visible
    ) {
        CameraShaderContent(modifier = Modifier.fillMaxSize())
    }
}
