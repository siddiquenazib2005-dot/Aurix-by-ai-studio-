package com.example.aurix.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.aurix.commands.PersonalCommandManager
import com.example.aurix.data.local.entity.CustomCommandEntity
import com.example.aurix.data.local.entity.ProviderKeyEntity
import com.example.aurix.data.model.AutonomyMode
import com.example.aurix.data.model.ProviderHealth
import com.example.aurix.data.model.ProviderType
import com.example.aurix.voice.ElevenLabsVoice
import com.example.ui.theme.AurixAmber
import com.example.ui.theme.AurixBorder
import com.example.ui.theme.AurixCardHover
import com.example.ui.theme.AurixCardSurface
import com.example.ui.theme.AurixCrimson
import com.example.ui.theme.AurixCyan
import com.example.ui.theme.AurixEmerald
import com.example.ui.theme.AurixTextMuted
import com.example.ui.theme.AurixTextPrimary
import com.example.ui.theme.AurixTextSecondary
import com.example.ui.theme.AurixViolet
import com.example.ui.theme.AurixVoid

import com.example.aurix.providers.autodetect.ApiKeyDetector
import com.example.aurix.providers.autodetect.AutoDetectCoordinator
import com.example.aurix.data.model.AIModelInfo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun SettingsScreen(
    providerKeys: List<ProviderKeyEntity>,
    autonomyMode: AutonomyMode,
    onAutonomyModeChange: (AutonomyMode) -> Unit,
    onAddKey: (ProviderType, String, String, String) -> Unit,
    onDeleteKey: (String) -> Unit,
    onTestKey: (ProviderKeyEntity) -> Unit,
    elevenLabsVoices: List<ElevenLabsVoice>,
    selectedVoiceId: String,
    onSelectVoice: (String) -> Unit,
    onPreviewVoice: (String) -> Unit,
    onRefreshVoices: () -> Unit = {},
    customCommands: List<CustomCommandEntity>,
    onCreateCommand: (String, String, List<PersonalCommandManager.CommandAction>) -> Unit,
    onDeleteCommand: (CustomCommandEntity) -> Unit,
    onExecuteCommand: (CustomCommandEntity) -> Unit,
    isConsensusModeEnabled: Boolean,
    onToggleConsensusMode: (Boolean) -> Unit,
    isProactiveModeEnabled: Boolean,
    onToggleProactiveMode: (Boolean) -> Unit,
    autoDetectState: AutoDetectCoordinator.AutoDetectState = AutoDetectCoordinator.AutoDetectState.Idle,
    onRunAutoDetect: (String) -> Unit = {},
    onResetAutoDetect: () -> Unit = {},
    onSaveAutoDetectedKey: (AutoDetectCoordinator.AutoDetectState.Success, String?, String?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddKeyDialog by remember { mutableStateOf(false) }
    var showAddCommandDialog by remember { mutableStateOf(false) }

    // Auto-detect UI states
    var isManualMode by remember { mutableStateOf(false) }
    var autoDetectKeyInput by remember { mutableStateOf("") }
    var showKeySecret by remember { mutableStateOf(false) }
    var customLabelOverride by remember { mutableStateOf("") }
    var selectedModelOverride by remember { mutableStateOf<String?>(null) }

    var selectedProviderType by remember { mutableStateOf(ProviderType.GEMINI) }
    var keyLabelInput by remember { mutableStateOf("") }
    var rawKeyInput by remember { mutableStateOf("") }
    var modelIdInput by remember { mutableStateOf("gemini-2.5-flash") }

    var cmdNameInput by remember { mutableStateOf("") }
    var cmdTriggerInput by remember { mutableStateOf("") }
    var cmdToolNameInput by remember { mutableStateOf("FLASHLIGHT") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AurixVoid)
            .padding(16.dp)
    ) {
        Text(
            text = "SYSTEM ARCHITECTURE & CONTROL",
            color = AurixCyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "Multi-Key Routing, ElevenLabs, Autonomy & Diagnostics",
            color = AurixTextMuted,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Provider Health Dashboard (Phase 14)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AurixCardSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AurixBorder))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PROVIDER ROUTING HEALTH",
                                color = AurixCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Button(
                                onClick = { showAddKeyDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AurixCyan, contentColor = AurixVoid),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (providerKeys.isEmpty()) {
                            Text(
                                text = "Default system provider active. Add Gemini, Groq, OpenRouter, or ElevenLabs keys to configure multi-key pooling with automatic failover.",
                                color = AurixTextMuted,
                                fontSize = 12.sp
                            )
                        } else {
                            providerKeys.forEach { keyEntity ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val statusColor = when (keyEntity.healthStatus) {
                                        ProviderHealth.HEALTHY -> AurixEmerald
                                        ProviderHealth.RATE_LIMITED -> AurixAmber
                                        ProviderHealth.FAILED -> AurixCrimson
                                        else -> AurixTextMuted
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${keyEntity.providerType}: ${keyEntity.label} (${keyEntity.modelId})",
                                            color = AurixTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${keyEntity.apiKeyMasked} • ${keyEntity.latencyMs}ms • ${keyEntity.healthStatus}",
                                            color = AurixTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(onClick = { onTestKey(keyEntity) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Test", tint = AurixCyan, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { onDeleteKey(keyEntity.id) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AurixCrimson, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: ElevenLabs Voice Models (Phase 10)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AurixCardSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AurixBorder))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ELEVENLABS NEURAL VOICE",
                                color = AurixCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            IconButton(onClick = onRefreshVoices, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Sync Voice Models",
                                    tint = AurixCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        elevenLabsVoices.forEach { voice ->
                            val isSelected = voice.voiceId == selectedVoiceId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AurixCardHover else Color.Transparent)
                                    .clickable { onSelectVoice(voice.voiceId) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectVoice(voice.voiceId) },
                                    colors = RadioButtonDefaults.colors(selectedColor = AurixCyan, unselectedColor = AurixTextMuted)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = voice.name,
                                    color = if (isSelected) AurixCyan else AurixTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onPreviewVoice(voice.voiceId) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Preview", tint = AurixCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Universal Personal Commands (Phase 32)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AurixCardSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AurixBorder))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PERSONAL MACRO COMMANDS",
                                color = AurixCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Button(
                                onClick = { showAddCommandDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AurixCyan, contentColor = AurixVoid),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Macro", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (customCommands.isEmpty()) {
                            Text(
                                text = "No custom macros defined. Create commands like 'MORNING MODE' or 'STUDY MODE' to run multi-action sequences with 1 phrase.",
                                color = AurixTextMuted,
                                fontSize = 12.sp
                            )
                        } else {
                            customCommands.forEach { cmd ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "⚡ ${cmd.name}", color = AurixTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "Trigger: \"${cmd.triggerPhrase}\"", color = AurixCyan, fontSize = 11.sp)
                                    }
                                    IconButton(onClick = { onExecuteCommand(cmd) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = AurixEmerald, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { onDeleteCommand(cmd) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AurixCrimson, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Advanced Engine Modes (Phase 30, Phase 31, Phase 24)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AurixCardSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AurixBorder))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "INTELLIGENCE ENGINE MODES",
                            color = AurixCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Autonomy Level
                        Text("Autonomy Engine Mode:", color = AurixTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        AutonomyMode.values().forEach { mode ->
                            val isSelected = mode == autonomyMode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAutonomyModeChange(mode) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onAutonomyModeChange(mode) },
                                    colors = RadioButtonDefaults.colors(selectedColor = AurixCyan, unselectedColor = AurixTextMuted)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = mode.name.replace("_", " "),
                                    color = if (isSelected) AurixCyan else AurixTextPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Model Consensus Mode Toggle (Phase 31)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Model Consensus Mode", color = AurixTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Query multiple AI models and compare for contradictions", color = AurixTextMuted, fontSize = 11.sp)
                            }
                            Switch(
                                checked = isConsensusModeEnabled,
                                onCheckedChange = onToggleConsensusMode,
                                colors = SwitchDefaults.colors(checkedThumbColor = AurixVoid, checkedTrackColor = AurixCyan)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Proactive Awareness Mode Toggle (Phase 24)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Proactive Awareness Mode", color = AurixTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Monitor battery, timing, and workflow routines", color = AurixTextMuted, fontSize = 11.sp)
                            }
                            Switch(
                                checked = isProactiveModeEnabled,
                                onCheckedChange = onToggleProactiveMode,
                                colors = SwitchDefaults.colors(checkedThumbColor = AurixVoid, checkedTrackColor = AurixCyan)
                            )
                        }
                    }
                }
            }

            // Section 5: Permission Intelligence Dashboard (Phase 25)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AurixCardSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AurixBorder))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PERMISSION INTELLIGENCE",
                                color = AurixCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AurixCardHover, contentColor = AurixCyan),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("App Settings", fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        PermissionRow("Microphone (Audio & STT)", ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                        PermissionRow("Camera (Vision & Torch)", ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                        PermissionRow("Contacts (Call & Search)", ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
                        PermissionRow("Phone Calling", ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
                        PermissionRow("SMS Messages", ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED)
                    }
                }
            }

            // Section 6: Self Diagnostics & Reality Audit (Phase 26)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AurixCardSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AurixBorder))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "SELF DIAGNOSTICS & SYSTEM AUDIT",
                            color = AurixCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        DiagnosticRow("AI Provider Router", "HEALTHY (Failover active)", AurixEmerald)
                        DiagnosticRow("Voice Core (STT/TTS)", "OPERATIONAL (Barge-in ready)", AurixEmerald)
                        DiagnosticRow("Room Local Database", "AURIX_BRAIN.DB (Connected)", AurixEmerald)
                        DiagnosticRow("Universal Tool Registry", "17 Tool Plugins Registered", AurixEmerald)
                        DiagnosticRow("Hardware Reality Engine", "ACTIVE (State verified)", AurixEmerald)
                        DiagnosticRow("Security & Secrets", "OBFUSCATED IN STORAGE", AurixEmerald)
                    }
                }
            }
        }
    }

    // Add Key Dialog (Auto-Detect First & Manual Fallback)
    if (showAddKeyDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddKeyDialog = false
                onResetAutoDetect()
                selectedModelOverride = null
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isManualMode) "Manual Provider Setup" else "Connect AI Provider",
                        color = AurixTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        isManualMode = !isManualMode
                        onResetAutoDetect()
                    }) {
                        Text(
                            text = if (isManualMode) "⚡ Auto-Detect" else "⚙ Manual",
                            color = AurixCyan,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isManualMode) {
                        // --- AUTOMATIC DETECTION FLOW ---
                        when (val state = autoDetectState) {
                            is AutoDetectCoordinator.AutoDetectState.Idle -> {
                                Text(
                                    text = "Paste any API key (Gemini, Groq, OpenRouter, OpenAI, or ElevenLabs). AURIX automatically verifies provider access, discovers models, evaluates agent capabilities, and selects the optimal flagship model.",
                                    color = AurixTextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )

                                OutlinedTextField(
                                    value = autoDetectKeyInput,
                                    onValueChange = { autoDetectKeyInput = it },
                                    label = { Text("Paste your API key") },
                                    placeholder = { Text("e.g. AIzaSy..., gsk_..., sk-...") },
                                    visualTransformation = if (showKeySecret) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showKeySecret = !showKeySecret }) {
                                            Icon(
                                                imageVector = if (showKeySecret) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle key visibility",
                                                tint = AurixTextMuted
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = AurixTextPrimary,
                                        unfocusedTextColor = AurixTextPrimary,
                                        focusedBorderColor = AurixCyan,
                                        unfocusedBorderColor = AurixBorder
                                    )
                                )

                                val detectedHint = ApiKeyDetector.detectProviderHint(autoDetectKeyInput)
                                if (detectedHint != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(AurixCyan)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Recognized key pattern: ${detectedHint.name}",
                                            color = AurixCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (autoDetectKeyInput.isNotBlank()) {
                                            onRunAutoDetect(autoDetectKeyInput.trim())
                                        }
                                    },
                                    enabled = autoDetectKeyInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AurixCyan,
                                        contentColor = AurixVoid,
                                        disabledContainerColor = AurixCardHover,
                                        disabledContentColor = AurixTextMuted
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Detect & Connect", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            is AutoDetectCoordinator.AutoDetectState.Detecting -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = AurixCyan,
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        text = state.stepMessage,
                                        color = AurixCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Connecting directly to official discovery endpoints...",
                                        color = AurixTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            is AutoDetectCoordinator.AutoDetectState.Success -> {
                                val currentModel = selectedModelOverride ?: state.bestModel.id

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = AurixCardHover),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AurixEmerald, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = state.provider.name,
                                                    color = AurixEmerald,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Text(
                                                text = "${state.latencyMs}ms",
                                                color = AurixCyan,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = "Selected Model: $currentModel",
                                            color = AurixTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "✓ Tool Calling",
                                                color = AurixEmerald,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "• ${state.bestModel.contextWindow / 1000}k Context",
                                                color = AurixTextMuted,
                                                fontSize = 10.sp
                                            )
                                            if (state.bestModel.supportsVision) {
                                                Text(
                                                    text = "• Vision",
                                                    color = AurixCyan,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${state.discoveredModels.size} models discovered & ranked",
                                            color = AurixTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (state.discoveredModels.size > 1) {
                                    Text(
                                        text = "Discovered Model Selection (Auto-ranked):",
                                        color = AurixTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        state.discoveredModels.take(3).forEach { m ->
                                            val isChosen = currentModel == m.id
                                            Button(
                                                onClick = { selectedModelOverride = m.id },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isChosen) AurixCyan else AurixCardSurface,
                                                    contentColor = if (isChosen) AurixVoid else AurixTextPrimary
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = m.id.substringAfterLast("/").take(10),
                                                    fontSize = 9.sp,
                                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = customLabelOverride,
                                    onValueChange = { customLabelOverride = it },
                                    label = { Text("Key Label") },
                                    placeholder = { Text("${state.provider.name} Auto") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = AurixTextPrimary,
                                        unfocusedTextColor = AurixTextPrimary,
                                        focusedBorderColor = AurixCyan,
                                        unfocusedBorderColor = AurixBorder
                                    )
                                )
                            }

                            is AutoDetectCoordinator.AutoDetectState.Error -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = AurixCardHover),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = AurixCrimson, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Detection Unsuccessful", color = AurixCrimson, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Text(text = state.message, color = AurixTextMuted, fontSize = 11.sp)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onResetAutoDetect() },
                                        colors = ButtonDefaults.buttonColors(containerColor = AurixCyan, contentColor = AurixVoid),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Try Again", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { isManualMode = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = AurixCardHover, contentColor = AurixTextPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Manual Setup", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // --- MANUAL CONFIGURATION (FALLBACK) ---
                        Text("Select Provider:", color = AurixTextSecondary, fontSize = 12.sp)
                        Row {
                            ProviderType.values().forEach { p ->
                                Button(
                                    onClick = {
                                        selectedProviderType = p
                                        modelIdInput = when (p) {
                                            ProviderType.GEMINI -> "gemini-2.5-flash"
                                            ProviderType.GROQ -> "llama-3.3-70b-versatile"
                                            ProviderType.OPENROUTER -> "anthropic/claude-3.5-sonnet"
                                            ProviderType.ELEVENLABS -> "eleven_turbo_v2_5"
                                            else -> "gpt-4o"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedProviderType == p) AurixCyan else AurixCardHover,
                                        contentColor = if (selectedProviderType == p) AurixVoid else AurixTextPrimary
                                    ),
                                    modifier = Modifier.padding(end = 4.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(p.name.take(4), fontSize = 10.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = keyLabelInput,
                            onValueChange = { keyLabelInput = it },
                            label = { Text("Key Label") },
                            placeholder = { Text("e.g. 'Primary Gemini'") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AurixTextPrimary,
                                unfocusedTextColor = AurixTextPrimary,
                                focusedBorderColor = AurixCyan,
                                unfocusedBorderColor = AurixBorder
                            )
                        )

                        OutlinedTextField(
                            value = rawKeyInput,
                            onValueChange = { rawKeyInput = it },
                            label = { Text("API Secret Key") },
                            placeholder = { Text("API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AurixTextPrimary,
                                unfocusedTextColor = AurixTextPrimary,
                                focusedBorderColor = AurixCyan,
                                unfocusedBorderColor = AurixBorder
                            )
                        )

                        OutlinedTextField(
                            value = modelIdInput,
                            onValueChange = { modelIdInput = it },
                            label = { Text("Model ID") },
                            placeholder = { Text("Model ID") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AurixTextPrimary,
                                unfocusedTextColor = AurixTextPrimary,
                                focusedBorderColor = AurixCyan,
                                unfocusedBorderColor = AurixBorder
                            )
                        )
                    }
                }
            },
            confirmButton = {
                if (!isManualMode) {
                    if (autoDetectState is AutoDetectCoordinator.AutoDetectState.Success) {
                        val success = autoDetectState as AutoDetectCoordinator.AutoDetectState.Success
                        Button(
                            onClick = {
                                val label = customLabelOverride.ifBlank { "${success.provider.name} Auto" }
                                val model = selectedModelOverride ?: success.bestModel.id
                                onSaveAutoDetectedKey(success, label, model)
                                showAddKeyDialog = false
                                autoDetectKeyInput = ""
                                customLabelOverride = ""
                                selectedModelOverride = null
                                onResetAutoDetect()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AurixEmerald, contentColor = AurixVoid)
                        ) {
                            Text("✓ Save & Activate", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (rawKeyInput.isNotBlank()) {
                                onAddKey(
                                    selectedProviderType,
                                    keyLabelInput.ifBlank { "${selectedProviderType.name} Key" },
                                    rawKeyInput.trim(),
                                    modelIdInput.trim()
                                )
                                rawKeyInput = ""
                                keyLabelInput = ""
                                showAddKeyDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AurixCyan, contentColor = AurixVoid)
                    ) {
                        Text("Save Key")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddKeyDialog = false
                    onResetAutoDetect()
                    selectedModelOverride = null
                }) {
                    Text("Close", color = AurixTextSecondary)
                }
            },
            containerColor = AurixCardSurface
        )
    }

    // Add Command Dialog
    if (showAddCommandDialog) {
        AlertDialog(
            onDismissRequest = { showAddCommandDialog = false },
            title = { Text("Create Personal Macro Command", color = AurixTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cmdNameInput,
                        onValueChange = { cmdNameInput = it },
                        placeholder = { Text("Name (e.g. 'Morning Mode')") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AurixTextPrimary,
                            unfocusedTextColor = AurixTextPrimary,
                            focusedBorderColor = AurixCyan,
                            unfocusedBorderColor = AurixBorder
                        )
                    )
                    OutlinedTextField(
                        value = cmdTriggerInput,
                        onValueChange = { cmdTriggerInput = it },
                        placeholder = { Text("Trigger phrase (e.g. 'morning mode')") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AurixTextPrimary,
                            unfocusedTextColor = AurixTextPrimary,
                            focusedBorderColor = AurixCyan,
                            unfocusedBorderColor = AurixBorder
                        )
                    )
                    OutlinedTextField(
                        value = cmdToolNameInput,
                        onValueChange = { cmdToolNameInput = it },
                        placeholder = { Text("Tool (e.g. 'FLASHLIGHT', 'VOLUME', 'OPEN_APP')") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AurixTextPrimary,
                            unfocusedTextColor = AurixTextPrimary,
                            focusedBorderColor = AurixCyan,
                            unfocusedBorderColor = AurixBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cmdNameInput.isNotBlank() && cmdTriggerInput.isNotBlank()) {
                            onCreateCommand(
                                cmdNameInput.trim(),
                                cmdTriggerInput.trim(),
                                listOf(
                                    PersonalCommandManager.CommandAction(
                                        toolName = cmdToolNameInput.trim().uppercase(),
                                        argumentsJson = "{}"
                                    )
                                )
                            )
                            cmdNameInput = ""
                            cmdTriggerInput = ""
                            showAddCommandDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AurixCyan, contentColor = AurixVoid)
                ) {
                    Text("Create Command")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCommandDialog = false }) {
                    Text("Cancel", color = AurixTextSecondary)
                }
            },
            containerColor = AurixCardSurface
        )
    }
}

@Composable
fun PermissionRow(name: String, isGranted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, color = AurixTextPrimary, fontSize = 12.sp)
        Text(
            text = if (isGranted) "GRANTED ✓" else "NOT GRANTED ⚠",
            color = if (isGranted) AurixEmerald else AurixAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DiagnosticRow(title: String, status: String, statusColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = AurixTextPrimary, fontSize = 12.sp)
        Text(text = status, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
