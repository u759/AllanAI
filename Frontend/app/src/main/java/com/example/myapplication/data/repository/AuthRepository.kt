package com.example.myapplication.data.repository

import com.example.myapplication.data.local.AuthLocalDataSource
import com.example.myapplication.data.local.AuthSession
import com.example.myapplication.data.local.StoredUser
import com.example.myapplication.data.remote.auth.AuthRemoteDataSource
import com.example.myapplication.data.remote.auth.dto.AuthResponseDto
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository layer for authentication operations
 *
 * Coordinates between remote API and local storage following MVVM pattern
 */
@Singleton
class AuthRepository @Inject constructor(
    private val localDataSource: AuthLocalDataSource,
    private val remoteDataSource: AuthRemoteDataSource
) {

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean = localDataSource.isLoggedIn()

    /**
     * Get current username
     */
    fun getCurrentUsername(): String? = localDataSource.getCurrentUsername()

    /**
     * Get current user email
     */
    fun getCurrentUserEmail(): String? = localDataSource.getCurrentUserEmail()

    /**
     * Get authentication token
     */
    fun getToken(): String? = localDataSource.getToken()

    /**
     * Sign in user with username and password
     */
    suspend fun signIn(username: String, password: String): Result<Unit> {
        return try {
            val response = remoteDataSource.signIn(username, password)
            saveAuthSession(response)
            Result.success(Unit)
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> Result.failure(Exception("Invalid username or password"))
                else -> Result.failure(Exception("Authentication failed: ${e.message()}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection."))
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
            val response = remoteDataSource.signUp(username, email, password)
            saveAuthSession(response)
            Result.success(Unit)
        } catch (e: HttpException) {
            when (e.code()) {
                409 -> {
                    // Parse error message to determine if username or email conflict
                    val errorBody = e.response()?.errorBody()?.string() ?: ""
                    when {
                        errorBody.contains("username", ignoreCase = true) ->
                            Result.failure(Exception("Username is already taken"))
                        errorBody.contains("email", ignoreCase = true) ->
                            Result.failure(Exception("Email is already registered"))
                        else ->
                            Result.failure(Exception("Username or email already exists"))
                    }
                }
                400 -> Result.failure(Exception("Invalid input. Please check your information."))
                else -> Result.failure(Exception("Sign up failed: ${e.message()}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(username: String, email: String): Result<Unit> {
        return try {
            val userId = localDataSource.getUserId()
                ?: return Result.failure(Exception("You must be logged in"))

            val updatedProfile = remoteDataSource.updateProfile(userId, username, email)

            // Update local storage with new user info
            localDataSource.updateUser(
                StoredUser(
                    id = updatedProfile.id,
                    username = updatedProfile.username,
                    email = updatedProfile.email
                )
            )

            Result.success(Unit)
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> Result.failure(Exception("You must be logged in"))
                409 -> {
                    val errorBody = e.response()?.errorBody()?.string() ?: ""
                    when {
                        errorBody.contains("username", ignoreCase = true) ->
                            Result.failure(Exception("Username is already taken"))
                        errorBody.contains("email", ignoreCase = true) ->
                            Result.failure(Exception("Email is already registered"))
                        else ->
                            Result.failure(Exception("Username or email already exists"))
                    }
                }
                else -> Result.failure(Exception("Profile update failed: ${e.message()}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection."))
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
            val userId = localDataSource.getUserId()
                ?: return Result.failure(Exception("You must be logged in"))

            val success = remoteDataSource.changePassword(userId, currentPassword, newPassword)

            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Password change failed"))
            }
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> Result.failure(Exception("Current password is incorrect"))
                403 -> Result.failure(Exception("You must be logged in"))
                else -> Result.failure(Exception("Password change failed: ${e.message()}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Log out current user
     */
    fun logout() {
        localDataSource.clear()
    }

    /**
     * Helper function to save authentication session from API response
     */
    private fun saveAuthSession(response: AuthResponseDto) {
        localDataSource.saveSession(
            AuthSession(
                token = response.token,
                tokenType = response.tokenType,
                expiresAtIso = response.expiresAt,
                user = StoredUser(
                    id = response.user.id,
                    username = response.user.username,
                    email = response.user.email
                )
            )
        )
    }
}
