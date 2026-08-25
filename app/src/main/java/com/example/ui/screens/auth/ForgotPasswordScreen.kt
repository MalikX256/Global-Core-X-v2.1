package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.SecurityUtils
import com.example.ui.theme.*

enum class ForgotPasswordStep {
    REQUEST_CODE,
    ENTER_NEW_PASSWORD,
    SUCCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onRequestResetCode: (email: String, onCodeSent: (code: String) -> Unit) -> Unit,
    onResetPassword: (email: String, code: String, newPass: String, onSuccess: () -> Unit) -> Unit,
    onNavigateBackToSignIn: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    var currentStep by remember { mutableStateOf(ForgotPasswordStep.REQUEST_CODE) }
    var email by remember { mutableStateOf("") }
    var resetCode by remember { mutableStateOf("") }
    var simulatedCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

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
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentStep == ForgotPasswordStep.ENTER_NEW_PASSWORD) {
                                currentStep = ForgotPasswordStep.REQUEST_CODE
                            } else {
                                onNavigateBackToSignIn()
                            }
                        },
                        modifier = Modifier.testTag("forgot_password_back_button")
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

                Spacer(modifier = Modifier.height(24.dp))

                when (currentStep) {
                    ForgotPasswordStep.REQUEST_CODE -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SleekBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = null, tint = SleekBlue, modifier = Modifier.size(32.dp))
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Reset Password",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekZinc100
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Enter your registered email address. If an account is associated, you will receive a secure 6-digit reset code.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SleekZinc400,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        val displayErr = errorMessage ?: localError
                        if (!displayErr.isNullOrBlank()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SleekSosRedMuted)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = SleekSosRed)
                                    Text(text = displayErr, style = MaterialTheme.typography.bodySmall, color = SleekZinc100)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                localError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_password_email_input"),
                            label = { Text("Account Email") },
                            placeholder = { Text("e.g. explorer@globalcore.com") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Email, contentDescription = null, tint = SleekBlue)
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SleekBlue,
                                unfocusedBorderColor = SleekZinc800,
                                focusedLabelColor = SleekBlue,
                                unfocusedLabelColor = SleekZinc400,
                                focusedContainerColor = SleekZinc900,
                                unfocusedContainerColor = SleekZinc900,
                                focusedTextColor = SleekZinc100,
                                unfocusedTextColor = SleekZinc200
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    val emailRes = SecurityUtils.validateEmail(email)
                                    if (!emailRes.isValid) {
                                        localError = (emailRes as SecurityUtils.ValidationResult.Invalid).errorMessage
                                    } else {
                                        onRequestResetCode(email.trim()) { code ->
                                            simulatedCode = code
                                            resetCode = code
                                            currentStep = ForgotPasswordStep.ENTER_NEW_PASSWORD
                                        }
                                    }
                                }
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                val emailRes = SecurityUtils.validateEmail(email)
                                if (!emailRes.isValid) {
                                    localError = (emailRes as SecurityUtils.ValidationResult.Invalid).errorMessage
                                } else {
                                    onRequestResetCode(email.trim()) { code ->
                                        simulatedCode = code
                                        resetCode = code
                                        currentStep = ForgotPasswordStep.ENTER_NEW_PASSWORD
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("send_reset_code_button"),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekBlue, contentColor = Color.White)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.5.dp)
                            } else {
                                Text("SEND RESET CODE", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }

                    ForgotPasswordStep.ENTER_NEW_PASSWORD -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SleekGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Password, contentDescription = null, tint = SleekGreen, modifier = Modifier.size(32.dp))
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Create New Password",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekZinc100
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Enter the 6-digit code sent to $email along with your new credentials.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SleekZinc400,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (simulatedCode.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SleekBlue.copy(alpha = 0.12f))
                            ) {
                                Text(
                                    text = "Reset Code: $simulatedCode (Pre-filled for testing)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekBlue,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        val displayErr = errorMessage ?: localError
                        if (!displayErr.isNullOrBlank()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SleekSosRedMuted)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = SleekSosRed)
                                    Text(text = displayErr, style = MaterialTheme.typography.bodySmall, color = SleekZinc100)
                                }
                            }
                        }

                        // Code Field
                        OutlinedTextField(
                            value = resetCode,
                            onValueChange = {
                                resetCode = it
                                localError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_code_input"),
                            label = { Text("6-Digit Reset Code") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = SleekBlue) },
                            singleLine = true,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SleekBlue,
                                unfocusedBorderColor = SleekZinc800,
                                focusedContainerColor = SleekZinc900,
                                unfocusedContainerColor = SleekZinc900,
                                focusedTextColor = SleekZinc100,
                                unfocusedTextColor = SleekZinc200
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // New Password Field
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                localError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_new_password_input"),
                            label = { Text("New Password") },
                            placeholder = { Text("Minimum 8 characters") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = SleekBlue) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = null,
                                        tint = SleekZinc500
                                    )
                                }
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SleekBlue,
                                unfocusedBorderColor = SleekZinc800,
                                focusedContainerColor = SleekZinc900,
                                unfocusedContainerColor = SleekZinc900,
                                focusedTextColor = SleekZinc100,
                                unfocusedTextColor = SleekZinc200
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Confirm New Password
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                localError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_confirm_password_input"),
                            label = { Text("Confirm New Password") },
                            leadingIcon = { Icon(Icons.Outlined.LockReset, contentDescription = null, tint = SleekBlue) },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = null,
                                        tint = SleekZinc500
                                    )
                                }
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SleekBlue,
                                unfocusedBorderColor = SleekZinc800,
                                focusedContainerColor = SleekZinc900,
                                unfocusedContainerColor = SleekZinc900,
                                focusedTextColor = SleekZinc100,
                                unfocusedTextColor = SleekZinc200
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                val passRes = SecurityUtils.validatePassword(newPassword)
                                val matchRes = SecurityUtils.validateConfirmPassword(newPassword, confirmPassword)
                                when {
                                    resetCode.isBlank() -> localError = "Please enter the 6-digit code."
                                    !passRes.isValid -> localError = (passRes as SecurityUtils.ValidationResult.Invalid).errorMessage
                                    !matchRes.isValid -> localError = (matchRes as SecurityUtils.ValidationResult.Invalid).errorMessage
                                    else -> {
                                        onResetPassword(email.trim(), resetCode.trim(), newPassword) {
                                            currentStep = ForgotPasswordStep.SUCCESS
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_new_password_button"),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekBlue, contentColor = Color.White)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.5.dp)
                            } else {
                                Text("UPDATE PASSWORD", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }

                    ForgotPasswordStep.SUCCESS -> {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SleekGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SleekGreen, modifier = Modifier.size(40.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Password Reset Complete",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekZinc100,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Your password has been successfully updated with high-grade cryptographic hashing. You may now sign in with your new credentials.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SleekZinc400,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = onNavigateBackToSignIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("return_to_sign_in_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekBlue, contentColor = Color.White)
                        ) {
                            Text("RETURN TO SIGN IN", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // Bottom Link
            if (currentStep != ForgotPasswordStep.SUCCESS) {
                TextButton(
                    onClick = onNavigateBackToSignIn,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(text = "Remember password? Sign In", style = MaterialTheme.typography.bodyMedium, color = SleekBlue)
                }
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }
}
