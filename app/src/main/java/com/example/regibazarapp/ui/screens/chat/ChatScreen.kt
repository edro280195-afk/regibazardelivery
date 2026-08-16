package com.example.regibazarapp.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.regibazarapp.data.models.ChatMessageDto
import com.example.regibazarapp.ui.theme.RegiBazar2026Palette
import com.example.regibazarapp.ui.theme.RegiBazar2026Tokens
import com.example.regibazarapp.ui.theme.regiBazar2026Palette
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    driverToken: String,
    deliveryId: Int,
    clientName: String = "",
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = regiBazar2026Palette()
    val listState = rememberLazyListState()
    val title = remember(deliveryId, clientName) {
        when {
            deliveryId == 0 -> "Chat con admin"
            clientName.isNotBlank() -> clientName
            else -> "Chat con clienta"
        }
    }

    LaunchedEffect(driverToken, deliveryId) {
        viewModel.initialize(driverToken, deliveryId)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            ChatHeader(
                title = title,
                isAdminChat = deliveryId == 0,
                isConnected = uiState.isConnected,
                palette = palette,
                onBack = onBack
            )
        },
        bottomBar = {
            ChatComposer(
                inputText = uiState.inputText,
                isSending = uiState.isSending,
                palette = palette,
                onInputChange = viewModel::onInputChange,
                onSend = viewModel::sendMessage
            )
        },
        containerColor = palette.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(palette.background)
        ) {
            when {
                uiState.isLoading -> ChatLoadingState(palette)
                uiState.messages.isEmpty() -> ChatEmptyState(
                    isAdminChat = deliveryId == 0,
                    palette = palette
                )
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = RegiBazar2026Tokens.ScreenPadding,
                            end = RegiBazar2026Tokens.ScreenPadding,
                            top = 12.dp,
                            bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        item(key = "chat-notice") {
                            ChatNotice(isAdminChat = deliveryId == 0, palette = palette)
                        }
                        items(uiState.messages, key = { it.id }) { message ->
                            MessageBubble(message = message, palette = palette)
                        }
                    }
                }
            }

            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = palette.error,
                    contentColor = Color(0xFFF8FAFF)
                ) {
                    Text(error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ChatHeader(
    title: String,
    isAdminChat: Boolean,
    isConnected: Boolean,
    palette: RegiBazar2026Palette,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.background)
            .statusBarsPadding()
            .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(RegiBazar2026Tokens.TouchTarget)
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Regresar",
                tint = palette.textPrimary
            )
        }
        Surface(
            shape = RoundedCornerShape(13.dp),
            color = if (isAdminChat) palette.accentSoft else palette.successSoft,
            contentColor = if (isAdminChat) palette.accent else palette.success,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isAdminChat) Icons.Rounded.SupportAgent else Icons.Rounded.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(if (isConnected) palette.success else palette.error, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isConnected) "En vivo" else "Sin conexión",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isConnected) palette.success else palette.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ChatComposer(
    inputText: String,
    isSending: Boolean,
    palette: RegiBazar2026Palette,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = palette.surface,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un mensaje") },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.surface,
                    unfocusedContainerColor = palette.surface,
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = palette.divider,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    focusedPlaceholderColor = palette.textSecondary,
                    unfocusedPlaceholderColor = palette.textSecondary,
                    cursorColor = palette.accent
                )
            )
            Surface(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isSending,
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (inputText.isNotBlank()) palette.accent else palette.surfaceMuted,
                contentColor = if (inputText.isNotBlank()) Color(0xFFF8FAFF) else palette.textSecondary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFFF8FAFF),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Enviar mensaje",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatNotice(isAdminChat: Boolean, palette: RegiBazar2026Palette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.accentSoft, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.SupportAgent,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            if (isAdminChat) {
                "Canal de soporte de la ruta. Usa mensajes cortos para resolver rápido."
            } else {
                "Conversación ligada a esta parada. Mantén dirección, pago y cambios aquí."
            },
            style = MaterialTheme.typography.bodySmall,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ChatLoadingState(palette: RegiBazar2026Palette) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .align(if (index % 2 == 0) Alignment.Start else Alignment.End)
                    .width(if (index % 2 == 0) 240.dp else 190.dp)
                    .height(if (index == 0) 56.dp else 74.dp)
                    .background(palette.surfaceMuted, RoundedCornerShape(18.dp))
            )
        }
    }
}

@Composable
private fun ChatEmptyState(isAdminChat: Boolean, palette: RegiBazar2026Palette) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = palette.accentSoft,
            contentColor = palette.accent,
            modifier = Modifier.size(74.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isAdminChat) Icons.Rounded.SupportAgent else Icons.Rounded.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Sin mensajes todavía",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(5.dp))
        Text(
            if (isAdminChat) {
                "Escribe al admin si necesitas apoyo con la ruta."
            } else {
                "Este chat queda guardado con la parada."
            },
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageDto,
    palette: RegiBazar2026Palette
) {
    val isDriver = message.sender.equals("Driver", ignoreCase = true)
    val timeText = remember(message.timestamp) { formatChatTime(message.timestamp) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isDriver) Alignment.End else Alignment.Start
    ) {
        if (!isDriver) {
            Text(
                text = message.sender,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 3.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isDriver) 18.dp else 6.dp,
                bottomEnd = if (isDriver) 6.dp else 18.dp
            ),
            color = if (isDriver) palette.accent else palette.surface,
            contentColor = if (isDriver) Color(0xFFF8FAFF) else palette.textPrimary,
            shadowElevation = if (isDriver) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 292.dp)
        ) {
            Column(Modifier.padding(horizontal = 13.dp, vertical = 9.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDriver) Color(0xFFF8FAFF) else palette.textPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDriver) Color(0xCCF8FAFF) else palette.textSecondary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

private fun formatChatTime(value: String): String {
    if (value.isBlank()) return ""
    val instant = try {
        OffsetDateTime.parse(value).toInstant()
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(value.take(19)).atZone(ZoneId.systemDefault()).toInstant()
        } catch (_: Exception) {
            null
        }
    }
    return instant
        ?.atZone(ZoneId.systemDefault())
        ?.format(DateTimeFormatter.ofPattern("H:mm"))
        ?: value.take(5)
}
