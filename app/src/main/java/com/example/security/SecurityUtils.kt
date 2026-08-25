package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.regex.Pattern

object SecurityUtils {

    private val secureRandom = SecureRandom()

    private val EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$"
    )

    private val USERNAME_PATTERN = Pattern.compile(
        "^[A-Za-z0-9_]{3,30}$"
    )

    // International phone number validator: accepts optional '+', 7 to 15 digits, spaces/hyphens allowed
    private val PHONE_PATTERN = Pattern.compile(
        "^\\+?[0-9\\s\\-()]{7,20}$"
    )

    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = (salt + password).toByteArray(Charsets.UTF_8)
        val hashBytes = digest.digest(input)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun generateAuthToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return "gcx_tok_" + bytes.joinToString("") { "%02x".format(it) }
    }

    fun generateUserId(): String {
        val uuidShort = UUID.randomUUID().toString().replace("-", "").take(10)
        return "user_gcx_$uuidShort"
    }

    fun generateVerificationCode(): String {
        val code = 100000 + secureRandom.nextInt(900000)
        return code.toString()
    }

    // --- Validation Rules ---

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val errorMessage: String) : ValidationResult()

        val isValid: Boolean get() = this is Valid
    }

    fun validateFullName(fullName: String): ValidationResult {
        val trimmed = fullName.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid("Full name is required.")
            trimmed.length < 2 -> ValidationResult.Invalid("Full name must be at least 2 characters.")
            trimmed.length > 60 -> ValidationResult.Invalid("Full name must not exceed 60 characters.")
            else -> ValidationResult.Valid
        }
    }

    fun validateUsername(username: String): ValidationResult {
        val trimmed = username.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid("Username is required.")
            trimmed.length < 3 -> ValidationResult.Invalid("Username must be at least 3 characters.")
            trimmed.length > 30 -> ValidationResult.Invalid("Username cannot exceed 30 characters.")
            !USERNAME_PATTERN.matcher(trimmed).matches() ->
                ValidationResult.Invalid("Username may only contain letters, numbers, and underscores.")
            else -> ValidationResult.Valid
        }
    }

    fun validateEmail(email: String): ValidationResult {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid("Email address is required.")
            !EMAIL_PATTERN.matcher(trimmed).matches() ->
                ValidationResult.Invalid("Please enter a valid email address.")
            else -> ValidationResult.Valid
        }
    }

    fun validatePhone(phone: String): ValidationResult {
        val trimmed = phone.trim()
        if (trimmed.isEmpty()) return ValidationResult.Valid // Phone is optional in some cases, or checked separately
        return when {
            !PHONE_PATTERN.matcher(trimmed).matches() ->
                ValidationResult.Invalid("Please enter a valid international phone number (e.g. +1234567890).")
            else -> ValidationResult.Valid
        }
    }

    fun validatePassword(password: String): ValidationResult {
        return when {
            password.length < 8 ->
                ValidationResult.Invalid("Password must be at least 8 characters long.")
            !password.any { it.isLetter() } ->
                ValidationResult.Invalid("Password must include at least one letter.")
            !password.any { it.isDigit() } ->
                ValidationResult.Invalid("Password must include at least one number.")
            else -> ValidationResult.Valid
        }
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): ValidationResult {
        return when {
            confirmPassword.isEmpty() ->
                ValidationResult.Invalid("Please confirm your password.")
            password != confirmPassword ->
                ValidationResult.Invalid("Passwords do not match.")
            else -> ValidationResult.Valid
        }
    }
}
