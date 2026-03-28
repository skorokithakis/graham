package com.stavros.graham

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.io.File

// Represents the readiness of a single expected model file or directory.
data class ModelFileStatus(
    val name: String,
    val present: Boolean,
    // Null when the file is absent or is a directory (size is not meaningful for directories).
    val sizeBytes: Long?,
)

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "About",
            style = MaterialTheme.typography.headlineMedium,
        )

        Text(
            text = "Graham — on-device voice assistant.",
            style = MaterialTheme.typography.bodyLarge,
        )

        HorizontalDivider()

        Text(
            text = "Version",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )

        HorizontalDivider()

        Text(
            text = "Stack",
            style = MaterialTheme.typography.titleMedium,
        )
        listOf(
            "UI: Jetpack Compose + Material 3",
            "STT: sherpa-onnx Parakeet TDT-CTC 110M + Silero VAD",
            "TTS: sherpa-onnx Piper VITS (en_US-amy-low)",
            "HTTP: OkHttp 4",
        ).forEach { line ->
            Text(
                text = "• $line",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun ModelStatusScreen() {
    val context = LocalContext.current

    // All checks are pure filesystem reads; no async work is needed.
    val asrStatuses = remember {
        val directory = File(context.filesDir, "parakeet-tdt-ctc-110m")
        listOf(
            "model.onnx",
            "tokens.txt",
            "silero_vad.onnx",
        ).map { filename ->
            val file = File(directory, filename)
            ModelFileStatus(
                name = filename,
                present = file.exists(),
                sizeBytes = if (file.exists()) file.length() else null,
            )
        }
    }

    val piperStatuses = remember {
        val directory = File(context.filesDir, "vits-piper-en_US-amy-low")
        val fileNames = listOf(
            "en_US-amy-low.onnx",
            "en_US-amy-low.onnx.json",
            "tokens.txt",
        )
        val fileStatuses = fileNames.map { filename ->
            val file = File(directory, filename)
            ModelFileStatus(
                name = filename,
                present = file.exists(),
                sizeBytes = if (file.exists()) file.length() else null,
            )
        }
        // espeak-ng-data is a directory; report its presence but not a size.
        val espeakDir = File(directory, "espeak-ng-data")
        fileStatuses + ModelFileStatus(
            name = "espeak-ng-data/",
            present = espeakDir.exists() && espeakDir.isDirectory,
            sizeBytes = null,
        )
    }

    // Probe sherpa-onnx runtime availability by attempting to resolve key JNI-backed
    // classes. This is the only meaningful runtime check available without actually
    // running inference, since the AAR is a compile-time dependency and its path is
    // not accessible at runtime.
    val sherpaStatuses = remember {
        listOf(
            "com.k2fsa.sherpa.onnx.OfflineRecognizer",
            "com.k2fsa.sherpa.onnx.OfflineTts",
            "com.k2fsa.sherpa.onnx.Vad",
        ).map { className ->
            val loadable = try {
                Class.forName(className)
                true
            } catch (_: Throwable) {
                // ClassNotFoundException covers missing classes; LinkageError and
                // UnsatisfiedLinkError cover native-library loading failures. All of
                // these mean the runtime is unavailable.
                false
            }
            ModelFileStatus(
                name = className.substringAfterLast('.'),
                present = loadable,
                sizeBytes = null,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Model status",
            style = MaterialTheme.typography.headlineMedium,
        )

        ModelSection(title = "Parakeet (filesDir/parakeet-tdt-ctc-110m)", statuses = asrStatuses)
        ModelSection(
            title = "Piper (filesDir/vits-piper-en_US-amy-low)",
            statuses = piperStatuses,
        )
        ModelSection(title = "Sherpa runtime", statuses = sherpaStatuses)
    }
}

@Composable
private fun ModelSection(title: String, statuses: List<ModelFileStatus>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        HorizontalDivider()
        statuses.forEach { status ->
            ModelStatusRow(status = status)
        }
    }
}

@Composable
private fun ModelStatusRow(status: ModelFileStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
        )
        val indicator = when {
            !status.present -> "✗ missing"
            status.sizeBytes != null -> "✓ ${formatSize(status.sizeBytes)}"
            else -> "✓ present"
        }
        Text(
            text = indicator,
            style = MaterialTheme.typography.bodySmall,
            color = if (status.present) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
    }
}
