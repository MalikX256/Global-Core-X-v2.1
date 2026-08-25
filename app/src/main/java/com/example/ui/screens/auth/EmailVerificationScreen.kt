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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun EmailVerificationScreen(
    email: String,
    generatedCode: String = "",
    isLoading: Boolean,
    errorMessage: String?,
    successMessage: String?,
    onVerifyCode: (code: String) -> Unit,
    onResendCode: () -> Unit,
    onContinueToApp: () -> Unit,
    onBackToSignIn: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var verificationCode by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    // Pre-fill with sandbox code if provided for testing ease
    LaunchedEffect(generatedCode) {
        if (generatedCode.isNotBlank() && verificationCode.isBlank()) {
            verificationCode = generatedCode
        }
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
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackToSignIn,
                        modifier = Modifier.testTag("verification_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Sign In",
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

                Spacer(modifier = Modifier.height(28.dp))

                // Hero Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SleekBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = null,
                        tint = SleekBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "ACCOUNT CREATED",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = SleekGreen,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Verify Your Email",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekZinc100
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "We have dispatched a 6-digit confirmation code to your email address:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SleekZinc400,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800)
                    )
                ) {
                    Text(
                        text = email.ifBlank { "explorer@globalcore.com" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekZinc100,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sandbox Helper Box
                if (generatedCode.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekBlue.copy(alpha = 0.12f)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(SleekBlue.copy(alpha = 0.3f))
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = SleekBlue)
                            Column {
                                Text(
                                    text = "Development Sandbox Code",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekBlue
                                )
                                Text(
                                    text = "Verification code: $generatedCode (Tap to copy/fill)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekZinc300,
                                    modifier = Modifier.clickable { verificationCode = generatedCode }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Error or Success Banner
                val displayErr = errorMessage ?: localError
                if (!displayErr.isNullOrBlank()) {
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
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = SleekSosRed)
                            Text(
                                text = displayErr,
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekZinc100
                            )
                        }
                    }
                }

                if (!successMessage.isNullOrBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekGreenMuted),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(SleekGreenBorder)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SleekGreen)
                            Text(
                                text = successMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekZinc100
                            )
                        }
                    }
                }

                // 6-digit Code Input
                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = {
                        if (it.length <= 8) {
                            verificationCode = it
                            localError = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_verification_code_input"),
                    label = { Text("6-Digit Verification Code") },
                    placeholder = { Text("e.g. 739281") },
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = SleekBlue)
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
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (verificationCode.trim().length < 4) {
                                localError = "Please enter the verification code."
                            } else {
                                onVerifyCode(verificationCode.trim())
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Verify Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (verificationCode.trim().isBlank()) {
                            localError = "Please enter the verification code."
                        } else {
                            onVerifyCode(verificationCode.trim())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("verify_email_submit_button"),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SleekGreen,
                        contentColor = SleekBlack
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SleekBlack, strokeWidth = 2.5.dp)
                    } else {
                        Text(
                            text = "VERIFY EMAIL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Resend Code Button
                OutlinedButton(
                    onClick = onResendCode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("resend_verification_button"),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekZinc200),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800)
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESEND VERIFICATION CODE")
                }
            }

            // Bottom Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButton(
                    onClick = onContinueToApp,
                    modifier = Modifier.testTag("verification_continue_to_app_button")
                ) {
                    Text(
                        text = "Continue to GlobalCore-X Home →",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekBlue
                    )
                }

                TextButton(onClick = onBackToSignIn) {
                    Text(
                        text = "Back to Sign In",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekZinc500
                    )
                }
            }
        }
    }
}
