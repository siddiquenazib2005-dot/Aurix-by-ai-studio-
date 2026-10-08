package com.example.aurix.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aurix.ui.components.SidebarDrawer
import com.example.ui.theme.AurixBorder
import com.example.ui.theme.AurixCardSurface
import com.example.ui.theme.AurixCyan
import com.example.ui.theme.AurixDarkSurface
import com.example.ui.theme.AurixTextMuted
import com.example.ui.theme.AurixTextPrimary
import com.example.ui.theme.AurixVoid
import com.example.aurix.ui.viewmodel.AurixTab
import com.example.aurix.ui.viewmodel.AurixViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: AurixViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val currentConvId by viewModel.currentConversationId.collectAsState()
    val currentMessages by viewModel.currentMessages.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val pendingAction by viewModel.pendingActionSummary.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val rmsLevel by viewModel.rmsLevel.collectAsState()
    val liveTranscription by viewModel.liveTranscription.collectAsState()
    val lastVoiceResponse by viewModel.lastVoiceResponse.collectAsState()
    val missions by viewModel.missions.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val providerKeys by viewModel.providerKeys.collectAsState()
    val autonomyMode by viewModel.autonomyMode.collectAsState()
    val elevenLabsVoices by viewModel.elevenLabsVoices.collectAsState()
    val selectedVoiceId by viewModel.selectedVoiceId.collectAsState()
    val customCommands by viewModel.customCommands.collectAsState()
    val isConsensusMode by viewModel.isConsensusModeEnabled.collectAsState()
    val isProactiveMode by viewModel.isProactiveModeEnabled.collectAsState()
    val selectedTelemetryForWhy by viewModel.selectedTelemetryForWhy.collectAsState()
    val autoDetectState by viewModel.autoDetectState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentConvTitle = conversations.firstOrNull { it.id == currentConvId }?.title ?: "AURIX Agent"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = AurixDarkSurface,
                modifier = Modifier.padding(end = 64.dp)
            ) {
                SidebarDrawer(
                    conversations = conversations,
                    currentConversationId = currentConvId,
                    onSelectConversation = { viewModel.selectConversation(it) },
                    onNewChat = { viewModel.createNewConversation() },
                    onRenameConversation = { id, title -> viewModel.renameConversation(id, title) },
                    onDeleteConversation = { viewModel.deleteConversation(it) },
                    onCloseDrawer = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = AurixDarkSurface,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = currentTab == AurixTab.CHAT,
                        onClick = { viewModel.selectTab(AurixTab.CHAT) },
                        icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                        label = { Text("Chat", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AurixCyan,
                            selectedTextColor = AurixCyan,
                            unselectedIconColor = AurixTextMuted,
                            unselectedTextColor = AurixTextMuted,
                            indicatorColor = AurixCardSurface
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == AurixTab.MISSIONS,
                        onClick = { viewModel.selectTab(AurixTab.MISSIONS) },
                        icon = { Icon(Icons.Default.Assignment, contentDescription = "Missions") },
                        label = { Text("Missions", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AurixCyan,
                            selectedTextColor = AurixCyan,
                            unselectedIconColor = AurixTextMuted,
                            unselectedTextColor = AurixTextMuted,
                            indicatorColor = AurixCardSurface
                        )
                    )

                    // Central glowing Mic Orb in bottom bar
                    NavigationBarItem(
                        selected = currentTab == AurixTab.VOICE,
                        onClick = {
                            viewModel.selectTab(AurixTab.VOICE)
                            viewModel.startListening()
                        },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AurixCyan)
                                    .border(2.dp, AurixVoid, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Live Voice Orb",
                                    tint = AurixVoid,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = { Text("Voice Orb", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AurixCyan) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AurixCyan,
                            selectedTextColor = AurixCyan,
                            unselectedIconColor = AurixCyan,
                            unselectedTextColor = AurixCyan,
                            indicatorColor = Color.Transparent
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == AurixTab.MEMORY,
                        onClick = { viewModel.selectTab(AurixTab.MEMORY) },
                        icon = { Icon(Icons.Default.Psychology, contentDescription = "Memory") },
                        label = { Text("Memory", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AurixCyan,
                            selectedTextColor = AurixCyan,
                            unselectedIconColor = AurixTextMuted,
                            unselectedTextColor = AurixTextMuted,
                            indicatorColor = AurixCardSurface
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == AurixTab.SETTINGS,
                        onClick = { viewModel.selectTab(AurixTab.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AurixCyan,
                            selectedTextColor = AurixCyan,
                            unselectedIconColor = AurixTextMuted,
                            unselectedTextColor = AurixTextMuted,
                            indicatorColor = AurixCardSurface
                        )
                    )
                }
            }
        ) { innerPadding ->
            Crossfade(
                targetState = currentTab,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                label = "TabCrossfade"
            ) { tab ->
                when (tab) {
                    AurixTab.CHAT -> ChatScreen(
                        conversationTitle = currentConvTitle,
                        messages = currentMessages,
                        isThinking = isThinking,
                        onSendMessage = { viewModel.handleUserMessage(it) },
                        onOpenSidebar = { scope.launch { drawerState.open() } },
                        onOpenVoice = {
                            viewModel.selectTab(AurixTab.VOICE)
                            viewModel.startListening()
                        },
                        pendingActionSummary = pendingAction,
                        onApproveAction = { viewModel.approveAction(it) }
                    )

                    AurixTab.VOICE -> VoiceScreen(
                        voiceState = voiceState,
                        rmsLevel = rmsLevel,
                        liveTranscription = liveTranscription,
                        lastAgentResponse = lastVoiceResponse,
                        onStartListening = { viewModel.startListening() },
                        onStopListening = { viewModel.stopListening() },
                        onStopSpeaking = { viewModel.stopSpeaking() }
                    )

                    AurixTab.MISSIONS -> MissionsScreen(
                        missions = missions,
                        missionEngine = viewModel.missionEngine,
                        onResumeMission = { viewModel.resumeMission(it) }
                    )

                    AurixTab.MEMORY -> MemoryScreen(
                        memories = memories,
                        onAddMemory = { content, category -> viewModel.addMemory(content, category) },
                        onDeleteMemory = { viewModel.deleteMemory(it) }
                    )

                    AurixTab.SETTINGS -> SettingsScreen(
                        providerKeys = providerKeys,
                        autonomyMode = autonomyMode,
                        onAutonomyModeChange = { viewModel.setAutonomyMode(it) },
                        onAddKey = { type, label, key, model -> viewModel.addProviderKey(type, label, key, model) },
                        onDeleteKey = { viewModel.deleteProviderKey(it) },
                        onTestKey = { viewModel.testProviderKey(it) },
                        elevenLabsVoices = elevenLabsVoices,
                        selectedVoiceId = selectedVoiceId,
                        onSelectVoice = { viewModel.selectVoice(it) },
                        onPreviewVoice = { viewModel.previewVoice(it) },
                        onRefreshVoices = { viewModel.refreshElevenLabsVoices() },
                        customCommands = customCommands,
                        onCreateCommand = { name, trigger, actions -> viewModel.createCustomCommand(name, trigger, actions) },
                        onDeleteCommand = { viewModel.deleteCustomCommand(it) },
                        onExecuteCommand = { viewModel.executeCustomCommand(it) },
                        isConsensusModeEnabled = isConsensusMode,
                        onToggleConsensusMode = { viewModel.toggleConsensusMode(it) },
                        isProactiveModeEnabled = isProactiveMode,
                        onToggleProactiveMode = { viewModel.toggleProactiveMode(it) },
                        autoDetectState = autoDetectState,
                        onRunAutoDetect = { viewModel.runAutoDetect(it) },
                        onResetAutoDetect = { viewModel.resetAutoDetect() },
                        onSaveAutoDetectedKey = { success, label, model -> viewModel.saveAutoDetectedKey(success, label, model) }
                    )
                }
            }

            // Why Engine Telemetry Dialog (Phase 29)
            selectedTelemetryForWhy?.let { telem ->
                com.example.aurix.ui.components.WhyEngineTelemetryDialog(
                    userIntent = "Executed tool ${telem.toolName}",
                    interpretation = "Parameters: ${telem.parametersJson}",
                    planSummary = "Dispatched via Reality Engine",
                    toolInvoked = telem.toolName,
                    resultReport = telem.message,
                    realityVerification = telem.verificationReport,
                    onDismiss = { viewModel.selectTelemetryForWhy(null) }
                )
            }
        }
    }
}
