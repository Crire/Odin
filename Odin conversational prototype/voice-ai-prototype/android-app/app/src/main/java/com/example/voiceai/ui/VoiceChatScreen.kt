package com.example.voiceai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voiceai.conversation.ConversationState
import com.example.voiceai.ui.components.MessageBubble
import com.example.voiceai.ui.components.RecordingButton
import com.example.voiceai.ui.components.SessionStatusBar

@Composable
fun VoiceChatScreen(
    state: ConversationState,
    hasAudioPermission: Boolean,
    onRequestAudioPermission: () -> Unit,
    onPrimaryAction: () -> Unit,
    onCloseSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Voice AI Prototype",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
        Text(
            text = "Protótipo local com backend WebSocket",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280)
        )

        Spacer(modifier = Modifier.height(18.dp))

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(state.messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        SessionStatusBar(state = state)
        Spacer(modifier = Modifier.height(18.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(WindowInsets.navigationBars.asPaddingValues())
        ) {
            OutlinedButton(
                onClick = onCloseSession,
                enabled = state.isConnected
            ) {
                Text(text = "Encerrar sessão")
            }

            RecordingButton(
                label = primaryButtonLabel(state, hasAudioPermission),
                isRecording = state.isRecording,
                enabled = true,
                onClick = {
                    if (state.isConnected && !hasAudioPermission) {
                        onRequestAudioPermission()
                    } else {
                        onPrimaryAction()
                    }
                }
            )
        }
    }
}

private fun primaryButtonLabel(
    state: ConversationState,
    hasAudioPermission: Boolean
): String {
    if (!state.isConnected) return "Conectar"
    if (!hasAudioPermission) return "Permitir microfone"
    if (state.isRecording) return "Parar"
    return "Falar"
}
