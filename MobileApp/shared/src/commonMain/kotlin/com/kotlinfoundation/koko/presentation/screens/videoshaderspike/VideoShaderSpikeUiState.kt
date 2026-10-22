package com.kotlinfoundation.koko.presentation.screens.videoshaderspike

class VideoShaderSpikeUiState

sealed interface VideoShaderSpikeUiEvent {
    data object OnClick : VideoShaderSpikeUiEvent
}
