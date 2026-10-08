package com.example.voiceai.audio

import android.media.AudioFormat

object AudioConfig {
    const val SAMPLE_RATE = 16000
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    const val CHUNK_DURATION_MS = 40

    private const val BYTES_PER_SAMPLE = 2
    const val CHUNK_SIZE_BYTES = SAMPLE_RATE * BYTES_PER_SAMPLE * CHUNK_DURATION_MS / 1000
}
