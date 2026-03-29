package com.stavros.graham

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavWriter {
    // WAV header is always 44 bytes for standard PCM format.
    private const val HEADER_SIZE = 44

    fun write(file: File, samples: ShortArray, sampleRate: Int, channels: Int = 1) {
        file.parentFile?.mkdirs()
        val dataSize = samples.size * 2
        FileOutputStream(file).use { stream ->
            stream.write(buildHeader(sampleRate, channels, dataSize))
            val byteBuffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in samples) {
                byteBuffer.putShort(sample)
            }
            stream.write(byteBuffer.array())
        }
    }

    private fun buildHeader(sampleRate: Int, channels: Int, dataSize: Int): ByteArray {
        val byteRate = sampleRate * channels * 2
        val blockAlign = channels * 2
        val buffer = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk descriptor.
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize + HEADER_SIZE - 8)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))

        // fmt sub-chunk.
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)           // Sub-chunk size for PCM.
        buffer.putShort(1)          // Audio format: PCM.
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(16)         // Bits per sample.

        // data sub-chunk.
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize)

        return buffer.array()
    }
}
