package com.example.ui.screens.support

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.remote.ApiClient
import com.example.data.remote.model.GeminiContent
import com.example.data.remote.model.GeminiPart
import com.example.data.remote.model.GeminiRequest
import com.example.service.GpsLocationData
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class SupportChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

private val STARTER_PROMPTS = listOf(
    "🗺️ How do I download offline maps?",
    "🛰️ How do I fix low GPS accuracy?",
    "⚡ How does battery saver tracking work?",
    "🚨 How does Emergency SOS work?",
    "💾 How do I save and export routes?",
    "📍 What does the Telemetry HUD show?"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSupportScreen(
    gpsLocation: GpsLocationData? = null,
    batteryLevel: Int = 85,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isTtsSpeaking by remember { mutableStateOf(false) }

    // Initialize TTS
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        textToSpeech = tts
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val messages = remember {
        mutableStateListOf(
            SupportChatMessage(
                text = "👋 Hello Explorer! I'm your GlobalCore-X Autonomous AI Support Specialist.\n\nI can assist you with:\n• Offline map downloads & Room storage management\n• Foreground live GPS tracking & telemetry diagnostics\n• Voice-guided navigation & route optimization\n• Emergency SOS & trusted contact setup\n\nHow can I help you today?",
                isFromUser = false
            )
        )
    }

    fun speakText(text: String) {
        val cleanText = text.replace(Regex("[*#•_]"), "").take(400)
        textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "SUPPORT_TTS")
        isTtsSpeaking = true
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        isTtsSpeaking = false
    }

    fun getOfflineFallbackResponse(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("offline") || q.contains("download") || q.contains("tile") -> {
                "📥 **Downloading Offline Maps**:\n1. Open **Settings & Profile** (top-right avatar icon).\n2. Scroll to the **Offline Maps & Tile Storage** section.\n3. Tap **Download Region** and choose your preferred radius (5km, 15km, or 30km around current GPS) or a city preset.\n4. Enable **Offline Only Mode** if you want the app to only read tiles from the Room database without consuming cellular data."
            }
            q.contains("accuracy") || q.contains("gps") || q.contains("satellite") || q.contains("signal") -> {
                "🛰️ **Improving GPS Accuracy**:\n• Ensure **Location** permission is set to **'Allow all the time'** and **Precise Location** is turned on.\n• In open sky conditions, accuracy is typically ±3m to ±5m.\n• If indoors or under dense cover, step outside or enable WiFi scanning in system settings to assist satellite triangulation."
            }
            q.contains("battery") || q.contains("saver") || q.contains("power") -> {
                "⚡ **Battery Optimization & Foreground Tracking**:\n• GlobalCore-X includes an intelligent **Battery Saver Mode** (configured in Settings).\n• When your battery drops below 20%, GPS polling dynamically switches from 2-second intervals to 12-second intervals, saving up to 60% battery life while preserving accurate route breadcrumbs in Room database."
            }
            q.contains("sos") || q.contains("emergency") -> {
                "🚨 **Emergency SOS Protocol**:\n• Tap the **SOS** tab on the bottom bar.\n• Press the red emergency button to start a 5-second countdown.\n• Once triggered, your live GPS coordinates, altitude, speed, and emergency distress beacon are dispatched via SMS / WhatsApp to all registered emergency contacts."
            }
            q.contains("save") || q.contains("route") || q.contains("record") || q.contains("export") -> {
                "💾 **Route Recording & Storage**:\n• Switch to the **Recording** tab.\n• Tap **Start Recording** to begin logging high-frequency coordinates into the local Room database.\n• When done, tap **Save Route** to preserve full elevation, speed stats, and polyline geometries."
            }
            q.contains("telemetry") || q.contains("hud") || q.contains("speed") || q.contains("altitude") -> {
                "📍 **Telemetry HUD Guide**:\n• The floating HUD on the Map Screen shows your **Real-Time Speed** (km/h or mph), **Barometric / GPS Altitude** in meters, **Compass Bearing**, and **GPS Fix Precision** in real-time."
            }
            else -> {
                "🛰️ **GlobalCore-X Assistant Response**:\nThank you for reaching out regarding: \"$query\".\n\nGlobalCore-X is equipped with full offline Room caching, foreground FusedLocation tracking, and smart AI navigation.\n\nNeed urgent assistance? You can also contact our lead engineer **Malik-X** directly on WhatsApp at **+256750985651**."
            }
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || isLoading) return
        val prompt = userText.trim()
        inputText = ""
        focusManager.clearFocus()

        messages.add(SupportChatMessage(text = prompt, isFromUser = true))
        isLoading = true

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)

            val apiKey = try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }

            var aiResponse: String? = null

            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                try {
                    val systemContext = "You are the GlobalCore-X Autonomous AI Support Specialist built into an advanced Android GPS Navigation app created by Malik-X. " +
                            "You provide clear, friendly, and expert assistance regarding GPS location tracking, offline map caching in Room database, FusedLocationProviderClient foreground service, emergency SOS alerts, route recording, and battery optimization. " +
                            "Current device context: GPS Lat=${gpsLocation?.latitude ?: 0.0}, Lng=${gpsLocation?.longitude ?: 0.0}, Battery=$batteryLevel%. " +
                            "Keep answers concise, actionable, well-structured with bullet points, and friendly."

                    val fullPrompt = "$systemContext\n\nUser Question: $prompt"

                    val request = GeminiRequest(
                        contents = listOf(
                            GeminiContent(
                                parts = listOf(GeminiPart(text = fullPrompt))
                            )
                        )
                    )

                    val response = withContext(Dispatchers.IO) {
                        ApiClient.geminiService.generateRouteSuggestion(apiKey, request)
                    }

                    aiResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                } catch (e: Exception) {
                    aiResponse = null
                }
            }

            // Fallback to rich embedded knowledge base if network is down or API key is not active
            if (aiResponse.isNullOrBlank()) {
                aiResponse = getOfflineFallbackResponse(prompt)
            }

            messages.add(SupportChatMessage(text = aiResponse, isFromUser = false))
            isLoading = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_support_screen"),
        containerColor = SleekBlack,
        topBar = {
            Surface(
                color = SleekZinc900,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SleekZinc800)
                                .testTag("btn_support_back")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = SleekZinc100,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(SleekBlue, SleekCyan))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "AI Support Agent",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SleekZinc100,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SleekGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        color = SleekGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "GlobalCore-X Specialist • Gemini 3.5",
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekZinc400,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Direct WhatsApp Support Quick Action
                    IconButton(
                        onClick = {
                            val waIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://wa.me/256750985651?text=Hello%20Malik-X%2C%20I%20need%20support%20with%20GlobalCoreX%20App.")
                            )
                            context.startActivity(waIntent)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SleekGreen.copy(alpha = 0.15f))
                            .border(1.dp, SleekGreen.copy(alpha = 0.3f), CircleShape)
                            .testTag("btn_whatsapp_support")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "WhatsApp Help",
                            tint = SleekGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = SleekZinc900,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800)
                ),
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Quick starter chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        items(STARTER_PROMPTS) { prompt ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SleekZinc800,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(SleekZinc700)
                                ),
                                modifier = Modifier
                                    .clickable { sendMessage(prompt) }
                                    .testTag("support_starter_chip")
                            ) {
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SleekZinc200,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Input field & Send button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    "Ask anything about maps, GPS, SOS...",
                                    color = SleekZinc500,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("support_input_field"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SleekZinc800,
                                unfocusedContainerColor = SleekZinc800,
                                focusedBorderColor = SleekBlue,
                                unfocusedBorderColor = SleekZinc700,
                                focusedTextColor = SleekZinc100,
                                unfocusedTextColor = SleekZinc100,
                                cursorColor = SleekBlue
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { sendMessage(inputText) })
                        )

                        IconButton(
                            onClick = { sendMessage(inputText) },
                            enabled = inputText.isNotBlank() && !isLoading,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (inputText.isNotBlank() && !isLoading) SleekBlue else SleekZinc800
                                )
                                .testTag("support_send_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = SleekBlue,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (inputText.isNotBlank()) Color.White else SleekZinc500,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // System Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SleekBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GpsFixed,
                                    contentDescription = null,
                                    tint = SleekBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "DIAGNOSTIC STATUS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SleekZinc500,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "GPS: ${if (gpsLocation?.isGpsActive == true) "Active (±${gpsLocation.accuracy.toInt()}m)" else "Standby"} • Batt: $batteryLevel%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekZinc200,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (isTtsSpeaking) {
                            IconButton(
                                onClick = { stopSpeaking() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeOff,
                                    contentDescription = "Stop TTS",
                                    tint = SleekAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { message ->
                ChatBubbleItem(
                    message = message,
                    onCopy = { text ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Support Message", text)
                        clipboard.setPrimaryClip(clip)
                    },
                    onSpeak = { text -> speakText(text) }
                )
            }

            if (isLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = SleekCyan,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "AI Agent is analyzing your query...",
                            style = MaterialTheme.typography.labelSmall,
                            color = SleekZinc400,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: SupportChatMessage,
    onCopy: (String) -> Unit,
    onSpeak: (String) -> Unit
) {
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
        ) {
            if (!message.isFromUser) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = SleekCyan,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "GlobalCore-X Agent",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            } else {
                Text(
                    text = "You",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekZinc400,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "• $timeStr",
                style = MaterialTheme.typography.labelSmall,
                color = SleekZinc600,
                fontSize = 10.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isFromUser) 18.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 18.dp
            ),
            color = if (message.isFromUser) SleekBlue else SleekZinc900,
            border = if (!message.isFromUser) {
                CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800)
                )
            } else null,
            modifier = Modifier
                .widthIn(max = 330.dp)
                .testTag(if (message.isFromUser) "user_chat_bubble" else "ai_chat_bubble")
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isFromUser) Color.White else SleekZinc100,
                    lineHeight = 20.sp,
                    fontSize = 13.5.sp
                )

                if (!message.isFromUser) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onSpeak(message.text) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Read aloud",
                                tint = SleekZinc400,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        IconButton(
                            onClick = { onCopy(message.text) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = SleekZinc400,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
