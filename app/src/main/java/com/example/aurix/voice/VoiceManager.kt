package com.example.aurix.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceManager(
    private val context: Context,
    val elevenLabsClient: ElevenLabsClient,
    private val scope: CoroutineScope
) {
    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f) // Normalized 0f..1f for Orb pulsing
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _liveTranscription = MutableStateFlow("")
    val liveTranscription: StateFlow<String> = _liveTranscription.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var androidTts: TextToSpeech? = null
    private var isTtsReady = false

    var onUserSpeechCompleted: ((String) -> Unit)? = null

    init {
        initAndroidTts()
    }

    private fun initAndroidTts() {
        androidTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                androidTts?.language = Locale.getDefault()
                isTtsReady = true
            }
        }
    }

    fun startListening() {
        // If speaking, interrupt immediately (Barge-in / Phase 11)
        if (_voiceState.value == VoiceState.SPEAKING) {
            _voiceState.value = VoiceState.INTERRUPTED
        }
        stopSpeaking()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = VoiceState.ERROR
            return
        }

        _voiceState.value = VoiceState.LISTENING
        _liveTranscription.value = ""

        scope.launch(Dispatchers.Main) {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.value = VoiceState.LISTENING
                    }

                    override fun onBeginningOfSpeech() {
                        // User started talking; ensure any lingering audio is cut off
                        stopSpeaking()
                        _voiceState.value = VoiceState.LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize -2dB..10dB to 0..1f
                        val norm = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _rmsLevel.value = norm
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _rmsLevel.value = 0f
                        _voiceState.value = VoiceState.THINKING
                    }

                    override fun onError(error: Int) {
                        _rmsLevel.value = 0f
                        if (_voiceState.value == VoiceState.LISTENING) {
                            _voiceState.value = VoiceState.IDLE
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _rmsLevel.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull().orEmpty().trim()
                        if (text.isNotBlank()) {
                            _liveTranscription.value = text
                            _voiceState.value = VoiceState.THINKING
                            onUserSpeechCompleted?.invoke(text)
                        } else {
                            _voiceState.value = VoiceState.IDLE
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let { partial ->
                            _liveTranscription.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            speechRecognizer?.startListening(intent)
        }
    }

    fun stopListening() {
        scope.launch(Dispatchers.Main) {
            speechRecognizer?.stopListening()
            if (_voiceState.value == VoiceState.LISTENING) {
                _voiceState.value = VoiceState.IDLE
            }
        }
    }

    fun speak(text: String, elevenLabsApiKey: String?, voiceId: String?, onDone: () -> Unit = {}) {
        _voiceState.value = VoiceState.SPEAKING

        // If ElevenLabs key provided, attempt realistic neural voice
        if (!elevenLabsApiKey.isNullOrBlank()) {
            scope.launch {
                val result = elevenLabsClient.synthesizeAndPlay(
                    text = text,
                    apiKey = elevenLabsApiKey,
                    voiceId = voiceId ?: "21m00Tcm4TlvDq8ikWAM",
                    onCompletion = {
                        _voiceState.value = VoiceState.IDLE
                        onDone()
                    }
                )
                if (result.isFailure) {
                    // Graceful fallback to Android built-in TTS
                    speakWithAndroidTts(text, onDone)
                }
            }
        } else {
            speakWithAndroidTts(text, onDone)
        }
    }

    private fun speakWithAndroidTts(text: String, onDone: () -> Unit) {
        if (!isTtsReady || androidTts == null) {
            _voiceState.value = VoiceState.IDLE
            onDone()
            return
        }

        androidTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _voiceState.value = VoiceState.SPEAKING
            }

            override fun onDone(utteranceId: String?) {
                _voiceState.value = VoiceState.IDLE
                onDone()
            }

            override fun onError(utteranceId: String?) {
                _voiceState.value = VoiceState.IDLE
                onDone()
            }
        })

        val utteranceId = "aurix_tts_${System.currentTimeMillis()}"
        androidTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        elevenLabsClient.stopPlayback()
        androidTts?.stop()
        if (_voiceState.value == VoiceState.SPEAKING) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun updateState(state: VoiceState) {
        _voiceState.value = state
    }

    fun release() {
        speechRecognizer?.destroy()
        androidTts?.shutdown()
        elevenLabsClient.stopPlayback()
    }
}
