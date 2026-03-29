package com.stavros.graham

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val TAG = "SpeechRecognizer"
private const val ASSET_DIR = "parakeet-tdt-ctc-110m"
private const val MODEL_FILE = "model.onnx"
private const val TOKENS_FILE = "tokens.txt"
private const val VAD_MODEL_FILE = "silero_vad.onnx"
private const val SAMPLE_RATE = 16000

// Silero VAD expects exactly 512 samples per window at 16kHz.
private const val VAD_WINDOW_SIZE = 512

class SpeechRecognizer(
    private val context: Context,
    private val onResult: (String, String?) -> Unit,
    private val onError: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
) {
    var preferredInputDevice: AudioDeviceInfo? = null

    private var vad: Vad? = null
    private var recognizer: OfflineRecognizer? = null
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    // SupervisorJob so that a failed recording coroutine doesn't cancel the scope itself,
    // allowing a subsequent startListening() call to launch a new coroutine.
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile private var isListening = false

    // Copies model files from assets to filesDir on first launch so that sherpa-onnx
    // (which reads files by path, not via AssetManager) can find them. A sentinel file
    // is written after a successful copy so that a partial copy from a previous crash is
    // detected and re-done rather than silently used.
    private fun ensureModelsOnDisk(): File {
        val modelDir = File(context.filesDir, ASSET_DIR)
        val sentinel = File(modelDir, ".copy_complete")

        if (modelDir.exists() && !sentinel.exists()) {
            Log.w(TAG, "Model directory exists but sentinel is missing; re-copying")
            modelDir.deleteRecursively()
        }

        if (modelDir.exists()) {
            Log.d(TAG, "Models already on disk at ${modelDir.absolutePath}")
            return modelDir
        }

        Log.d(TAG, "Copying models from assets to ${modelDir.absolutePath}")
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

    suspend fun initialize(): Unit = withContext(Dispatchers.IO) {
        Log.d(TAG, "Initializing SpeechRecognizer")
        val modelDir = ensureModelsOnDisk()

        val sileroConfig = SileroVadModelConfig()
        sileroConfig.model = File(modelDir, VAD_MODEL_FILE).absolutePath
        sileroConfig.threshold = 0.5f
        sileroConfig.minSilenceDuration = 0.5f
        sileroConfig.minSpeechDuration = 0.25f
        sileroConfig.windowSize = VAD_WINDOW_SIZE
        sileroConfig.maxSpeechDuration = 30.0f

        val vadConfig = VadModelConfig()
        vadConfig.sileroVadModelConfig = sileroConfig
        vadConfig.sampleRate = SAMPLE_RATE
        vadConfig.numThreads = 1
        vadConfig.provider = "cpu"
        vadConfig.debug = false

        vad = Vad(assetManager = null, config = vadConfig)
        Log.d(TAG, "VAD initialized")

        val nemoConfig = OfflineNemoEncDecCtcModelConfig()
        nemoConfig.model = File(modelDir, MODEL_FILE).absolutePath

        val modelConfig = OfflineModelConfig()
        modelConfig.nemo = nemoConfig
        modelConfig.tokens = File(modelDir, TOKENS_FILE).absolutePath
        modelConfig.numThreads = 2
        modelConfig.provider = "cpu"
        modelConfig.debug = false

        val recognizerConfig = OfflineRecognizerConfig()
        recognizerConfig.featConfig.sampleRate = SAMPLE_RATE
        recognizerConfig.modelConfig = modelConfig

        recognizer = OfflineRecognizer(assetManager = null, config = recognizerConfig)
        Log.d(TAG, "OfflineRecognizer initialized")
    }

    fun startListening() {
        if (isListening) {
            Log.w(TAG, "startListening() called while already listening; ignoring")
            return
        }

        val currentVad = vad ?: run {
            onError("SpeechRecognizer not initialized")
            return
        }
        val currentRecognizer = recognizer ?: run {
            onError("SpeechRecognizer not initialized")
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        // Use at least 8 VAD windows worth of buffer to avoid overruns.
        val bufferSize = maxOf(minBufferSize, VAD_WINDOW_SIZE * 8 * 2)

        // VOICE_COMMUNICATION enables AGC/AEC/NS which helps BT mic quality but degrades
        // STT accuracy on the built-in mic. Use it only when routing to a BT device.
        val audioSource = if (preferredInputDevice != null) {
            MediaRecorder.AudioSource.VOICE_COMMUNICATION
        } else {
            MediaRecorder.AudioSource.MIC
        }

        val record = AudioRecord(
            audioSource,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            onError("AudioRecord failed to initialize")
            return
        }

        preferredInputDevice?.let { device ->
            val accepted = record.setPreferredDevice(device)
            Log.d(TAG, "setPreferredDevice(${device.productName}, type=${device.type}, id=${device.id}) -> $accepted")
        }

        audioRecord = record
        isListening = true
        record.startRecording()
        Log.d(TAG, "AudioRecord started; routedDevice=${record.routedDevice?.let { "${it.productName} type=${it.type}" } ?: "null"}")

        recordingJob = scope.launch {
            runRecordingLoop(record, currentVad, currentRecognizer)
        }
    }

    private suspend fun runRecordingLoop(
        record: AudioRecord,
        currentVad: Vad,
        currentRecognizer: OfflineRecognizer,
    ) {
        val shortBuffer = ShortArray(VAD_WINDOW_SIZE)
        var wasSpeechDetected = false

        try {
            while (isListening) {
                val samplesRead = record.read(shortBuffer, 0, VAD_WINDOW_SIZE)
                if (samplesRead <= 0) continue

                val floatSamples = FloatArray(samplesRead) { index ->
                    shortBuffer[index] / Short.MAX_VALUE.toFloat()
                }

                val rms = computeRms(floatSamples)
                withContext(Dispatchers.Main) { onRmsChanged(rms) }

                currentVad.acceptWaveform(floatSamples)

                val isSpeechNow = currentVad.isSpeechDetected()

                if (wasSpeechDetected && !isSpeechNow) {
                    // Speech segment just ended; drain any buffered segments.
                    while (!currentVad.empty()) {
                        val segment = currentVad.front()
                        currentVad.pop()
                        transcribeSegment(segment.samples, currentRecognizer)
                    }
                    currentVad.reset()
                }

                wasSpeechDetected = isSpeechNow
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Recording loop error", exception)
            withContext(Dispatchers.Main) { onError(exception.message ?: "Unknown recording error") }
        } finally {
            Log.d(TAG, "Recording loop exited")
        }
    }

    private suspend fun transcribeSegment(samples: FloatArray, currentRecognizer: OfflineRecognizer) {
        Log.d(TAG, "Transcribing segment of ${samples.size} samples")
        val audioFilePath = writeSttLog(samples)
        val stream = currentRecognizer.createStream()
        try {
            stream.acceptWaveform(samples, SAMPLE_RATE)
            currentRecognizer.decode(stream)
            val text = currentRecognizer.getResult(stream).text.trim()
            Log.d(TAG, "Transcription result: '$text'")
            if (text.isNotBlank()) {
                withContext(Dispatchers.Main) { onResult(text, audioFilePath) }
            }
        } finally {
            stream.release()
        }
    }

    private suspend fun writeSttLog(samples: FloatArray): String = withContext(Dispatchers.IO) {
        val shorts = ShortArray(samples.size) { index ->
            (samples[index].coerceIn(-1.0f, 1.0f) * Short.MAX_VALUE).toInt().toShort()
        }
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"))
        val file = File(context.cacheDir, "audio_logs/stt/stt_$timestamp.wav")
        WavWriter.write(file, shorts, SAMPLE_RATE)
        Log.d(TAG, "STT audio written to ${file.absolutePath}")
        file.absolutePath
    }

    private fun computeRms(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f
        val sumOfSquares = samples.fold(0f) { accumulator, sample -> accumulator + sample * sample }
        val linearRms = Math.sqrt((sumOfSquares / samples.size).toDouble()).toFloat()
        if (linearRms <= 0f) return 0f
        // Convert to dB and shift into the 0–10 range that WaveformIndicator expects.
        // 20*log10 of typical speech RMS (~0.05–0.3) gives roughly -26 to -10 dB.
        // Adding 30 maps this to roughly 4–20, then clamping to 0–10.
        val db = (20.0 * Math.log10(linearRms.toDouble())).toFloat() + 30f
        return db.coerceAtLeast(0f)
    }

    fun stopListening() {
        Log.d(TAG, "Stopping listening")
        isListening = false
        recordingJob?.cancel()
        recordingJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        vad?.reset()
    }

    fun destroy() {
        Log.d(TAG, "Destroying SpeechRecognizer")
        isListening = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        vad?.release()
        vad = null
        recognizer?.release()
        recognizer = null
        scope.cancel()
    }
}
