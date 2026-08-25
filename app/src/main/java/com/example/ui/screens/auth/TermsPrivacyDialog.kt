package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun TermsOfServiceDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SleekZinc900,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = SleekBlue)
                        Text(
                            text = "Terms of Service",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekZinc100
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SleekZinc400)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Acceptance of Terms",
                        fontWeight = FontWeight.Bold,
                        color = SleekZinc200,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "By accessing or using GLOBALCORE-X, you agree to be bound by these Terms of Service. GlobalCore-X provides real-time GPS tracking, AI route planning, and emergency SOS broadcasting tools.",
                        color = SleekZinc400,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "2. User Accounts & Security",
                        fontWeight = FontWeight.Bold,
                        color = SleekZinc200,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "You are responsible for maintaining the confidentiality of your authentication credentials. GlobalCore-X uses high-grade cryptographic hashing to protect your password. Any activity conducted under your authenticated internal ID is your responsibility.",
                        color = SleekZinc400,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "3. Location Tracking & Telemetry",
                        fontWeight = FontWeight.Bold,
                        color = SleekZinc200,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Location telemetry is used to deliver real-time waypoint recording and turn-by-turn guidance. You retain full control over your telemetry preferences in the Location Privacy controls.",
                        color = SleekZinc400,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "4. Emergency SOS Relay",
                        fontWeight = FontWeight.Bold,
                        color = SleekZinc200,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Emergency SOS broadcast transmits real-time coordinates to designated trusted emergency contacts. Use responsibly in actual danger or urgent assistance scenarios.",
                        color = SleekZinc400,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekBlue)
                ) {
                    Text("I Understand & Accept", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SleekZinc900,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SleekGreen)
                        Text(
                            text = "Privacy Policy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekZinc100
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SleekZinc400)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Zero Unsolicited Tracking",
                        fontWeight = FontWeight.Bold,
                        color = SleekZinc200,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "GLOBALCORE-X never tracks your location secretly. Telemetry is collected only during active GPS tracking or waypoint recording sessions explicitly triggered by you.",
                        color = SleekZinc400,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "2. Cryptographic Security",
                        fontWeight = FontWeight.Bold,
                        color = SleekZinc200,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "All passwords are salted and hashed with SHA-256 before local or remote persistence. Session tokens are securely managed in protected application storage.",
                        color = SleekZinc400,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "3. Data Ownership & Right to Deletion",
                        fontWeight = FontWeight.Bold,
                        color = SleekZinc200,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "You own your route logs, waypoints, and history. You may purge all recorded data or completely delete your account at any time with immediate cryptographic erasure.",
                        color = SleekZinc400,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekGreen, contentColor = SleekBlack)
                ) {
                    Text("Understood", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
