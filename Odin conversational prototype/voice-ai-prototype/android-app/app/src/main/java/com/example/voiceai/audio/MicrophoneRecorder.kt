package com.example.voiceai.audio

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MicrophoneRecorder(private val scope: CoroutineScope) {
    private val recording = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(
        onChunk: (ByteArray) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (recording.getAndSet(true)) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            AudioConfig.SAMPLE_RATE,
            AudioConfig.CHANNEL_CONFIG,
            AudioConfig.ENCODING
        )
        val bufferSize = maxOf(minBufferSize, AudioConfig.CHUNK_SIZE_BYTES * 2)

        val recorder = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioConfig.ENCODING)
                    .setSampleRate(AudioConfig.SAMPLE_RATE)
                    .setChannelMask(AudioConfig.CHANNEL_CONFIG)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()

        audioRecord = recorder
        recordingJob = scope.launch(Dispatchers.IO) {
            try {
                recorder.startRecording()
                val buffer = ByteArray(AudioConfig.CHUNK_SIZE_BYTES)

                while (isActive && recording.get()) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        onChunk(buffer.copyOf(read))
                    } else if (read < 0) {
                        throw IllegalStateException("AudioRecord read failed: $read")
                    }
                }
            } catch (error: Throwable) {
                if (recording.get()) {
                    onError(error)
                }
            } finally {
                releaseRecorder()
            }
        }
    }

    fun stop() {
        if (!recording.getAndSet(false)) return
        runCatching {
            audioRecord?.stop()
        }
        recordingJob?.cancel()
        recordingJob = null
    }

    @Synchronized
    private fun releaseRecorder() {
        val recorder = audioRecord ?: return
        runCatching {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop()
            }
        }
        recorder.release()
        audioRecord = null
    }
}
