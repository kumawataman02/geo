
package com.geo.auth

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geo.commontes.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: LoginRepository
) : ViewModel() {

    private val _state = MutableStateFlow(repository.getCurrentUser()?.let { UiState.Success(it) } ?: UiState.Idle)

    val state: StateFlow<UiState<User>> = _state.asStateFlow()

    fun signInWithGoogle(context: Context) {
        if (_state.value is UiState.Loading) return

        viewModelScope.launch {
            _state.value = UiState.Loading

            try {
                val user = repository.signInWithGoogle(context)

                repository.saveUser(user)

                _state.value = UiState.Success(user)

            } catch (e: GetCredentialCancellationException) {
                _state.value = UiState.Idle

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                _state.value = UiState.Error(
                    e.message ?: "Login or profile save failed"
                )
            }
        }
    }
}

