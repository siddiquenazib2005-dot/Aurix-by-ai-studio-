package com.example.aurix.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aurix.brain.AgentBrain
import com.example.aurix.data.local.AurixDatabase
import com.example.aurix.data.local.entity.ConversationEntity
import com.example.aurix.data.local.entity.MemoryEntity
import com.example.aurix.data.local.entity.MessageEntity
import com.example.aurix.data.local.entity.MissionEntity
import com.example.aurix.data.local.entity.ProviderKeyEntity
import com.example.aurix.data.model.AutonomyMode
import com.example.aurix.data.model.ChatMessageItem
import com.example.aurix.data.model.MemoryCategory
import com.example.aurix.data.model.ProviderType
import com.example.aurix.memory.SemanticMemoryEngine
import com.example.aurix.mission.MissionEngine
import com.example.aurix.providers.ProviderRouter
import com.example.aurix.security.SecureStorage
import com.example.aurix.tools.RealityEngine
import com.example.aurix.tools.ToolRegistry
import com.example.aurix.voice.ElevenLabsClient
import com.example.aurix.voice.ElevenLabsVoice
import com.example.aurix.voice.VoiceManager
import com.example.aurix.voice.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AurixTab {
    CHAT,
    VOICE,
    MISSIONS,
    MEMORY,
    SETTINGS
}

class AurixViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AurixDatabase.getInstance(application)
    val toolRegistry = ToolRegistry(application)
    val realityEngine = RealityEngine(db.telemetryDao())
    val missionEngine = MissionEngine(db.missionDao(), toolRegistry, realityEngine)
    val memoryEngine = SemanticMemoryEngine(db.memoryDao(), db.messageDao())
    val providerRouter = ProviderRouter(db.providerKeyDao())
    val elevenLabsClient = ElevenLabsClient(application)
    val voiceManager = VoiceManager(application, elevenLabsClient, viewModelScope)
    val personalCommandManager = com.example.aurix.commands.PersonalCommandManager(db.customCommandDao(), toolRegistry, realityEngine)
    val consensusEngine = com.example.aurix.brain.ConsensusEngine(providerRouter)
    val proactiveAwarenessEngine = com.example.aurix.intelligence.ProactiveAwarenessEngine(application, db.telemetryDao(), memoryEngine)

    val brain = AgentBrain(
        providerRouter = providerRouter,
        toolRegistry = toolRegistry,
        realityEngine = realityEngine,
        missionEngine = missionEngine,
        memoryEngine = memoryEngine
    )

    // UI Navigation State
    private val _currentTab = MutableStateFlow(AurixTab.CHAT)
    val currentTab: StateFlow<AurixTab> = _currentTab.asStateFlow()

    // Conversations & Messages
    val conversations: StateFlow<List<ConversationEntity>> = db.conversationDao().getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentConversationId = MutableStateFlow("conv_default")
    val currentConversationId: StateFlow<String> = _currentConversationId.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val currentMessages: StateFlow<List<MessageEntity>> = _currentMessages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _pendingActionSummary = MutableStateFlow<String?>(null)
    val pendingActionSummary: StateFlow<String?> = _pendingActionSummary.asStateFlow()

    // Autonomy Mode
    private val _autonomyMode = MutableStateFlow(AutonomyMode.SAFE_AUTO)
    val autonomyMode: StateFlow<AutonomyMode> = _autonomyMode.asStateFlow()

    // Voice State
    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState
    val rmsLevel: StateFlow<Float> = voiceManager.rmsLevel
    val liveTranscription: StateFlow<String> = voiceManager.liveTranscription

    private val _lastVoiceResponse = MutableStateFlow("")
    val lastVoiceResponse: StateFlow<String> = _lastVoiceResponse.asStateFlow()

    private val _elevenLabsVoices = MutableStateFlow<List<ElevenLabsVoice>>(
        listOf(
            ElevenLabsVoice("21m00Tcm4TlvDq8ikWAM", "Rachel (Calm & Clear)"),
            ElevenLabsVoice("AZnzlk1XvdvUeBnXmlld", "Domi (Confident & Strong)"),
            ElevenLabsVoice("EXAVITQu4vr4xnSDxMaL", "Bella (Warm & Expressive)"),
            ElevenLabsVoice("ErXwobaYiN019PkySvjV", "Antoni (Futuristic & Crisp)"),
            ElevenLabsVoice("VR6AewLTigWG4xSOukaG", "Arnold (Deep & Direct)")
        )
    )
    val elevenLabsVoices: StateFlow<List<ElevenLabsVoice>> = _elevenLabsVoices.asStateFlow()
    private val _isRefreshingVoices = MutableStateFlow(false)
    val isRefreshingVoices: StateFlow<Boolean> = _isRefreshingVoices.asStateFlow()

    private val _selectedVoiceId = MutableStateFlow("21m00Tcm4TlvDq8ikWAM")
    val selectedVoiceId: StateFlow<String> = _selectedVoiceId.asStateFlow()

    // Data from Database
    val missions: StateFlow<List<MissionEntity>> = missionEngine.allMissionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = memoryEngine.allMemoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val providerKeys: StateFlow<List<ProviderKeyEntity>> = providerRouter.allKeysFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCommands: StateFlow<List<com.example.aurix.data.local.entity.CustomCommandEntity>> = personalCommandManager.allCommandsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val telemetryList: StateFlow<List<com.example.aurix.data.local.entity.DeviceTelemetryEntity>> = db.telemetryDao().getRecentTelemetry()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isConsensusModeEnabled = MutableStateFlow(false)
    val isConsensusModeEnabled: StateFlow<Boolean> = _isConsensusModeEnabled.asStateFlow()

    private val _isProactiveModeEnabled = MutableStateFlow(true)
    val isProactiveModeEnabled: StateFlow<Boolean> = _isProactiveModeEnabled.asStateFlow()

    private val _selectedTelemetryForWhy = MutableStateFlow<com.example.aurix.data.local.entity.DeviceTelemetryEntity?>(null)
    val selectedTelemetryForWhy: StateFlow<com.example.aurix.data.local.entity.DeviceTelemetryEntity?> = _selectedTelemetryForWhy.asStateFlow()

    val autoDetectCoordinator = com.example.aurix.providers.autodetect.AutoDetectCoordinator()
    private val _autoDetectState = MutableStateFlow<com.example.aurix.providers.autodetect.AutoDetectCoordinator.AutoDetectState>(com.example.aurix.providers.autodetect.AutoDetectCoordinator.AutoDetectState.Idle)
    val autoDetectState: StateFlow<com.example.aurix.providers.autodetect.AutoDetectCoordinator.AutoDetectState> = _autoDetectState.asStateFlow()

    fun runAutoDetect(apiKey: String) {
        viewModelScope.launch {
            _autoDetectState.value = com.example.aurix.providers.autodetect.AutoDetectCoordinator.AutoDetectState.Detecting("Analyzing API key format...")
            val result = autoDetectCoordinator.runDetectionFlow(apiKey) { progress ->
                _autoDetectState.value = com.example.aurix.providers.autodetect.AutoDetectCoordinator.AutoDetectState.Detecting(progress)
            }
            _autoDetectState.value = result
        }
    }

    fun resetAutoDetect() {
        _autoDetectState.value = com.example.aurix.providers.autodetect.AutoDetectCoordinator.AutoDetectState.Idle
    }

    fun saveAutoDetectedKey(
        result: com.example.aurix.providers.autodetect.AutoDetectCoordinator.AutoDetectState.Success,
        labelOverride: String? = null,
        modelOverride: String? = null
    ) {
        viewModelScope.launch {
            val label = if (!labelOverride.isNullOrBlank()) labelOverride else "${result.provider.name} Auto"
            val modelId = if (!modelOverride.isNullOrBlank()) modelOverride else result.bestModel.id
            val entity = ProviderKeyEntity(
                id = "key_${result.provider.name.lowercase()}_${System.currentTimeMillis()}",
                providerType = result.provider,
                label = label,
                apiKeyMasked = SecureStorage.maskKey(result.rawKey),
                apiKeyEncrypted = SecureStorage.obfuscateKey(result.rawKey),
                modelId = modelId,
                isActive = true,
                latencyMs = result.latencyMs
            )
            db.providerKeyDao().insertOrUpdate(entity)
            resetAutoDetect()
        }
    }

    init {
        // Wire voice completion callback
        voiceManager.onUserSpeechCompleted = { spokenText ->
            handleUserMessage(spokenText, speakResponse = true)
        }

        // Initialize default conversation
        viewModelScope.launch {
            val conv = db.conversationDao().getConversationById("conv_default")
            if (conv == null) {
                db.conversationDao().insertOrUpdate(
                    ConversationEntity(id = "conv_default", title = "Welcome to AURIX")
                )
                db.messageDao().insertMessage(
                    MessageEntity(
                        id = "msg_welcome",
                        conversationId = "conv_default",
                        sender = "aurix",
                        content = "AURIX Core Online. I am your autonomous multimodal agent. You can talk to me directly via voice or type any command in English or Hinglish."
                    )
                )
            }
            loadMessages("conv_default")
        }
    }

    fun selectTab(tab: AurixTab) {
        _currentTab.value = tab
    }

    fun selectConversation(id: String) {
        _currentConversationId.value = id
        loadMessages(id)
    }

    fun createNewConversation() {
        viewModelScope.launch {
            val newId = "conv_${System.currentTimeMillis()}"
            val newConv = ConversationEntity(
                id = newId,
                title = "Session ${System.currentTimeMillis() % 1000}"
            )
            db.conversationDao().insertOrUpdate(newConv)
            _currentConversationId.value = newId
            loadMessages(newId)
        }
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            db.conversationDao().renameConversation(id, newTitle)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            db.conversationDao().deleteConversation(id)
            db.conversationDao().deleteMessagesForConversation(id)
            if (_currentConversationId.value == id) {
                val remaining = db.conversationDao().getAllConversations()
                // pick default
                _currentConversationId.value = "conv_default"
                loadMessages("conv_default")
            }
        }
    }

    private fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            db.messageDao().getMessagesForConversation(conversationId).collect { msgs ->
                _currentMessages.value = msgs
            }
        }
    }

    fun handleUserMessage(text: String, speakResponse: Boolean = false) {
        if (text.isBlank()) return
        val convId = _currentConversationId.value

        viewModelScope.launch {
            // Save user message
            val userMsg = MessageEntity(
                id = "msg_${System.currentTimeMillis()}",
                conversationId = convId,
                sender = "user",
                content = text
            )
            db.messageDao().insertMessage(userMsg)

            _isThinking.value = true

            // Check custom command macros first (Phase 32)
            val matchedCommand = personalCommandManager.findMatchingCommand(text)
            val brainResult = if (matchedCommand != null) {
                val reports = personalCommandManager.executeCommand(matchedCommand)
                com.example.aurix.brain.AgentBrain.BrainResult(
                    responseText = "⚡ Executed Custom Macro '${matchedCommand.name}':\n" + reports.joinToString("\n") { "• $it" },
                    telemetryReport = "Custom Command '${matchedCommand.name}' completed"
                )
            } else if (_isConsensusModeEnabled.value && !text.contains("on", ignoreCase = true) && !text.contains("off", ignoreCase = true)) {
                // Phase 31 Model Consensus Check
                val consensus = consensusEngine.queryConsensus(text)
                com.example.aurix.brain.AgentBrain.BrainResult(
                    responseText = consensus.finalAnswer + "\n\n[" + consensus.consensusNotes + "]",
                    telemetryReport = "Consensus agreement: ${(consensus.agreementRatio * 100).toInt()}%"
                )
            } else {
                val history = db.messageDao().getRecentMessages(convId, limit = 6).map {
                    ChatMessageItem(it.sender, it.content)
                }

                // Brain reasoning & execution
                brain.processUserRequest(
                    conversationId = convId,
                    userInput = text,
                    autonomyMode = _autonomyMode.value,
                    history = history
                )
            }

            _isThinking.value = false

            // Save assistant message
            val agentMsg = MessageEntity(
                id = "msg_resp_${System.currentTimeMillis()}",
                conversationId = convId,
                sender = "aurix",
                content = brainResult.responseText,
                missionId = brainResult.missionId
            )
            db.messageDao().insertMessage(agentMsg)

            // Update conversation title if first message
            val conv = db.conversationDao().getConversationById(convId)
            if (conv != null && conv.title.startsWith("Session")) {
                db.conversationDao().renameConversation(convId, text.take(24))
            }

            // If voice response requested
            _lastVoiceResponse.value = brainResult.responseText
            if (speakResponse || _currentTab.value == AurixTab.VOICE) {
                // Find ElevenLabs key if available
                val elevenKey = providerKeys.value.firstOrNull { it.providerType == ProviderType.ELEVENLABS && it.isActive }
                val rawElevenKey = elevenKey?.let { SecureStorage.deobfuscateKey(it.apiKeyEncrypted) }
                voiceManager.speak(
                    text = brainResult.responseText,
                    elevenLabsApiKey = rawElevenKey,
                    voiceId = _selectedVoiceId.value
                )
            }
        }
    }

    fun approveAction(approved: Boolean) {
        _pendingActionSummary.value = null
        if (approved) {
            handleUserMessage("proceed with action")
        }
    }

    fun startListening() {
        voiceManager.startListening()
    }

    fun stopListening() {
        voiceManager.stopListening()
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun setAutonomyMode(mode: AutonomyMode) {
        _autonomyMode.value = mode
    }

    fun addProviderKey(type: ProviderType, label: String, rawKey: String, modelId: String) {
        viewModelScope.launch {
            val entity = ProviderKeyEntity(
                id = "key_${type.name.lowercase()}_${System.currentTimeMillis()}",
                providerType = type,
                label = label,
                apiKeyMasked = SecureStorage.maskKey(rawKey),
                apiKeyEncrypted = SecureStorage.obfuscateKey(rawKey),
                modelId = modelId,
                isActive = true
            )
            db.providerKeyDao().insertOrUpdate(entity)
        }
    }

    fun deleteProviderKey(id: String) {
        viewModelScope.launch {
            db.providerKeyDao().deleteById(id)
        }
    }

    fun testProviderKey(keyEntity: ProviderKeyEntity) {
        viewModelScope.launch {
            providerRouter.testKey(keyEntity)
        }
    }

    fun selectVoice(voiceId: String) {
        _selectedVoiceId.value = voiceId
    }

    fun refreshElevenLabsVoices() {
        viewModelScope.launch {
            _isRefreshingVoices.value = true
            val elevenKey = providerKeys.value.firstOrNull { it.providerType == ProviderType.ELEVENLABS && it.isActive }
            val rawElevenKey = elevenKey?.let { SecureStorage.deobfuscateKey(it.apiKeyEncrypted) }
            if (!rawElevenKey.isNullOrBlank()) {
                val res = elevenLabsClient.getAvailableVoices(rawElevenKey)
                if (res.isSuccess) {
                    val voices = res.getOrNull().orEmpty()
                    if (voices.isNotEmpty()) {
                        _elevenLabsVoices.value = voices
                    }
                }
            }
            _isRefreshingVoices.value = false
        }
    }

    fun previewVoice(voiceId: String) {
        val elevenKey = providerKeys.value.firstOrNull { it.providerType == ProviderType.ELEVENLABS && it.isActive }
        val rawElevenKey = elevenKey?.let { SecureStorage.deobfuscateKey(it.apiKeyEncrypted) }
        voiceManager.speak(
            text = "AURIX voice systems initialized and ready.",
            elevenLabsApiKey = rawElevenKey,
            voiceId = voiceId
        )
    }

    fun addMemory(content: String, category: MemoryCategory) {
        viewModelScope.launch {
            memoryEngine.storeMemory(content, category, importance = 4)
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            memoryEngine.deleteMemory(id)
        }
    }

    fun resumeMission(missionId: String) {
        viewModelScope.launch {
            missionEngine.executeOrResumeMission(missionId, _autonomyMode.value)
        }
    }

    fun toggleConsensusMode(enabled: Boolean) {
        _isConsensusModeEnabled.value = enabled
    }

    fun toggleProactiveMode(enabled: Boolean) {
        _isProactiveModeEnabled.value = enabled
    }

    fun selectTelemetryForWhy(item: com.example.aurix.data.local.entity.DeviceTelemetryEntity?) {
        _selectedTelemetryForWhy.value = item
    }

    fun createCustomCommand(
        name: String,
        triggerPhrase: String,
        actions: List<com.example.aurix.commands.PersonalCommandManager.CommandAction>
    ) {
        viewModelScope.launch {
            personalCommandManager.createCommand(name, triggerPhrase, actions)
        }
    }

    fun deleteCustomCommand(cmd: com.example.aurix.data.local.entity.CustomCommandEntity) {
        viewModelScope.launch {
            personalCommandManager.deleteCommand(cmd)
        }
    }

    fun executeCustomCommand(cmd: com.example.aurix.data.local.entity.CustomCommandEntity) {
        viewModelScope.launch {
            val reports = personalCommandManager.executeCommand(cmd)
            val convId = _currentConversationId.value
            val msg = MessageEntity(
                id = "msg_${System.currentTimeMillis()}",
                conversationId = convId,
                sender = "aurix",
                content = "Executed '${cmd.name}':\n" + reports.joinToString("\n") { "• $it" }
            )
            db.messageDao().insertMessage(msg)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
