package com.example.composeexamples.viewmodel

import androidx.lifecycle.ViewModel
import com.example.composeexamples.data.ComposeFeature
import com.example.composeexamples.data.ComposeFeatureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class ComposeFeatureUiState(
    val features: List<ComposeFeature> = emptyList(),
    val currentIndex: Int = 0
) {
    val currentFeature: ComposeFeature?
        get() = features.getOrNull(currentIndex)

    val canShowPrevious: Boolean
        get() = currentIndex > 0

    val canShowNext: Boolean
        get() = currentIndex < features.lastIndex
}

class ComposeFeatureViewModel(
    private val repository: ComposeFeatureRepository = ComposeFeatureRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ComposeFeatureUiState(features = repository.loadFeatures(), currentIndex = 0)
    )
    val uiState: StateFlow<ComposeFeatureUiState> = _uiState

    fun selectFeature(featureId: String) {
        val index = _uiState.value.features.indexOfFirst { it.id == featureId }
        if (index >= 0) {
            _uiState.update { state -> state.copy(currentIndex = index) }
        }
    }

    fun showNext() {
        _uiState.update { state ->
            if (state.canShowNext) state.copy(currentIndex = state.currentIndex + 1) else state
        }
    }

    fun showPrevious() {
        _uiState.update { state ->
            if (state.canShowPrevious) state.copy(currentIndex = state.currentIndex - 1) else state
        }
    }
}
