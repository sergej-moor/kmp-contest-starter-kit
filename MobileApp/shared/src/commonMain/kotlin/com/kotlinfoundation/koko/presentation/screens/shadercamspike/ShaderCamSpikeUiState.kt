package com.kotlinfoundation.koko.presentation.screens.shadercamspike

class ShaderCamSpikeUiState

sealed interface ShaderCamSpikeUiEvent {
    data object OnClick : ShaderCamSpikeUiEvent
}
