package com.example.myapplication.data.remote.auth

import com.example.myapplication.data.remote.auth.dto.AuthResponseDto
import com.example.myapplication.data.remote.auth.dto.ChangePasswordRequestDto
import com.example.myapplication.data.remote.auth.dto.SignInRequestDto
import com.example.myapplication.data.remote.auth.dto.SignUpRequestDto
import com.example.myapplication.data.remote.auth.dto.UpdateProfileRequestDto
import com.example.myapplication.data.remote.auth.dto.UserProfileDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AuthApi {

    @POST("api/users")
    suspend fun signUp(
        @Body request: SignUpRequestDto
    ): AuthResponseDto

    @POST("api/users/signin")
    suspend fun signIn(
        @Body request: SignInRequestDto
    ): AuthResponseDto

    @GET("api/users/{userId}")
    suspend fun getProfile(
        @Path("userId") userId: String
    ): UserProfileDto

    @PUT("api/users/{userId}")
    suspend fun updateProfile(
        @Path("userId") userId: String,
        @Body request: UpdateProfileRequestDto
    ): UserProfileDto

    @PUT("api/users/{userId}/password")
    suspend fun changePassword(
        @Path("userId") userId: String,
        @Body request: ChangePasswordRequestDto
    ): Response<Unit>
}
