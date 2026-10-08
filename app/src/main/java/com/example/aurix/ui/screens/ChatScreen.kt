package com.example.aurix.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aurix.data.local.entity.MessageEntity
import com.example.ui.theme.AurixAmber
import com.example.ui.theme.AurixBorder
import com.example.ui.theme.AurixCardHover
import com.example.ui.theme.AurixCardSurface
import com.example.ui.theme.AurixCyan
import com.example.ui.theme.AurixDarkSurface
import com.example.ui.theme.AurixElectricBlue
import com.example.ui.theme.AurixEmerald
import com.example.ui.theme.AurixTextMuted
import com.example.ui.theme.AurixTextPrimary
import com.example.ui.theme.AurixTextSecondary
import com.example.ui.theme.AurixViolet
import com.example.ui.theme.AurixVoid

@Composable
fun ChatScreen(
    conversationTitle: String,
    messages: List<MessageEntity>,
    isThinking: Boolean,
    onSendMessage: (String) -> Unit,
    onOpenSidebar: () -> Unit,
    onOpenVoice: () -> Unit,
    pendingActionSummary: String?,
    onApproveAction: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AurixVoid)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AurixDarkSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenSidebar) {
                Icon(Icons.Default.Menu, contentDescription = "Sidebar", tint = AurixCyan)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversationTitle,
                    color = AurixTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "Neural Agent • Online",
                    color = AurixEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            // Quick Voice Mode toggle icon
            IconButton(
                onClick = onOpenVoice,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AurixCardSurface)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice Mode", tint = AurixCyan)
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    EmptyChatIntro(onSelectSuggestion = { suggestion ->
                        textInput = suggestion
                    })
                }
            }

            items(messages, key = { it.id }) { message ->
                MessageBubble(message)
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AurixCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AURIX is planning and verifying...",
                            color = AurixTextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Sensitive Action Preview Approval Dialog (Phase 30 Dry Run)
        pendingActionSummary?.let { summary ->
            AlertDialog(
                onDismissRequest = { onApproveAction(false) },
                title = { Text("Approve Sensitive Action", color = AurixTextPrimary) },
                text = {
                    Text(
                        text = "AURIX is requesting permission to execute:\n\n• $summary\n\nDo you want to proceed?",
                        color = AurixTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { onApproveAction(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = AurixCyan, contentColor = AurixVoid)
                    ) {
                        Text("Approve & Run")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onApproveAction(false) }) {
                        Text("Cancel", color = AurixAmber)
                    }
                },
                containerColor = AurixCardSurface
            )
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AurixDarkSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        "Command or chat in English / Hinglish...",
                        color = AurixTextMuted,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AurixCardSurface,
                    unfocusedContainerColor = AurixCardSurface,
                    focusedBorderColor = AurixCyan,
                    unfocusedBorderColor = AurixBorder,
                    focusedTextColor = AurixTextPrimary,
                    unfocusedTextColor = AurixTextPrimary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendMessage(textInput.trim())
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (textInput.isNotBlank()) AurixCyan else AurixCardHover)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (textInput.isNotBlank()) AurixVoid else AurixTextMuted
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: MessageEntity) {
    val isUser = message.sender.equals("user", ignoreCase = true)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AurixCardSurface)
                    .border(1.dp, AurixCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = AurixCyan, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) AurixCardHover else AurixCardSurface
            ),
            border = if (!isUser) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AurixBorder)) else null,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    color = AurixTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                // Tool execution indicator or reality badge
                if (message.content.contains("Verified", ignoreCase = true) || message.content.contains("✓")) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = AurixEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reality Engine Confirmed",
                            color = AurixEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatIntro(onSelectSuggestion: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AURIX PERSONAL AGENT",
            color = AurixCyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Voice-First Multimodal Action Engine",
            color = AurixTextMuted,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        val suggestions = listOf(
            "Flashlight on kar do",
            "Mummy ko message kar ki main late hoon aur 8 baje reminder laga dena",
            "Set volume to 80%",
            "Spotify open karo",
            "Battery status check karo"
        )

        Text(
            text = "SUGGESTED COMMANDS (ENGLISH & HINGLISH)",
            color = AurixTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        suggestions.forEach { sug ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelectSuggestion(sug) },
                colors = CardDefaults.cardColors(containerColor = AurixCardSurface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ $sug",
                        color = AurixTextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = AurixCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
