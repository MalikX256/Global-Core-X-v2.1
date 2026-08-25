package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.SecurityUtils
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onSignUp: (
        fullName: String,
        username: String,
        email: String,
        phone: String,
        country: String,
        avatarUrl: String,
        password: String,
        agreedToTerms: Boolean,
        agreedToPrivacy: Boolean
    ) -> Unit,
    onNavigateToSignIn: () -> Unit,
    onNavigateBackToWelcome: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Global / Auto-Detect") }
    var selectedAvatar by remember { mutableStateOf("avatar_1") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var agreedToTerms by remember { mutableStateOf(false) }
    var agreedToPrivacy by remember { mutableStateOf(false) }

    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    var localValidationError by remember { mutableStateOf<String?>(null) }

    // Live validation states
    val isLengthValid = password.length >= 8
    val hasLetter = password.any { it.isLetter() }
    val hasNumber = password.any { it.isDigit() }
    val isPasswordMatch = confirmPassword.isNotEmpty() && password == confirmPassword

    val avatarPresets = listOf(
        Pair("avatar_1", "🔷"),
        Pair("avatar_2", "⚡"),
        Pair("avatar_3", "🛰️"),
        Pair("avatar_4", "🛡️"),
        Pair("avatar_5", "🧭"),
        Pair("avatar_6", "🌐")
    )

    if (showTermsDialog) {
        TermsOfServiceDialog(onDismiss = { showTermsDialog = false })
    }
    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SleekZinc950
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBackToWelcome,
                    modifier = Modifier.testTag("sign_up_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = SleekZinc300
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "GLOBALCORE-X",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekZinc500,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = SleekZinc100,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Join the global telemetry & navigation network",
                style = MaterialTheme.typography.bodyMedium,
                color = SleekZinc400,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Error Banner
            val displayError = errorMessage ?: localValidationError
            if (!displayError.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSosRedMuted),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SleekSosRed.copy(alpha = 0.4f))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = SleekSosRed
                        )
                        Text(
                            text = displayError,
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekZinc100,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Avatar Selector (Optional)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "CHOOSE AVATAR BADGE (OPTIONAL)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekZinc400,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(avatarPresets) { (id, emoji) ->
                        val isSelected = selectedAvatar == id
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) SleekBlue.copy(alpha = 0.25f) else SleekZinc900)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) SleekBlue else SleekZinc800,
                                    shape = CircleShape
                                )
                                .clickable { selectedAvatar = id },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
            }

            // 1. Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    localValidationError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_up_fullname_input"),
                label = { Text("Full Name *") },
                placeholder = { Text("e.g. Malik Kasoma") },
                leadingIcon = {
                    Icon(Icons.Outlined.Badge, contentDescription = null, tint = SleekBlue)
                },
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = outlinedTextFieldColors(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Username
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    localValidationError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_up_username_input"),
                label = { Text("Username *") },
                placeholder = { Text("e.g. malik_core") },
                leadingIcon = {
                    Icon(Icons.Outlined.AlternateEmail, contentDescription = null, tint = SleekBlue)
                },
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = outlinedTextFieldColors(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Email Address
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    localValidationError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_up_email_input"),
                label = { Text("Email Address *") },
                placeholder = { Text("e.g. explorer@globalcore.com") },
                leadingIcon = {
                    Icon(Icons.Outlined.Email, contentDescription = null, tint = SleekBlue)
                },
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = outlinedTextFieldColors(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Phone Number (International)
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    localValidationError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_up_phone_input"),
                label = { Text("Phone Number (International) *") },
                placeholder = { Text("e.g. +1 555 0199 or +256 750 985651") },
                leadingIcon = {
                    Icon(Icons.Outlined.Phone, contentDescription = null, tint = SleekBlue)
                },
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = outlinedTextFieldColors(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Country / Region (Optional)
            OutlinedTextField(
                value = country,
                onValueChange = { country = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_up_country_input"),
                label = { Text("Country / Region (Optional)") },
                placeholder = { Text("e.g. United States, Uganda, United Kingdom") },
                leadingIcon = {
                    Icon(Icons.Outlined.Public, contentDescription = null, tint = SleekCyan)
                },
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = outlinedTextFieldColors(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Password
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    localValidationError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_up_password_input"),
                label = { Text("Password *") },
                placeholder = { Text("Minimum 8 characters") },
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = SleekBlue)
                },
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.testTag("sign_up_password_toggle")
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = if (passwordVisible) SleekBlue else SleekZinc500
                        )
                    }
                },
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                shape = RoundedCornerShape(14.dp),
                colors = outlinedTextFieldColors(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            // Password Security Hints
            if (password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PasswordCriterionChip(label = "8+ chars", isValid = isLengthValid)
                    PasswordCriterionChip(label = "Letter", isValid = hasLetter)
                    PasswordCriterionChip(label = "Number", isValid = hasNumber)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7. Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    localValidationError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_up_confirm_password_input"),
                label = { Text("Confirm Password *") },
                placeholder = { Text("Re-enter password") },
                leadingIcon = {
                    Icon(Icons.Outlined.LockReset, contentDescription = null, tint = SleekBlue)
                },
                trailingIcon = {
                    IconButton(
                        onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                        modifier = Modifier.testTag("sign_up_confirm_password_toggle")
                    ) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                            tint = if (confirmPasswordVisible) SleekBlue else SleekZinc500
                        )
                    }
                },
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                shape = RoundedCornerShape(14.dp),
                colors = outlinedTextFieldColors(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )

            if (confirmPassword.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isPasswordMatch) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (isPasswordMatch) SleekGreen else SleekSosRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isPasswordMatch) "Passwords match" else "Passwords do not match",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPasswordMatch) SleekGreen else SleekSosRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Checkboxes: Terms & Privacy
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = { agreedToTerms = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SleekBlue,
                            uncheckedColor = SleekZinc600,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.testTag("sign_up_terms_checkbox")
                    )
                    Text(
                        text = "I agree to the ",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekZinc300
                    )
                    Text(
                        text = "Terms of Service",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showTermsDialog = true }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = agreedToPrivacy,
                        onCheckedChange = { agreedToPrivacy = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SleekBlue,
                            uncheckedColor = SleekZinc600,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.testTag("sign_up_privacy_checkbox")
                    )
                    Text(
                        text = "I agree to the ",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekZinc300
                    )
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showPrivacyDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Create Account Action Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    val nameRes = SecurityUtils.validateFullName(fullName)
                    val userRes = SecurityUtils.validateUsername(username)
                    val emailRes = SecurityUtils.validateEmail(email)
                    val phoneRes = SecurityUtils.validatePhone(phone)
                    val passRes = SecurityUtils.validatePassword(password)
                    val matchRes = SecurityUtils.validateConfirmPassword(password, confirmPassword)

                    when {
                        !nameRes.isValid -> localValidationError = (nameRes as SecurityUtils.ValidationResult.Invalid).errorMessage
                        !userRes.isValid -> localValidationError = (userRes as SecurityUtils.ValidationResult.Invalid).errorMessage
                        !emailRes.isValid -> localValidationError = (emailRes as SecurityUtils.ValidationResult.Invalid).errorMessage
                        !phoneRes.isValid -> localValidationError = (phoneRes as SecurityUtils.ValidationResult.Invalid).errorMessage
                        !passRes.isValid -> localValidationError = (passRes as SecurityUtils.ValidationResult.Invalid).errorMessage
                        !matchRes.isValid -> localValidationError = (matchRes as SecurityUtils.ValidationResult.Invalid).errorMessage
                        !agreedToTerms -> localValidationError = "Please agree to the Terms of Service."
                        !agreedToPrivacy -> localValidationError = "Please agree to the Privacy Policy."
                        else -> {
                            localValidationError = null
                            onSignUp(
                                fullName.trim(),
                                username.trim().lowercase(),
                                email.trim().lowercase(),
                                phone.trim(),
                                country.trim(),
                                selectedAvatar,
                                password,
                                agreedToTerms,
                                agreedToPrivacy
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_up_submit_button"),
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SleekBlue,
                    contentColor = Color.White,
                    disabledContainerColor = SleekBlue.copy(alpha = 0.4f),
                    disabledContentColor = SleekZinc400
                )
            ) {
                if (isLoading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = "Creating your account...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    Text(
                        text = "CREATE ACCOUNT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Navigation to Sign In
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SleekZinc400
                )
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekBlue,
                    modifier = Modifier
                        .clickable(enabled = !isLoading) { onNavigateToSignIn() }
                        .testTag("sign_up_already_have_account_link")
                )
            }
        }
    }
}

@Composable
private fun PasswordCriterionChip(label: String, isValid: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isValid) SleekGreen.copy(alpha = 0.15f) else SleekZinc900,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isValid) SleekGreen else SleekZinc700
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isValid) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isValid) SleekGreen else SleekZinc500,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isValid) SleekGreen else SleekZinc400
            )
        }
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SleekBlue,
    unfocusedBorderColor = SleekZinc800,
    focusedLabelColor = SleekBlue,
    unfocusedLabelColor = SleekZinc400,
    focusedContainerColor = SleekZinc900,
    unfocusedContainerColor = SleekZinc900,
    focusedTextColor = SleekZinc100,
    unfocusedTextColor = SleekZinc200
)
