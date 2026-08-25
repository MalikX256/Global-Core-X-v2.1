package com.example.data.auth

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.UserEntity
import com.example.security.SecureTokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class AuthRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val userDao = db.userDao()
    private val tripDao = db.tripDao()
    private val locationDao = db.locationDao()
    private val recordedRouteDao = db.recordedRouteDao()
    private val routeWaypointDao = db.routeWaypointDao()
    private val savedRouteDao = db.savedRouteDao()
    private val emergencyContactDao = db.emergencyContactDao()
    private val sharedSessionDao = db.sharedSessionDao()
    private val userPreferencesDao = db.userPreferencesDao()

    val tokenManager = SecureTokenManager(context)
    val authService: AuthenticationService = LocalAuthenticationService(userDao)

    val currentUser: Flow<UserEntity?> = authService.getCurrentUser()

    suspend fun autoLoginCheck(): UserEntity? = withContext(Dispatchers.IO) {
        val current = userDao.getCurrentUserDirect()
        if (current != null && current.isCurrentLoggedIn && current.rememberMe) {
            return@withContext current
        }

        val savedUserId = tokenManager.getAuthenticatedUserId()
        val token = tokenManager.getAuthToken()
        if (savedUserId != null && token != null && tokenManager.isRememberMe()) {
            val user = userDao.getUserById(savedUserId)
            if (user != null) {
                userDao.setLoggedInUser(user.id, token)
                return@withContext user.copy(isCurrentLoggedIn = true)
            }
        }
        null
    }

    suspend fun register(request: RegisterRequest): AuthResult<AuthResponse> {
        val result = authService.register(request)
        if (result is AuthResult.Success) {
            tokenManager.saveSession(
                userId = result.data.user.id,
                token = result.data.token,
                rememberMe = true
            )
            tokenManager.saveIdentifier(result.data.user.email)
        }
        return result
    }

    suspend fun login(request: LoginRequest): AuthResult<AuthResponse> {
        val result = authService.login(request)
        if (result is AuthResult.Success) {
            tokenManager.saveSession(
                userId = result.data.user.id,
                token = result.data.token,
                rememberMe = request.rememberMe
            )
            tokenManager.saveIdentifier(request.identifier)
        }
        return result
    }

    suspend fun logout(): AuthResult<Unit> = withContext(Dispatchers.IO) {
        tokenManager.clearSession()
        authService.logout()
    }

    suspend fun verifyEmail(userId: String, code: String): AuthResult<UserEntity> {
        return authService.verifyEmail(userId, code)
    }

    suspend fun resendEmailVerification(userId: String): AuthResult<String> {
        return authService.resendEmailVerification(userId)
    }

    suspend fun sendPasswordResetCode(email: String): AuthResult<String> {
        return authService.sendPasswordResetCode(email)
    }

    suspend fun resetPassword(request: ResetPasswordRequest): AuthResult<Unit> {
        return authService.resetPassword(request)
    }

    suspend fun updateProfile(userId: String, update: ProfileUpdateRequest): AuthResult<UserEntity> {
        return authService.updateProfile(userId, update)
    }

    suspend fun changePassword(userId: String, request: ChangePasswordRequest): AuthResult<Unit> {
        return authService.changePassword(userId, request)
    }

    suspend fun deleteAccount(userId: String): AuthResult<Unit> = withContext(Dispatchers.IO) {
        tokenManager.clearSession()
        // Clean all user relational telemetry, routes, trips, contacts, preferences securely
        tripDao.deleteAllTrips()
        locationDao.deleteAllLocations()
        recordedRouteDao.deleteAllRecordedRoutes()
        routeWaypointDao.deleteAllWaypoints()
        userDao.deleteUserById(userId)
        authService.logout()
    }
}
