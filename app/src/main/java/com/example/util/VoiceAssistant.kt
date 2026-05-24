package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.data.PreferencesManager
import java.util.Locale

class VoiceAssistant(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val prefs = PreferencesManager(context)

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("en"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("VoiceAssistant", "English language is not supported or missing data.")
            } else {
                isInitialized = true
            }
        } else {
            Log.e("VoiceAssistant", "Initialization failed!")
        }
    }

    fun speakProduct(productName: String, price: Double) {
        if (!isInitialized || !prefs.voiceEnabled) return

        val textToSpeak = when (prefs.language) {
            "ur" -> {
                // Urdu translation style
                val cleanPriceText = price.toInt().toString()
                "$productName, $cleanPriceText Rupay"
            }
            else -> {
                // English style
                val cleanPriceText = price.toInt().toString()
                "$productName, $cleanPriceText Rupees"
            }
        }

        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "PriceWalaScanSpeechId")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
