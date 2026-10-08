package com.example.voiceai

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voiceai.conversation.ConversationViewModel
import com.example.voiceai.permissions.rememberAudioPermissionState
import com.example.voiceai.ui.VoiceChatScreen

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<ConversationViewModel> {
        ConversationViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val audioPermission = rememberAudioPermissionState()

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF2563EB),
                    secondary = Color(0xFF0F766E),
                    background = Color(0xFFF7F8FA),
                    surface = Color.White,
                    onPrimary = Color.White,
                    onSecondary = Color.White
                )
            ) {
                Surface {
                    VoiceChatScreen(
                        state = state,
                        hasAudioPermission = audioPermission.isGranted,
                        onRequestAudioPermission = audioPermission.request,
                        onPrimaryAction = { viewModel.handlePrimaryAction(audioPermission.isGranted) },
                        onCloseSession = viewModel::closeSession
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        viewModel.shutdown()
        super.onDestroy()
    }
}

private class ConversationViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ConversationViewModel(application) as T
    }
}
