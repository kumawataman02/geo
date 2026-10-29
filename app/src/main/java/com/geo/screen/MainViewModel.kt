
package com.geo.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geo.auth.User
import com.geo.commontes.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainRepository: MainRepository
) : ViewModel() {

    private val _makeGroup = MutableStateFlow<UiState<String>>(UiState.Idle)

    val makeGroup = _makeGroup.asStateFlow()

    private val _searchState = MutableStateFlow<UiState<User>>(UiState.Idle)

    val searchState = _searchState.asStateFlow()

    fun groupMake(group: Group) {
        viewModelScope.launch {
            _makeGroup.value = UiState.Loading

            val result = mainRepository.makeGroup(group)

            _makeGroup.value = result.fold(
                onSuccess = { groupId ->
                    UiState.Success(groupId)
                },
                onFailure = { exception ->
                    UiState.Error(
                        exception.message ?: "Failed to create group"
                    )
                }
            )
        }
    }

    fun searchFriend(email: String) {
        val normalizedEmail = email.trim().lowercase()

        if (normalizedEmail.isBlank()) return

        viewModelScope.launch {
            _searchState.value = UiState.Loading

            val result = mainRepository.searchUserByEmail(
                normalizedEmail
            )

            _searchState.value = result.fold(
                onSuccess = { user ->
                    if (user != null) {
                        UiState.Success(user)
                    } else {
                        UiState.Error("No user found with this email")
                    }
                },
                onFailure = { exception ->
                    UiState.Error(
                        exception.message ?: "Failed to search user"
                    )
                }
            )
        }
    }

    // Reset States
    fun resetGroupState() {
        _makeGroup.value = UiState.Idle
    }

    fun resetSearchState() {
        _searchState.value = UiState.Idle
    }
}
