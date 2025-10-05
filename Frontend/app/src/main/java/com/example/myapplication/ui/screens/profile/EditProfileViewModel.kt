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
 * ViewModel for Edit Profile screen
 * 
 * Manages profile editing and updates
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()
    
    init {
        loadCurrentProfile()
    }
    
    /**
     * Load current user profile data
     */
    private fun loadCurrentProfile() {
        viewModelScope.launch {
            val username = authRepository.getCurrentUsername() ?: ""
            val email = authRepository.getCurrentUserEmail() ?: ""
            
            _state.value = _state.value.copy(
                username = username,
                email = email
            )
        }
    }
    
    /**
     * Update username field
     */
    fun updateUsername(username: String) {
        _state.value = _state.value.copy(username = username)
    }
    
    /**
     * Update email field
     */
    fun updateEmail(email: String) {
        _state.value = _state.value.copy(email = email)
    }
    
    /**
     * Save profile changes
     */
    fun saveChanges() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null
            )
            
            authRepository.updateProfile(_state.value.username, _state.value.email)
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
 * UI State for Edit Profile screen
 */
data class EditProfileState(
    val username: String = "",
    val email: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
