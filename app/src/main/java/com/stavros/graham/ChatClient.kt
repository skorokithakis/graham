package com.stavros.graham

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ChatClient(private val serverUrl: String, private val bodyTemplate: String) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
    private val jsonMediaType = "application/json".toMediaType()

    fun shutdown() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }

    suspend fun sendMessage(text: String): String = withContext(Dispatchers.IO) {
        val escapedText = jsonEscape(text)
        val jsonBody = bodyTemplate.replace("\$transcript", escapedText)

        // Extract basic auth credentials from the URL if present (user:pass@host).
        val (cleanUrl, authHeader) = parseBasicAuth(serverUrl)

        val requestBuilder = Request.Builder()
            .url(cleanUrl)
            .post(jsonBody.toRequestBody(jsonMediaType))

        if (authHeader != null) {
            requestBuilder.header("Authorization", authHeader)
        }

        httpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Server returned ${response.code}")
            }
            val body = response.body?.string() ?: ""
            try {
                JSONObject(body).getString("response")
            } catch (_: Exception) {
                body
            }
        }
    }

    companion object {
        // Escapes a string for safe embedding as a JSON string value. Using JSONObject
        // to produce the escaped form avoids hand-rolling the full JSON escape table.
        fun jsonEscape(text: String): String {
            val serialized = JSONObject().put("x", text).toString()
            // serialized is {"x":"<escaped>"} — extract the value between the first `":"`
            // and the last `"}`.
            val start = serialized.indexOf("\":\"") + 3
            val end = serialized.lastIndexOf("\"}")
            return if (start in 3..end) serialized.substring(start, end) else text
        }

        // Parses basic auth credentials embedded in the URL (scheme://user:pass@host/path).
        // Returns the URL with credentials stripped and the Authorization header value,
        // or the original URL and null if no credentials are present.
        fun parseBasicAuth(url: String): Pair<String, String?> {
            val schemeEnd = url.indexOf("://")
            if (schemeEnd == -1) return Pair(url, null)

            val afterScheme = url.substring(schemeEnd + 3)
            val atIndex = afterScheme.indexOf('@')
            if (atIndex == -1) return Pair(url, null)

            val credentials = afterScheme.substring(0, atIndex)
            val rest = afterScheme.substring(atIndex + 1)
            val cleanUrl = url.substring(0, schemeEnd + 3) + rest

            val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            return Pair(cleanUrl, "Basic $encoded")
        }
    }
}
