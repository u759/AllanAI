package com.example.myapplication.data.repository

import com.example.myapplication.data.local.AuthManager
import com.example.myapplication.data.local.PasswordChangeResult
import com.example.myapplication.data.local.ProfileUpdateResult
import com.example.myapplication.data.local.SignUpResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository layer for authentication operations
 * 
 * Abstracts AuthManager from ViewModels following MVVM pattern
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authManager: AuthManager
) {
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean = authManager.isLoggedIn()
    
    /**
     * Get current username
     */
    fun getCurrentUsername(): String? = authManager.getCurrentUsername()
    
    /**
     * Get current user email
     */
    fun getCurrentUserEmail(): String? = authManager.getCurrentUserEmail()
    
    /**
     * Sign in user with username and password
     */
    suspend fun signIn(username: String, password: String): Result<Unit> {
        return try {
            val success = authManager.signIn(username, password)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid username or password"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign up new user
     */
    suspend fun signUp(
        username: String,
        email: String,
        password: String
    ): Result<Unit> {
        return try {
            when (val result = authManager.signUp(username, email, password)) {
                is SignUpResult.Success -> Result.success(Unit)
                is SignUpResult.UsernameTaken -> Result.failure(Exception("Username is already taken"))
                is SignUpResult.EmailTaken -> Result.failure(Exception("Email is already registered"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateProfile(username: String, email: String): Result<Unit> {
        return try {
            when (val result = authManager.updateProfile(username, email)) {
                is ProfileUpdateResult.Success -> Result.success(Unit)
                is ProfileUpdateResult.UsernameTaken -> Result.failure(Exception("Username is already taken"))
                is ProfileUpdateResult.EmailTaken -> Result.failure(Exception("Email is already registered"))
                is ProfileUpdateResult.NotLoggedIn -> Result.failure(Exception("You must be logged in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Change user password
     */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            when (val result = authManager.changePassword(currentPassword, newPassword)) {
                is PasswordChangeResult.Success -> Result.success(Unit)
                is PasswordChangeResult.WrongCurrentPassword -> Result.failure(Exception("Current password is incorrect"))
                is PasswordChangeResult.NotLoggedIn -> Result.failure(Exception("You must be logged in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Log out current user
     */
    fun logout() {
        authManager.logout()
    }
}
