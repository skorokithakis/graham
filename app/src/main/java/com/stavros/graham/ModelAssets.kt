package com.stavros.graham

import android.content.Context
import android.util.Log
import java.io.File
import java.io.InputStream

// Copies model files from assets to filesDir on first launch so that native engines
// which read models by path (not via AssetManager) can find them. A sentinel file is
// written after a successful copy so that a partial copy from a previous crash is
// detected and re-done rather than silently used.
object ModelAssets {
    // Copies [assetDir] into filesDir and returns the resulting directory. If
    // [legacyDirName] is given and exists, it is deleted first (used to clean up an
    // orphaned model directory from a previous release). [tag] keeps the diagnostic
    // logs attributable to the calling manager; they are the main signal for
    // first-run copy problems.
    fun ensureOnDisk(
        context: Context,
        assetDir: String,
        tag: String,
        legacyDirName: String? = null,
    ): File {
        val modelDir = File(context.filesDir, assetDir)
        val sentinel = File(modelDir, ".copy_complete")

        if (legacyDirName != null) {
            val legacyDir = File(context.filesDir, legacyDirName)
            if (legacyDir.exists()) {
                Log.d(tag, "Removing legacy model directory ${legacyDir.absolutePath}")
                legacyDir.deleteRecursively()
            }
        }

        if (modelDir.exists() && !sentinel.exists()) {
            Log.w(tag, "Model directory exists but sentinel is missing; re-copying")
            modelDir.deleteRecursively()
        }

        if (modelDir.exists()) {
            Log.d(tag, "Model already on disk at ${modelDir.absolutePath}")
            return modelDir
        }

        Log.d(tag, "Copying model from assets to ${modelDir.absolutePath}")
        modelDir.mkdirs()
        copyAssetDir(context, assetDir, modelDir)
        sentinel.createNewFile()
        Log.d(tag, "Model copy complete")
        return modelDir
    }

    private fun copyAssetDir(context: Context, assetPath: String, destDir: File) {
        val assets = context.assets.list(assetPath) ?: return
        for (name in assets) {
            val childAssetPath = "$assetPath/$name"
            val destFile = File(destDir, name)
            val children = context.assets.list(childAssetPath)
            if (children != null && children.isNotEmpty()) {
                destFile.mkdirs()
                copyAssetDir(context, childAssetPath, destFile)
            } else {
                copyAssetFile(context, childAssetPath, destFile)
            }
        }
    }

    private fun copyAssetFile(context: Context, assetPath: String, destFile: File) {
        val inputStream: InputStream = context.assets.open(assetPath)
        inputStream.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
