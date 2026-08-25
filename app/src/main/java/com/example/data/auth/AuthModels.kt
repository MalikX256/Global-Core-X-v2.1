package com.example.data.auth

import com.example.data.local.entity.UserEntity

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : AuthResult<Nothing>()
    object Loading : AuthResult<Nothing>()
}

data class RegisterRequest(
    val fullName: String,
    val username: String,
    val email: String,
    val phone: String = "",
    val country: String = "",
    val avatarUrl: String = "",
    val password: String,
    val agreedToTerms: Boolean,
    val agreedToPrivacy: Boolean
)

data class LoginRequest(
    val identifier: String, // email or username
    val password: String,
    val rememberMe: Boolean = true
)

data class ProfileUpdateRequest(
    val fullName: String,
    val username: String,
    val phone: String,
    val country: String,
    val avatarUrl: String
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class ResetPasswordRequest(
    val email: String,
    val verificationCode: String,
    val newPassword: String
)

data class AuthResponse(
    val user: UserEntity,
    val token: String,
    val isEmailVerified: Boolean,
    val message: String = ""
)

sealed class AuthScreenState {
    object Welcome : AuthScreenState()
    object SignIn : AuthScreenState()
    object SignUp : AuthScreenState()
    data class EmailVerification(val userId: String, val email: String, val generatedCode: String = "") : AuthScreenState()
    object ForgotPassword : AuthScreenState()
    object Authenticated : AuthScreenState()
}
