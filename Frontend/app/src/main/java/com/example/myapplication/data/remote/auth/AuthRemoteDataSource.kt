package com.example.myapplication.data.remote.auth

import com.example.myapplication.data.remote.auth.dto.AuthResponseDto
import com.example.myapplication.data.remote.auth.dto.ChangePasswordRequestDto
import com.example.myapplication.data.remote.auth.dto.SignInRequestDto
import com.example.myapplication.data.remote.auth.dto.SignUpRequestDto
import com.example.myapplication.data.remote.auth.dto.UpdateProfileRequestDto
import com.example.myapplication.data.remote.auth.dto.UserProfileDto
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source for authentication API calls.
 *
 * Wraps AuthApi and provides error handling for network operations.
 */
@Singleton
class AuthRemoteDataSource @Inject constructor(
    private val authApi: AuthApi
) {

    /**
     * Sign up a new user
     *
     * @throws HttpException if the server returns an error (e.g., 409 for conflicts)
     * @throws IOException if network fails
     */
    suspend fun signUp(
        username: String,
        email: String,
        password: String
    ): AuthResponseDto {
        return authApi.signUp(
            SignUpRequestDto(
                username = username,
                email = email,
                password = password
            )
        )
    }

    /**
     * Sign in existing user
     *
     * @throws HttpException if the server returns an error (e.g., 401 for invalid credentials)
     * @throws IOException if network fails
     */
    suspend fun signIn(
        username: String,
        password: String
    ): AuthResponseDto {
        return authApi.signIn(
            SignInRequestDto(
                username = username,
                password = password
            )
        )
    }

    /**
     * Get user profile
     *
     * @throws HttpException if the server returns an error
     * @throws IOException if network fails
     */
    suspend fun getProfile(userId: String): UserProfileDto {
        return authApi.getProfile(userId)
    }

    /**
     * Update user profile
     *
     * @throws HttpException if the server returns an error (e.g., 409 for conflicts)
     * @throws IOException if network fails
     */
    suspend fun updateProfile(
        userId: String,
        username: String,
        email: String
    ): UserProfileDto {
        return authApi.updateProfile(
            userId = userId,
            request = UpdateProfileRequestDto(
                username = username,
                email = email
            )
        )
    }

    /**
     * Change user password
     *
     * @throws HttpException if the server returns an error (e.g., 401 for wrong password)
     * @throws IOException if network fails
     */
    suspend fun changePassword(
        userId: String,
        currentPassword: String,
        newPassword: String
    ): Boolean {
        val response = authApi.changePassword(
            userId = userId,
            request = ChangePasswordRequestDto(
                currentPassword = currentPassword,
                newPassword = newPassword
            )
        )
        return response.isSuccessful
    }
}
