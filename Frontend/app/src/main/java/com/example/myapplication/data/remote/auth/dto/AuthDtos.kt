package com.example.myapplication.data.remote.auth.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    @Json(name = "token") val token: String,
    @Json(name = "tokenType") val tokenType: String,
    @Json(name = "expiresAt") val expiresAt: String?,
    @Json(name = "user") val user: UserProfileDto
)

@JsonClass(generateAdapter = true)
data class UserProfileDto(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class SignUpRequestDto(
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class SignInRequestDto(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequestDto(
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class ChangePasswordRequestDto(
    @Json(name = "currentPassword") val currentPassword: String,
    @Json(name = "newPassword") val newPassword: String
)
