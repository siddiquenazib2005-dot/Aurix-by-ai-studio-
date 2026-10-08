package com.example.aurix.voice

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ElevenLabsClient(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private var currentPlayer: MediaPlayer? = null

    suspend fun getAvailableVoices(apiKey: String): Result<List<ElevenLabsVoice>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.success(getDefaultVoices())
        }
        try {
            val req = Request.Builder()
                .url("https://api.elevenlabs.io/v1/voices")
                .addHeader("xi-api-key", apiKey)
                .get()
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                return@withContext Result.success(getDefaultVoices())
            }

            val body = resp.body?.string().orEmpty()
            val json = JSONObject(body)
            val voicesArr = json.optJSONArray("voices") ?: JSONArray()
            val list = mutableListOf<ElevenLabsVoice>()
            for (i in 0 until voicesArr.length()) {
                val v = voicesArr.getJSONObject(i)
                list.add(
                    ElevenLabsVoice(
                        voiceId = v.getString("voice_id"),
                        name = v.getString("name"),
                        category = v.optString("category", "custom"),
                        previewUrl = v.optString("preview_url")
                    )
                )
            }
            Result.success(if (list.isNotEmpty()) list else getDefaultVoices())
        } catch (e: Exception) {
            Result.success(getDefaultVoices())
        }
    }

    suspend fun synthesizeAndPlay(
        text: String,
        apiKey: String,
        voiceId: String = "21m00Tcm4TlvDq8ikWAM", // Default Rachel
        modelId: String = "eleven_turbo_v2_5",
        onCompletion: () -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            stopPlayback()

            val endpoint = "https://api.elevenlabs.io/v1/text-to-speech/$voiceId"
            val json = JSONObject()
            json.put("text", text)
            json.put("model_id", modelId)

            val voiceSettings = JSONObject()
            voiceSettings.put("stability", 0.5)
            voiceSettings.put("similarity_boost", 0.75)
            json.put("voice_settings", voiceSettings)

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url(endpoint)
                .addHeader("xi-api-key", apiKey)
                .post(body)
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("ElevenLabs API HTTP ${resp.code}"))
            }

            val audioBytes = resp.body?.bytes() ?: return@withContext Result.failure(Exception("Empty audio stream"))
            val tempFile = File.createTempFile("aurix_tts_", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            withContext(Dispatchers.Main) {
                val mp = MediaPlayer()
                currentPlayer = mp
                mp.setDataSource(tempFile.absolutePath)
                mp.prepare()
                mp.setOnCompletionListener {
                    tempFile.delete()
                    currentPlayer = null
                    onCompletion()
                }
                mp.start()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stopPlayback() {
        try {
            currentPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        currentPlayer = null
    }

    private fun getDefaultVoices(): List<ElevenLabsVoice> = listOf(
        ElevenLabsVoice("21m00Tcm4TlvDq8ikWAM", "Rachel (Calm & Clear)"),
        ElevenLabsVoice("AZnzlk1XvdvUeBnXmlld", "Domi (Confident & Strong)"),
        ElevenLabsVoice("EXAVITQu4vr4xnSDxMaL", "Bella (Warm & Expressive)"),
        ElevenLabsVoice("ErXwobaYiN019PkySvjV", "Antoni (Futuristic & Crisp)"),
        ElevenLabsVoice("VR6AewLTigWG4xSOukaG", "Arnold (Deep & Direct)")
    )
}
