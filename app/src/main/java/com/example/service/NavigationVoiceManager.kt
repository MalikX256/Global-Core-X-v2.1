package com.example.service

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.remote.model.OsrmStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class NavigationVoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    
    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady = _isTtsReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking = _isSpeaking.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    private val _lastSpokenMessage = MutableStateFlow<String?>(null)
    val lastSpokenMessage = _lastSpokenMessage.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("NavigationVoiceManager", "Error initializing TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            tts?.setSpeechRate(0.95f)
            tts?.setPitch(1.0f)
            
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            })

            _isTtsReady.value = true
            Log.d("NavigationVoiceManager", "TextToSpeech successfully initialized")
        } else {
            Log.e("NavigationVoiceManager", "Failed to initialize TextToSpeech, status: $status")
            _isTtsReady.value = false
        }
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        if (muted) {
            stopSpeaking()
        }
    }

    fun toggleMute(): Boolean {
        val next = !_isMuted.value
        setMuted(next)
        return next
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (_isMuted.value || text.isBlank()) return

        _lastSpokenMessage.value = text

        if (_isTtsReady.value && tts != null) {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "NAV_VOICE_${System.currentTimeMillis()}")
            }
            tts?.speak(text, queueMode, params, "NAV_VOICE_${System.currentTimeMillis()}")
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e("NavigationVoiceManager", "Error stopping TTS", e)
        }
    }

    /**
     * Speaks real-time route voice briefing based on the calculated AI recommendations
     */
    fun speakAiRouteBriefing(
        destName: String,
        distanceKm: Double,
        durationMin: Int,
        travelMode: String,
        aiRecommendation: String
    ) {
        if (_isMuted.value) return

        // Clean AI recommendation markdown characters for natural voice pronunciation
        val cleanAiRec = aiRecommendation
            .replace("*", "")
            .replace("#", "")
            .replace("•", "")
            .replace("✓", "")
            .replace("\n", ". ")
            .trim()

        val briefingText = buildString {
            append("Starting navigation to ${destName.ifBlank { "your destination" }}. ")
            append("Total distance is ${String.format(Locale.US, "%.1f", distanceKm)} kilometers, estimated travel time is $durationMin minutes. ")
            if (cleanAiRec.isNotBlank()) {
                append("AI Route Guidance: $cleanAiRec. ")
            }
            append("Proceed to the highlighted route.")
        }

        speak(briefingText)
    }

    /**
     * Speaks step-by-step turn navigation instructions
     */
    fun speakNavigationStep(
        step: OsrmStep?,
        stepIndex: Int,
        totalSteps: Int,
        distanceMetersRemaining: Double
    ) {
        if (_isMuted.value || step == null) return

        val instruction = formatManeuverToSpokenText(step, distanceMetersRemaining)
        speak(instruction)
    }

    /**
     * Speaks destination arrival prompt
     */
    fun speakArrival(destName: String) {
        if (_isMuted.value) return
        val arrivalText = "You have arrived at ${destName.ifBlank { "your destination" }}. Navigation complete."
        speak(arrivalText)
    }

    /**
     * Formats an OSRM step into clear, natural voice instructions
     */
    fun formatManeuverToSpokenText(step: OsrmStep, distanceRemaining: Double): String {
        val distText = if (distanceRemaining > 1000) {
            "In ${String.format(Locale.US, "%.1f", distanceRemaining / 1000.0)} kilometers, "
        } else if (distanceRemaining > 20) {
            "In ${(distanceRemaining / 10).toInt() * 10} meters, "
        } else {
            "Now, "
        }

        val maneuverType = step.maneuver?.type?.lowercase() ?: "turn"
        val modifier = step.maneuver?.modifier?.lowercase() ?: "straight"
        val roadName = step.name.ifBlank { "the road" }

        val turnInstruction = when {
            maneuverType.contains("arrive") -> "you will arrive at your destination"
            maneuverType.contains("depart") -> "head $modifier on $roadName"
            maneuverType.contains("roundabout") -> {
                val mod = if (modifier.isNotBlank()) "take the $modifier exit" else "enter the roundabout"
                "$mod onto $roadName"
            }
            modifier.contains("slight right") -> "bear slightly right onto $roadName"
            modifier.contains("slight left") -> "bear slightly left onto $roadName"
            modifier.contains("right") -> "turn right onto $roadName"
            modifier.contains("left") -> "turn left onto $roadName"
            modifier.contains("uturn") || modifier.contains("u-turn") -> "make a legal U-turn on $roadName"
            modifier.contains("straight") || maneuverType.contains("continue") -> "continue straight on $roadName"
            else -> {
                step.maneuver?.instruction?.ifBlank { "proceed on $roadName" }
                    ?: "continue onto $roadName"
            }
        }

        return "$distText$turnInstruction."
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            _isTtsReady.value = false
        } catch (e: Exception) {
            Log.e("NavigationVoiceManager", "Error shutting down TTS", e)
        }
    }
}
