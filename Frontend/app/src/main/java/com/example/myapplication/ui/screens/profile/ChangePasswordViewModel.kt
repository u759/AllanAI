package com.example.myapplication.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Change Password screen
 * 
 * Manages password change logic and validation
 */
@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(ChangePasswordState())
    val state: StateFlow<ChangePasswordState> = _state.asStateFlow()
    
    /**
     * Update current password field
     */
    fun updateCurrentPassword(password: String) {
        _state.value = _state.value.copy(currentPassword = password)
    }
    
    /**
     * Update new password field
     */
    fun updateNewPassword(password: String) {
        _state.value = _state.value.copy(newPassword = password)
    }
    
    /**
     * Update confirm password field
     */
    fun updateConfirmPassword(password: String) {
        _state.value = _state.value.copy(confirmPassword = password)
    }
    
    /**
     * Change password with validation
     */
    fun changePassword() {
        val state = _state.value
        
        // Validate inputs
        when {
            state.currentPassword.isBlank() -> {
                _state.value = state.copy(error = "Current password cannot be empty")
                return
            }
            state.newPassword.isBlank() -> {
                _state.value = state.copy(error = "New password cannot be empty")
                return
            }
            state.newPassword != state.confirmPassword -> {
                _state.value = state.copy(error = "Passwords do not match")
                return
            }
        }
        
        viewModelScope.launch {
            _state.value = state.copy(
                isLoading = true,
                error = null
            )
            
            authRepository.changePassword(state.currentPassword, state.newPassword)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

/**
 * UI State for Change Password screen
 */
data class ChangePasswordState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
