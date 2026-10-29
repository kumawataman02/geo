package com.geo.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geo.auth.User
import com.geo.commontes.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
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

    private val _selectedFriends = MutableStateFlow<List<User>>(emptyList())

    val selectedFriends = _selectedFriends.asStateFlow()

    private val _inviteAction = MutableStateFlow<UiState<Unit>>(UiState.Idle)

    val inviteAction = _inviteAction.asStateFlow()

    val myUid = mainRepository.myUid

    val invites = mainRepository.observeMyInvites()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val groups = mainRepository.observeMyGroups()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun groupMake(groupName: String) {
        viewModelScope.launch {
            _makeGroup.value = UiState.Loading

            val result = mainRepository.makeGroup(groupName, _selectedFriends.value)

            _makeGroup.value = result.fold(
                onSuccess = { groupId ->
                    _selectedFriends.value = emptyList()
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

    fun addFriend(user: User) {
        if (_selectedFriends.value.none { it.userId == user.userId }) {
            _selectedFriends.value += user
        }
        resetSearchState()
    }

    fun removeFriend(user: User) {
        _selectedFriends.value -= user
    }

    fun acceptInvite(invite: Invite) = inviteAction { mainRepository.acceptInvite(invite) }

    fun rejectInvite(invite: Invite) = inviteAction { mainRepository.rejectInvite(invite) }

    private fun inviteAction(action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _inviteAction.value = UiState.Loading
            _inviteAction.value = action().fold(
                onSuccess = { UiState.Idle },
                onFailure = { UiState.Error(it.message ?: "Something went wrong") }
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
