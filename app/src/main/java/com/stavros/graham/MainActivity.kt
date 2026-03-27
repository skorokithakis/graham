package com.stavros.graham

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

enum class NavigationDestination(val label: String) {
    Conversation("Conversation"),
    Settings("Settings"),
    About("About"),
    ModelStatus("Model status"),
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentDestination by remember { mutableStateOf(NavigationDestination.Conversation) }
                    val conversationViewModel: ConversationViewModel = viewModel()
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val scope = rememberCoroutineScope()

                    // Back press on any non-Conversation destination returns to Conversation,
                    // mirroring the old back-from-Settings behavior.
                    if (currentDestination != NavigationDestination.Conversation) {
                        BackHandler {
                            currentDestination = NavigationDestination.Conversation
                        }
                    }

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet {
                                NavigationDestination.entries.forEach { destination ->
                                    NavigationDrawerItem(
                                        label = { Text(destination.label) },
                                        selected = currentDestination == destination,
                                        onClick = {
                                            if (destination != NavigationDestination.Conversation) {
                                                conversationViewModel.stopConversation()
                                            }
                                            currentDestination = destination
                                            scope.launch { drawerState.close() }
                                        },
                                    )
                                }
                            }
                        },
                    ) {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(
                                                imageVector = Icons.Filled.Menu,
                                                contentDescription = "Open navigation drawer",
                                            )
                                        }
                                    },
                                    title = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.ic_graham_logo),
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                            )
                                            Text("Graham")
                                        }
                                    },
                                )
                            },
                        ) { innerPadding ->
                            Surface(modifier = Modifier.padding(innerPadding)) {
                                when (currentDestination) {
                                    NavigationDestination.Conversation -> ConversationScreen(
                                        conversationViewModel = conversationViewModel,
                                    )
                                    NavigationDestination.Settings -> SettingsScreen(
                                        onSave = {
                                            conversationViewModel.reloadSettings()
                                            currentDestination = NavigationDestination.Conversation
                                        },
                                    )
                                    NavigationDestination.About -> AboutScreen()
                                    NavigationDestination.ModelStatus -> ModelStatusScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

