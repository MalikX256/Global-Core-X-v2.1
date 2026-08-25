package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    isLoading: Boolean,
    errorMessage: String?,
    savedIdentifier: String? = null,
    onSignIn: (identifier: String, password: String, rememberMe: Boolean) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateBackToWelcome: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var identifier by remember { mutableStateOf(savedIdentifier ?: "") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var localValidationErr by remember { mutableStateOf<String?>(null) }

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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBackToWelcome,
                        modifier = Modifier.testTag("sign_in_back_button")
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

                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekZinc100
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Sign in to access your navigation telemetry & live routes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SleekZinc400
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Error Message Box
                val displayError = errorMessage ?: localValidationErr
                if (!displayError.isNullOrBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp),
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

                // Demo Account Quick Fill Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable {
                            identifier = "explorer@globalcore.com"
                            password = "password123"
                            localValidationErr = null
                        }
                        .testTag("demo_account_pill"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekBlue.copy(alpha = 0.12f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SleekBlue.copy(alpha = 0.35f))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = SleekBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Demo Account Available",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SleekZinc200,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "explorer@globalcore.com • password123",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekBlue,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SleekBlue.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "Autofill",
                                color = SleekZinc100,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Field 1: Email / Username
                OutlinedTextField(
                    value = identifier,
                    onValueChange = {
                        identifier = it
                        localValidationErr = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_in_identifier_input"),
                    label = { Text("Email or Username") },
                    placeholder = { Text("e.g. malik or malik@globalcore.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = if (identifier.isNotEmpty()) SleekBlue else SleekZinc500
                        )
                    },
                    trailingIcon = {
                        if (identifier.isNotEmpty()) {
                            IconButton(onClick = { identifier = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SleekZinc500)
                            }
                        }
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
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Field 2: Password with Eye Icon
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        localValidationErr = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_in_password_input"),
                    label = { Text("Password") },
                    placeholder = { Text("Enter your secure password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = if (password.isNotEmpty()) SleekBlue else SleekZinc500
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.testTag("sign_in_password_toggle")
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
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (identifier.isBlank() || password.isBlank()) {
                                localValidationErr = "Please enter both your identifier and password."
                            } else {
                                onSignIn(identifier.trim(), password, rememberMe)
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Options Row: Remember Me + Forgot Password
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { rememberMe = !rememberMe }
                            .padding(end = 6.dp)
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = SleekBlue,
                                uncheckedColor = SleekZinc600,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.testTag("sign_in_remember_me_checkbox")
                        )
                        Text(
                            text = "Remember me",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekZinc300
                        )
                    }

                    TextButton(
                        onClick = onNavigateToForgotPassword,
                        enabled = !isLoading,
                        modifier = Modifier.testTag("sign_in_forgot_password_button")
                    ) {
                        Text(
                            text = "Forgot password?",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Sign In Action Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (identifier.isBlank() || password.isBlank()) {
                            localValidationErr = "Please fill in all required fields."
                        } else {
                            onSignIn(identifier.trim(), password, rememberMe)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("sign_in_submit_button"),
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
                                text = "Signing in...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        Text(
                            text = "SIGN IN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                         )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Demo Login Button
                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        onSignIn("explorer@globalcore.com", "password123", true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sign_in_quick_demo_button"),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SleekZinc900,
                        contentColor = SleekCyan
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SleekCyan.copy(alpha = 0.5f))
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = SleekCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Quick Demo Access (Explore as Guest)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Bottom Switch to Register
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SleekZinc400
                )
                Text(
                    text = "Create one",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekBlue,
                    modifier = Modifier
                        .clickable(enabled = !isLoading) { onNavigateToSignUp() }
                        .testTag("sign_in_create_one_link")
                )
            }
        }
    }
}
