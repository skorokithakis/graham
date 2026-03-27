package com.stavros.graham

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showSettings by remember { mutableStateOf(false) }
                    val conversationViewModel: ConversationViewModel = viewModel()

                    if (showSettings) {
                        BackHandler { showSettings = false }
                        SettingsScreen(
                            onSave = {
                                conversationViewModel.reloadSettings()
                                showSettings = false
                            },
                        )
                    } else {
                        ConversationScreen(
                            conversationViewModel = conversationViewModel,
                            onOpenSettings = { showSettings = true },
                        )
                    }
                }
            }
        }
    }
}
