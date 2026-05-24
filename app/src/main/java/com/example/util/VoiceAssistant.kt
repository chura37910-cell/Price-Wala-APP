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
                // Speak price naturally in Urdu style (e.g., "Pepsi, teen so bees rupay")
                val priceInUrdu = convertToUrduWords(price.toInt())
                "$productName, $priceInUrdu rupay"
            }
            else -> {
                // English style
                val cleanPriceText = price.toInt().toString()
                "$productName, $cleanPriceText Rupees"
            }
        }

        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "PriceWalaScanSpeechId")
    }

    private fun convertToUrduWords(number: Int): String {
        if (number <= 0) return "sifar"
        var n = number
        val sb = java.lang.StringBuilder()
        
        if (n >= 100000) {
            val lakhs = n / 100000
            sb.append(convertToUrduWords(lakhs)).append(" lakh ")
            n %= 100000
        }
        
        if (n >= 1000) {
            val thousands = n / 1000
            sb.append(convertToUrduWords(thousands)).append(" hazaar ")
            n %= 1000
        }
        
        if (n >= 100) {
            val hundreds = n / 100
            val hundredWords = when(hundreds) {
                1 -> "ek"
                2 -> "do"
                3 -> "teen"
                4 -> "chaar"
                5 -> "paanch"
                6 -> "chey"
                7 -> "saat"
                8 -> "aath"
                9 -> "nau"
                else -> ""
            }
            sb.append(hundredWords).append(" so ")
            n %= 100
        }
        
        if (n > 0) {
            val word = when {
                n < 10 -> when(n) {
                    1 -> "ek"
                    2 -> "do"
                    3 -> "teen"
                    4 -> "chaar"
                    5 -> "paanch"
                    6 -> "chey"
                    7 -> "saat"
                    8 -> "aath"
                    9 -> "nau"
                    else -> ""
                }
                n in 10..19 -> when(n) {
                    10 -> "das"
                    11 -> "gyarah"
                    12 -> "barah"
                    13 -> "teerah"
                    14 -> "chaudah"
                    15 -> "pandrah"
                    16 -> "solah"
                    17 -> "satarah"
                    18 -> "atharah"
                    19 -> "unnees"
                    else -> ""
                }
                else -> {
                    val tens = n / 10
                    val units = n % 10
                    val tensWord = when(tens) {
                        2 -> "bees"
                        3 -> "tees"
                        4 -> "chalees"
                        5 -> "pachaas"
                        6 -> "saath"
                        7 -> "sattar"
                        8 -> "assi"
                        9 -> "navvey"
                        else -> ""
                    }
                    if (units > 0) {
                        val unitWord = when(units) {
                            1 -> "ek"
                            2 -> "do"
                            3 -> "teen"
                            4 -> "chaar"
                            5 -> "paanch"
                            6 -> "chey"
                            7 -> "saat"
                            8 -> "aath"
                            9 -> "nau"
                            else -> ""
                        }
                        "$tensWord aur $unitWord"
                    } else {
                        tensWord
                    }
                }
            }
            sb.append(word)
        }
        
        return sb.toString().trim().replace("\\s+".toRegex(), " ")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
