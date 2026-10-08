package com.example.voiceai.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class AndroidTtsEngine(context: Context) : TextToSpeech.OnInitListener {
    private var textToSpeech: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            ready = false
            return
        }

        val tts = textToSpeech ?: return
        val result = tts.setLanguage(Locale("pt", "BR"))
        ready = result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun speak(text: String) {
        val tts = textToSpeech ?: return
        if (!ready) return

        tts.stop()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "assistant-${System.nanoTime()}")
    }

    fun stop() {
        textToSpeech?.stop()
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        ready = false
    }
}
