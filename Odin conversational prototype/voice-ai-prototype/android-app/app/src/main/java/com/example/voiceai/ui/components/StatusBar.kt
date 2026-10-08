package com.example.voiceai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voiceai.conversation.ConversationState
import com.example.voiceai.conversation.ConversationStatus

@Composable
fun SessionStatusBar(
    state: ConversationState,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (state.status) {
        ConversationStatus.DISCONNECTED -> "Desconectado" to Color(0xFF6B7280)
        ConversationStatus.CONNECTED -> "Conectado" to Color(0xFF0F766E)
        ConversationStatus.LISTENING -> "Ouvindo..." to Color(0xFF2563EB)
        ConversationStatus.TRANSCRIBING -> "Transcrevendo..." to Color(0xFF7C3AED)
        ConversationStatus.THINKING -> "Pensando..." to Color(0xFFB45309)
        ConversationStatus.RESPONDING -> "Respondendo..." to Color(0xFF0F766E)
        ConversationStatus.ERROR -> "Erro: ${state.errorMessage ?: "verifique a sessão"}" to Color(0xFFDC2626)
    }

    Text(
        text = label,
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .background(color, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
}
