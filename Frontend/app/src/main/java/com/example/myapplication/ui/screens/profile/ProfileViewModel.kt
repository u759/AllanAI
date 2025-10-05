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
 * ViewModel for Profile screen
 * 
 * Manages user profile display and logout
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()
    
    init {
        loadProfile()
    }
    
    /**
     * Load current user profile
     */
    private fun loadProfile() {
        viewModelScope.launch {
            val username = authRepository.getCurrentUsername() ?: "User"
            val email = authRepository.getCurrentUserEmail() ?: ""
            
            _state.value = _state.value.copy(
                username = username,
                email = email
            )
        }
    }
    
    /**
     * Log out current user
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _state.value = _state.value.copy(isLoggedOut = true)
        }
    }
}

/**
 * UI State for Profile screen
 */
data class ProfileState(
    val username: String = "",
    val email: String = "",
    val isLoggedOut: Boolean = false
)
