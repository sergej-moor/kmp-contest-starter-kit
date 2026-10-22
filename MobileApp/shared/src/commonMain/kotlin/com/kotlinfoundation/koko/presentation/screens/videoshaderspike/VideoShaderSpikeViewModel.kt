package com.kotlinfoundation.koko.presentation.screens.videoshaderspike

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VideoShaderSpikeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VideoShaderSpikeUiState())
    val uiState: StateFlow<VideoShaderSpikeUiState> = _uiState.asStateFlow()

    fun onUiEvent(event: VideoShaderSpikeUiEvent) {
        when (event) {
            VideoShaderSpikeUiEvent.OnClick -> TODO()
        }
    }
}
