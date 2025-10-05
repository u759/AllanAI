package com.example.myapplication.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Simple local authentication manager using SharedPreferences
 * 
 * WARNING: This is NOT secure - passwords are stored in plain text.
 * For production, use proper encryption and backend authentication.
 */
class AuthManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    /**
     * Check if a user is currently logged in
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    /**
     * Get the currently logged in username
     */
    fun getCurrentUsername(): String? {
        return prefs.getString(KEY_CURRENT_USERNAME, null)
    }
    
    /**
     * Sign up a new user
     * Returns true if successful, false if username already exists
     */
    fun signUp(username: String, email: String, password: String): SignUpResult {
        // Check if username already exists
        if (prefs.contains(getUserKey(username))) {
            return SignUpResult.UsernameTaken
        }
        
        // Check if email already exists
        val existingUsername = findUsernameByEmail(email)
        if (existingUsername != null) {
            return SignUpResult.EmailTaken
        }
        
        // Save user data
        prefs.edit().apply {
            putString(getUserKey(username), email)
            putString(getPasswordKey(username), password)
            putString(getEmailToUsernameKey(email), username)
            apply()
        }
        
        return SignUpResult.Success
    }
    
    /**
     * Sign in a user
     * Returns true if credentials are valid
     */
    fun signIn(username: String, password: String): Boolean {
        val storedPassword = prefs.getString(getPasswordKey(username), null)
        
        if (storedPassword == null || storedPassword != password) {
            return false
        }
        
        // Mark user as logged in
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_CURRENT_USERNAME, username)
            apply()
        }
        
        return true
    }
    
    /**
     * Get user email by username
     */
    fun getUserEmail(username: String): String? {
        return prefs.getString(getUserKey(username), null)
    }
    
    /**
     * Get current logged in user's email
     */
    fun getCurrentUserEmail(): String? {
        val username = getCurrentUsername() ?: return null
        return getUserEmail(username)
    }
    
    /**
     * Change password for the currently logged in user
     */
    fun changePassword(currentPassword: String, newPassword: String): PasswordChangeResult {
        val username = getCurrentUsername() ?: return PasswordChangeResult.NotLoggedIn
        val storedPassword = prefs.getString(getPasswordKey(username), null)
        
        if (storedPassword != currentPassword) {
            return PasswordChangeResult.WrongCurrentPassword
        }
        
        // Update password
        prefs.edit().apply {
            putString(getPasswordKey(username), newPassword)
            apply()
        }
        
        return PasswordChangeResult.Success
    }
    
    /**
     * Update profile information for the currently logged in user
     */
    fun updateProfile(newUsername: String, newEmail: String): ProfileUpdateResult {
        val currentUsername = getCurrentUsername() ?: return ProfileUpdateResult.NotLoggedIn
        val currentEmail = getUserEmail(currentUsername)
        
        // If username changed, check if new username is available
        if (newUsername != currentUsername) {
            if (prefs.contains(getUserKey(newUsername))) {
                return ProfileUpdateResult.UsernameTaken
            }
        }
        
        // If email changed, check if new email is available
        if (newEmail != currentEmail) {
            val existingUsername = findUsernameByEmail(newEmail)
            if (existingUsername != null && existingUsername != currentUsername) {
                return ProfileUpdateResult.EmailTaken
            }
        }
        
        // Get current password
        val password = prefs.getString(getPasswordKey(currentUsername), null)
            ?: return ProfileUpdateResult.NotLoggedIn
        
        prefs.edit().apply {
            // Remove old entries if username changed
            if (newUsername != currentUsername) {
                remove(getUserKey(currentUsername))
                remove(getPasswordKey(currentUsername))
            }
            
            // Remove old email mapping if email changed
            if (newEmail != currentEmail && currentEmail != null) {
                remove(getEmailToUsernameKey(currentEmail))
            }
            
            // Add new entries
            putString(getUserKey(newUsername), newEmail)
            putString(getPasswordKey(newUsername), password)
            putString(getEmailToUsernameKey(newEmail), newUsername)
            putString(KEY_CURRENT_USERNAME, newUsername)
            
            apply()
        }
        
        return ProfileUpdateResult.Success
    }
    
    /**
     * Log out the current user
     */
    fun logout() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_CURRENT_USERNAME)
            apply()
        }
    }
    
    /**
     * Clear all stored data (for testing/debugging)
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
    
    // Helper methods
    
    private fun getUserKey(username: String) = "user_$username"
    private fun getPasswordKey(username: String) = "password_$username"
    private fun getEmailToUsernameKey(email: String) = "email_$email"
    
    private fun findUsernameByEmail(email: String): String? {
        return prefs.getString(getEmailToUsernameKey(email), null)
    }
    
    companion object {
        private const val PREFS_NAME = "allan_ai_auth"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_CURRENT_USERNAME = "current_username"
    }
}

sealed class SignUpResult {
    object Success : SignUpResult()
    object UsernameTaken : SignUpResult()
    object EmailTaken : SignUpResult()
}

sealed class PasswordChangeResult {
    object Success : PasswordChangeResult()
    object NotLoggedIn : PasswordChangeResult()
    object WrongCurrentPassword : PasswordChangeResult()
}

sealed class ProfileUpdateResult {
    object Success : ProfileUpdateResult()
    object NotLoggedIn : ProfileUpdateResult()
    object UsernameTaken : ProfileUpdateResult()
    object EmailTaken : ProfileUpdateResult()
}
