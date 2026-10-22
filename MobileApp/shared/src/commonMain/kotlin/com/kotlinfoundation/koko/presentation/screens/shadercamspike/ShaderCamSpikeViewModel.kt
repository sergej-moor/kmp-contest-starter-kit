package com.kotlinfoundation.koko.presentation.screens.shadercamspike

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShaderCamSpikeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ShaderCamSpikeUiState())
    val uiState: StateFlow<ShaderCamSpikeUiState> = _uiState.asStateFlow()

    fun onUiEvent(event: ShaderCamSpikeUiEvent) {
        when (event) {
            ShaderCamSpikeUiEvent.OnClick -> TODO()
        }
    }
}
