package com.example.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun WelcomeScreen(
    onNavigateToSignIn: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulsing")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAngle"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SleekZinc950
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle Navigation Radar Graphic in Background
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .align(Alignment.TopCenter)
            ) {
                val center = Offset(size.width / 2f, size.height * 0.45f)
                val maxRadius = size.width * 0.42f

                // Concentric circles
                val rings = listOf(0.25f, 0.5f, 0.75f, 1f)
                rings.forEach { fraction ->
                    drawCircle(
                        color = SleekBlue.copy(alpha = 0.08f),
                        radius = maxRadius * fraction,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Pulsing wave
                drawCircle(
                    color = SleekBlue.copy(alpha = (1f - pulseRadius) * 0.25f),
                    radius = maxRadius * pulseRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Crosshairs
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                drawLine(
                    color = SleekZinc800.copy(alpha = 0.5f),
                    start = Offset(center.x - maxRadius, center.y),
                    end = Offset(center.x + maxRadius, center.y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
                drawLine(
                    color = SleekZinc800.copy(alpha = 0.5f),
                    start = Offset(center.x, center.y - maxRadius),
                    end = Offset(center.x, center.y + maxRadius),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )

                // Rotating radar sweep line
                val rad = Math.toRadians(radarAngle.toDouble())
                val sweepEnd = Offset(
                    (center.x + maxRadius * Math.cos(rad)).toFloat(),
                    (center.y + maxRadius * Math.sin(rad)).toFloat()
                )
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(SleekBlue.copy(alpha = 0.4f), Color.Transparent),
                        start = center,
                        end = sweepEnd
                    ),
                    start = center,
                    end = sweepEnd,
                    strokeWidth = 2.dp.toPx()
                )

                // Satellite Nodes
                val node1 = Offset(center.x + maxRadius * 0.6f, center.y - maxRadius * 0.3f)
                val node2 = Offset(center.x - maxRadius * 0.45f, center.y + maxRadius * 0.5f)
                val node3 = Offset(center.x + maxRadius * 0.25f, center.y + maxRadius * 0.7f)

                drawCircle(color = SleekGreen, radius = 3.5.dp.toPx(), center = node1)
                drawCircle(color = SleekCyan, radius = 3.dp.toPx(), center = node2)
                drawCircle(color = SleekAmber, radius = 3.dp.toPx(), center = node3)
            }

            // Top-level Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Logo & Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(SleekBlue, SleekCyan)
                                )
                            )
                            .border(1.dp, SleekZinc700, RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "GlobalCore-X Logo",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "GLOBALCORE-X",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = SleekZinc100,
                        letterSpacing = 2.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Explore. Navigate. Track. Stay Connected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SleekZinc400,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Capabilities feature cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeaturePill(
                        icon = Icons.Default.GpsFixed,
                        iconTint = SleekBlue,
                        title = "High-Precision GPS & Tracking",
                        description = "Continuous multi-satellite telemetry & live breadcrumb recording"
                    )
                    FeaturePill(
                        icon = Icons.Default.AutoAwesome,
                        iconTint = SleekPurple,
                        title = "AI Route Optimization",
                        description = "Intelligent turn-by-turn routing with Gemini road analysis"
                    )
                    FeaturePill(
                        icon = Icons.Default.EmergencyShare,
                        iconTint = SleekSosRed,
                        title = "Emergency SOS & Live Relay",
                        description = "One-touch coordinates broadcasting to trusted emergency contacts"
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateToSignIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("welcome_sign_in_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SleekBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "SIGN IN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onNavigateToSignUp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("welcome_create_account_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SleekZinc100
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(SleekZinc700, SleekZinc600))
                        )
                    ) {
                        Text(
                            text = "CREATE ACCOUNT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "GlobalCore-X Security • End-to-End Encrypted Session",
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekZinc600,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SleekZinc900.copy(alpha = 0.8f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekZinc200
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekZinc500,
                    fontSize = 12.sp
                )
            }
        }
    }
}
