package com.geo.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geo.commontes.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FenceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mainRepository: MainRepository
) : ViewModel() {


    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    val myUid = mainRepository.myUid

    private val _group = MutableStateFlow<UiState<Group>>(UiState.Loading)

    val group = _group.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<Unit>>(UiState.Idle)

    val saveState = _saveState.asStateFlow()

    init {
        viewModelScope.launch {
            _group.value = mainRepository.getGroup(groupId).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Failed to load group") }
            )
        }
    }

    fun saveFence(fence: Fence) {
        viewModelScope.launch {
            _saveState.value = UiState.Loading
            _saveState.value = mainRepository.setFence(groupId, fence).fold(
                onSuccess = { UiState.Success(Unit) },
                onFailure = { UiState.Error(it.message ?: "Failed to save fence") }
            )
        }
    }
}
