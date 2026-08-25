package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.*
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo = AuthRepository(application.applicationContext)

    val currentUser: StateFlow<UserEntity?> = authRepo.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _screenState = MutableStateFlow<AuthScreenState>(AuthScreenState.Welcome)
    val screenState: StateFlow<AuthScreenState> = _screenState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _savedIdentifier = MutableStateFlow<String?>(authRepo.tokenManager.getSavedIdentifier())
    val savedIdentifier: StateFlow<String?> = _savedIdentifier.asStateFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            _isLoading.value = true
            val existingUser = authRepo.autoLoginCheck()
            if (existingUser != null) {
                _screenState.value = AuthScreenState.Authenticated
            } else {
                _screenState.value = AuthScreenState.Welcome
            }
            _isLoading.value = false
        }
    }

    fun navigateTo(state: AuthScreenState) {
        _errorMessage.value = null
        _successMessage.value = null
        _screenState.value = state
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun signIn(identifier: String, password: String, rememberMe: Boolean, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            val req = LoginRequest(identifier = identifier, password = password, rememberMe = rememberMe)
            when (val res = authRepo.login(req)) {
                is AuthResult.Success -> {
                    _savedIdentifier.value = identifier
                    _screenState.value = AuthScreenState.Authenticated
                    _successMessage.value = res.data.message
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _errorMessage.value = res.message
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun signUp(
        fullName: String,
        username: String,
        email: String,
        phone: String,
        country: String,
        avatarUrl: String,
        password: String,
        agreedToTerms: Boolean,
        agreedToPrivacy: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            val req = RegisterRequest(
                fullName = fullName,
                username = username,
                email = email,
                phone = phone,
                country = country,
                avatarUrl = avatarUrl,
                password = password,
                agreedToTerms = agreedToTerms,
                agreedToPrivacy = agreedToPrivacy
            )

            when (val res = authRepo.register(req)) {
                is AuthResult.Success -> {
                    val user = res.data.user
                    _savedIdentifier.value = user.email
                    _screenState.value = AuthScreenState.EmailVerification(
                        userId = user.id,
                        email = user.email,
                        generatedCode = user.verificationCode
                    )
                    _successMessage.value = res.data.message
                }
                is AuthResult.Error -> {
                    _errorMessage.value = res.message
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun verifyEmail(userId: String, code: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            when (val res = authRepo.verifyEmail(userId, code)) {
                is AuthResult.Success -> {
                    _screenState.value = AuthScreenState.Authenticated
                    _successMessage.value = "Email verified successfully. Welcome to GlobalCore-X!"
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _errorMessage.value = res.message
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun resendVerificationCode(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            when (val res = authRepo.resendEmailVerification(userId)) {
                is AuthResult.Success -> {
                    val current = _screenState.value
                    if (current is AuthScreenState.EmailVerification) {
                        _screenState.value = current.copy(generatedCode = res.data)
                    }
                    _successMessage.value = "New verification code generated and sent to your email."
                }
                is AuthResult.Error -> {
                    _errorMessage.value = res.message
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun requestPasswordResetCode(email: String, onCodeSent: (code: String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            when (val res = authRepo.sendPasswordResetCode(email)) {
                is AuthResult.Success -> {
                    _successMessage.value = "Reset instructions sent to $email."
                    onCodeSent(res.data)
                }
                is AuthResult.Error -> {
                    _errorMessage.value = res.message
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun resetPassword(email: String, code: String, newPass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            val req = ResetPasswordRequest(email = email, verificationCode = code, newPassword = newPass)
            when (val res = authRepo.resetPassword(req)) {
                is AuthResult.Success -> {
                    _successMessage.value = "Password successfully reset."
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _errorMessage.value = res.message
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun updateProfile(userId: String, fullName: String, username: String, phone: String, country: String, avatarUrl: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val req = ProfileUpdateRequest(fullName, username, phone, country, avatarUrl)
            when (val res = authRepo.updateProfile(userId, req)) {
                is AuthResult.Success -> {
                    _successMessage.value = "Profile updated successfully."
                    onResult(true, null)
                }
                is AuthResult.Error -> {
                    _errorMessage.value = res.message
                    onResult(false, res.message)
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun changePassword(userId: String, currentPass: String, newPass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val req = ChangePasswordRequest(currentPass, newPass)
            when (val res = authRepo.changePassword(userId, req)) {
                is AuthResult.Success -> {
                    _successMessage.value = "Password changed successfully."
                    onResult(true, null)
                }
                is AuthResult.Error -> {
                    _errorMessage.value = res.message
                    onResult(false, res.message)
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            authRepo.logout()
            _screenState.value = AuthScreenState.Welcome
            _isLoading.value = false
        }
    }

    fun deleteAccount(userId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            authRepo.deleteAccount(userId)
            _screenState.value = AuthScreenState.Welcome
            _isLoading.value = false
            onComplete()
        }
    }
}
