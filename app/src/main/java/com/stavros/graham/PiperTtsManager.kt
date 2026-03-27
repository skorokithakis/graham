package com.stavros.graham

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

private const val TAG = "PiperTtsManager"
private const val ASSET_DIR = "vits-piper-en_US-amy-low"
private const val MODEL_FILE = "en_US-amy-low.onnx"
private const val TOKENS_FILE = "tokens.txt"
private const val ESPEAK_DATA_DIR = "espeak-ng-data"

class PiperTtsManager(private val context: Context) {
    private var tts: OfflineTts? = null
    private var audioTrack: AudioTrack? = null
    @Volatile private var stopped = false
    private val settings = Settings(context)

    // Copies the model files from assets to the filesystem on first launch so that
    // espeak-ng (which reads files by path, not via AssetManager) can find them.
    // A sentinel file is written after a successful copy so that a partial copy from a
    // previous crash is detected and re-done rather than silently used.
    private fun ensureModelOnDisk(): File {
        val modelDir = File(context.filesDir, ASSET_DIR)
        val sentinel = File(modelDir, ".copy_complete")

        if (modelDir.exists() && !sentinel.exists()) {
            Log.w(TAG, "Model directory exists but sentinel is missing; re-copying")
            modelDir.deleteRecursively()
        }

        if (modelDir.exists()) {
            Log.d(TAG, "Model already on disk at ${modelDir.absolutePath}")
            return modelDir
        }

        Log.d(TAG, "Copying model from assets to ${modelDir.absolutePath}")
        modelDir.mkdirs()
        copyAssetDir(ASSET_DIR, modelDir)
        sentinel.createNewFile()
        Log.d(TAG, "Model copy complete")
        return modelDir
    }

    private fun copyAssetDir(assetPath: String, destDir: File) {
        val assets = context.assets.list(assetPath) ?: return
        for (name in assets) {
            val childAssetPath = "$assetPath/$name"
            val destFile = File(destDir, name)
            val children = context.assets.list(childAssetPath)
            if (children != null && children.isNotEmpty()) {
                destFile.mkdirs()
                copyAssetDir(childAssetPath, destFile)
            } else {
                copyAssetFile(childAssetPath, destFile)
            }
        }
    }

    private fun copyAssetFile(assetPath: String, destFile: File) {
        val inputStream: InputStream = context.assets.open(assetPath)
        inputStream.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    suspend fun initialize(): Unit = withContext(Dispatchers.Default) {
        Log.d(TAG, "Initializing PiperTtsManager")
        val modelDir = ensureModelOnDisk()

        val vitsConfig = OfflineTtsVitsModelConfig(
            model = File(modelDir, MODEL_FILE).absolutePath,
            lexicon = "",
            tokens = File(modelDir, TOKENS_FILE).absolutePath,
            dataDir = File(modelDir, ESPEAK_DATA_DIR).absolutePath,
            dictDir = "",
            noiseScale = 0.667f,
            noiseScaleW = 0.8f,
            lengthScale = 1.0f,
        )

        // OfflineTtsModelConfig has many fields for different model types; we only set
        // the ones relevant to VITS/Piper. The no-arg constructor provides empty defaults
        // for all other model type configs.
        val modelConfig = OfflineTtsModelConfig()
        modelConfig.vits = vitsConfig
        modelConfig.numThreads = 2
        modelConfig.debug = false
        modelConfig.provider = "cpu"

        val config = OfflineTtsConfig()
        config.model = modelConfig

        tts = OfflineTts(assetManager = null, config = config)
        Log.d(TAG, "PiperTtsManager initialized, sampleRate=${tts!!.sampleRate()}")
    }

    suspend fun speak(text: String): Unit = withContext(Dispatchers.Default) {
        stopped = false
        val engine = tts ?: run {
            Log.w(TAG, "speak() called before initialize()")
            return@withContext
        }

        val speed = settings.ttsSpeed
        Log.d(TAG, "Synthesizing: $text (speed=$speed)")
        val audio = engine.generate(text = text, sid = 0, speed = speed)
        Log.d(TAG, "Synthesis done: ${audio.samples.size} samples at ${audio.sampleRate} Hz")

        val shorts = floatArrayToShortArray(audio.samples)
        playAudio(shorts, audio.sampleRate)
    }

    fun stop() {
        stopped = true
    }

    fun destroy() {
        Log.d(TAG, "Destroying PiperTtsManager")
        audioTrack?.release()
        audioTrack = null
        tts?.release()
        tts = null
    }

    private fun floatArrayToShortArray(floats: FloatArray): ShortArray {
        val shorts = ShortArray(floats.size)
        for (i in floats.indices) {
            val clamped = floats[i].coerceIn(-1.0f, 1.0f)
            shorts[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
        }
        return shorts
    }

    // Plays the given PCM samples synchronously, returning only after playback completes.
    private suspend fun playAudio(samples: ShortArray, sampleRate: Int) {
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()

        val chunkSize = bufferSize / 2
        var offset = 0
        while (offset < samples.size) {
            currentCoroutineContext().ensureActive()
            if (stopped) break
            val end = minOf(offset + chunkSize, samples.size)
            track.write(samples, offset, end - offset)
            offset = end
        }

        try {
            track.stop()
        } catch (_: IllegalStateException) {
            // Already stopped.
        } finally {
            track.release()
        }
        audioTrack = null
        Log.d(TAG, "Playback complete")
    }
}
