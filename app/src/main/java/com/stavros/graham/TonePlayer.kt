package com.stavros.graham

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val SAMPLE_RATE = 44100

private val AUDIO_ATTRIBUTES = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
    .build()

private fun generateSine(frequencyHz: Double, durationMs: Int): ShortArray {
    val sampleCount = SAMPLE_RATE * durationMs / 1000
    val samples = ShortArray(sampleCount)
    for (index in 0 until sampleCount) {
        val angle = 2.0 * PI * frequencyHz * index / SAMPLE_RATE
        samples[index] = (sin(angle) * Short.MAX_VALUE).toInt().toShort()
    }
    return samples
}

// Generates a sine wave that sweeps linearly from startFrequencyHz to endFrequencyHz.
private fun generateSweep(startFrequencyHz: Double, endFrequencyHz: Double, durationMs: Int): ShortArray {
    val sampleCount = SAMPLE_RATE * durationMs / 1000
    val samples = ShortArray(sampleCount)
    var phase = 0.0
    for (index in 0 until sampleCount) {
        val fraction = index.toDouble() / sampleCount
        val instantFrequency = startFrequencyHz + (endFrequencyHz - startFrequencyHz) * fraction
        phase += 2.0 * PI * instantFrequency / SAMPLE_RATE
        samples[index] = (sin(phase) * Short.MAX_VALUE).toInt().toShort()
    }
    return samples
}

private fun playAndRelease(samples: ShortArray) {
    val track = AudioTrack.Builder()
        .setAudioAttributes(AUDIO_ATTRIBUTES)
        .setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        )
        .setBufferSizeInBytes(samples.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()
    try {
        track.write(samples, 0, samples.size)
        track.play()
        // MODE_STATIC plays the entire buffer once; we wait for the playhead to reach
        // the end so the caller blocks until the tone is fully audible before returning.
        while (track.playbackHeadPosition < samples.size) {
            Thread.sleep(10)
        }
        track.stop()
    } finally {
        track.release()
    }
}

object TonePlayer {
    // Short confirmation beep played when speech is received or TTS finishes.
    suspend fun playAckTone() {
        val samples = generateSine(frequencyHz = 880.0, durationMs = 200)
        withContext(Dispatchers.IO) {
            playAndRelease(samples)
        }
    }

    // Descending tone played on /stop to signal end of conversation, distinct from the ack beep.
    suspend fun playHangupTone() {
        val samples = generateSweep(startFrequencyHz = 880.0, endFrequencyHz = 440.0, durationMs = 500)
        withContext(Dispatchers.IO) {
            playAndRelease(samples)
        }
    }
}
