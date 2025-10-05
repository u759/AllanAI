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
 * ViewModel for authentication screens (SignIn, SignUp)
 * 
 * Manages authentication state and coordinates with AuthRepository
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    val authRepository: AuthRepository
) : ViewModel() {
    
    private val _signInState = MutableStateFlow(SignInState())
    val signInState: StateFlow<SignInState> = _signInState.asStateFlow()
    
    private val _signUpState = MutableStateFlow(SignUpState())
    val signUpState: StateFlow<SignUpState> = _signUpState.asStateFlow()
    
    /**
     * Sign in user
     */
    fun signIn(username: String, password: String) {
        viewModelScope.launch {
            _signInState.value = _signInState.value.copy(
                isLoading = true,
                error = null
            )
            
            authRepository.signIn(username, password)
                .onSuccess {
                    _signInState.value = _signInState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                }
                .onFailure { error ->
                    _signInState.value = _signInState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }
    
    /**
     * Sign up new user
     */
    fun signUp(username: String, email: String, password: String, confirmPassword: String) {
        // Validate inputs
        when {
            username.isBlank() -> {
                _signUpState.value = _signUpState.value.copy(
                    error = "Username cannot be empty"
                )
                return
            }
            email.isBlank() -> {
                _signUpState.value = _signUpState.value.copy(
                    error = "Email cannot be empty"
                )
                return
            }
            password.isBlank() -> {
                _signUpState.value = _signUpState.value.copy(
                    error = "Password cannot be empty"
                )
                return
            }
            password != confirmPassword -> {
                _signUpState.value = _signUpState.value.copy(
                    error = "Passwords do not match"
                )
                return
            }
        }
        
        viewModelScope.launch {
            _signUpState.value = _signUpState.value.copy(
                isLoading = true,
                error = null
            )
            
            authRepository.signUp(username, email, password)
                .onSuccess {
                    // Auto-login after successful signup
                    authRepository.signIn(username, password)
                        .onSuccess {
                            _signUpState.value = _signUpState.value.copy(
                                isLoading = false,
                                isSuccess = true
                            )
                        }
                        .onFailure { error ->
                            _signUpState.value = _signUpState.value.copy(
                                isLoading = false,
                                error = error.message
                            )
                        }
                }
                .onFailure { error ->
                    _signUpState.value = _signUpState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }
    
    /**
     * Clear sign in error
     */
    fun clearSignInError() {
        _signInState.value = _signInState.value.copy(error = null)
    }
    
    /**
     * Clear sign up error
     */
    fun clearSignUpError() {
        _signUpState.value = _signUpState.value.copy(error = null)
    }
    
    /**
     * Reset sign in state (for navigation)
     */
    fun resetSignInState() {
        _signInState.value = SignInState()
    }
    
    /**
     * Reset sign up state (for navigation)
     */
    fun resetSignUpState() {
        _signUpState.value = SignUpState()
    }
}

/**
 * UI State for Sign In screen
 */
data class SignInState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

/**
 * UI State for Sign Up screen
 */
data class SignUpState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
