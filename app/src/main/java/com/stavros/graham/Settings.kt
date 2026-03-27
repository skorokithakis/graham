package com.stavros.graham

import android.content.Context

private const val PREFS_NAME = "graham_settings"
private const val KEY_SERVER_URL = "serverUrl"
private const val KEY_BODY_TEMPLATE = "bodyTemplate"
private const val KEY_TTS_SPEED = "ttsSpeed"

const val DEFAULT_SERVER_URL = "http://10.0.2.2:3000/chat"
const val DEFAULT_BODY_TEMPLATE = """{"message": "${'$'}transcript", "source": "graham", "sender": "stavros"}"""
const val DEFAULT_TTS_SPEED = 1.3f

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var bodyTemplate: String
        get() = prefs.getString(KEY_BODY_TEMPLATE, DEFAULT_BODY_TEMPLATE) ?: DEFAULT_BODY_TEMPLATE
        set(value) = prefs.edit().putString(KEY_BODY_TEMPLATE, value).apply()

    var ttsSpeed: Float
        get() = prefs.getFloat(KEY_TTS_SPEED, DEFAULT_TTS_SPEED)
        set(value) = prefs.edit().putFloat(KEY_TTS_SPEED, value.coerceIn(0.8f, 1.8f)).apply()
}
