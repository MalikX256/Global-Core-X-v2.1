package com.example.data.auth

import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

interface AuthenticationService {
    suspend fun register(request: RegisterRequest): AuthResult<AuthResponse>
    suspend fun login(request: LoginRequest): AuthResult<AuthResponse>
    suspend fun logout(): AuthResult<Unit>
    suspend fun sendPasswordResetCode(email: String): AuthResult<String>
    suspend fun resetPassword(request: ResetPasswordRequest): AuthResult<Unit>
    suspend fun verifyEmail(userId: String, code: String): AuthResult<UserEntity>
    suspend fun resendEmailVerification(userId: String): AuthResult<String>
    fun getCurrentUser(): Flow<UserEntity?>
    suspend fun updateProfile(userId: String, update: ProfileUpdateRequest): AuthResult<UserEntity>
    suspend fun changePassword(userId: String, request: ChangePasswordRequest): AuthResult<Unit>
    suspend fun deleteAccount(userId: String): AuthResult<Unit>
}
