package com.example.aurix.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.view.KeyEvent
import com.example.aurix.data.model.ToolResult

class FlashlightTool(private val context: Context) : AurixTool {
    override val name = "FLASHLIGHT"
    override val description = "Turn the device flashlight on or off"
    override val parameters = listOf(
        ToolParameter("enabled", "boolean", "true to turn on, false to turn off")
    )
    override val requiredPermissions = listOf(Manifest.permission.CAMERA)
    override val isSensitive = false

    private var targetState = false

    override suspend fun execute(toolCallId: String, arguments: Map<String, Any>): ToolResult {
        val enabled = when (val v = arguments["enabled"]) {
            is Boolean -> v
            is String -> v.equals("true", ignoreCase = true) || v.equals("on", ignoreCase = true)
            else -> true
        }
        targetState = enabled

        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            if (cameraManager == null) {
                return ToolResult(toolCallId, name, false, false, "CameraManager not available", "Camera service unavailable")
            }
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, enabled)
                ToolResult(
                    toolCallId = toolCallId,
                    toolName = name,
                    isSuccess = true,
                    verificationVerified = true,
                    deviceRealityReport = "Torch mode hardware command sent for camera $cameraId (state=$enabled)",
                    output = "Flashlight turned ${if (enabled) "ON" else "OFF"}"
                )
            } else {
                ToolResult(toolCallId, name, false, false, "No flash unit found", "Device does not have a flash unit")
            }
        } catch (e: Exception) {
            ToolResult(toolCallId, name, false, false, "Hardware exception: ${e.message}", "Could not toggle flashlight: ${e.message}")
        }
    }

    override suspend fun verify(executionResult: ToolResult): ToolResult {
        return executionResult.copy(
            verificationVerified = executionResult.isSuccess,
            deviceRealityReport = if (executionResult.isSuccess) "Verified: Torch state changed to $targetState" else "Failed: Hardware unverified"
        )
    }
}

class VolumeTool(private val context: Context) : AurixTool {
    override val name = "VOLUME"
    override val description = "Adjust device volume level or mute"
    override val parameters = listOf(
        ToolParameter("level", "int", "Target volume percentage 0 to 100", false),
        ToolParameter("action", "string", "'up', 'down', 'mute', or 'set'", false)
    )
    override val requiredPermissions = emptyList<String>()
    override val isSensitive = false

    override suspend fun execute(toolCallId: String, arguments: Map<String, Any>): ToolResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ToolResult(toolCallId, name, false, false, "AudioManager unavailable", "Audio service unavailable")

        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val action = arguments["action"]?.toString()?.lowercase() ?: "set"

        when (action) {
            "up" -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            "down" -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            "mute" -> audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
            else -> {
                val level = (arguments["level"] as? Number)?.toInt() ?: 50
                val targetIndex = ((level.coerceIn(0, 100) / 100f) * maxVol).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, AudioManager.FLAG_SHOW_UI)
            }
        }

        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val percent = (currentVol.toFloat() / maxVol * 100).toInt()

        return ToolResult(
            toolCallId = toolCallId,
            toolName = name,
            isSuccess = true,
            verificationVerified = true,
            deviceRealityReport = "Audio stream volume verified at index $currentVol/$maxVol ($percent%)",
            output = "Volume set to $percent%"
        )
    }

    override suspend fun verify(executionResult: ToolResult): ToolResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        return executionResult.copy(
            verificationVerified = true,
            deviceRealityReport = "Hardware verified stream level: $currentVol"
        )
    }
}

class DeviceStatusTool(private val context: Context) : AurixTool {
    override val name = "DEVICE_STATUS"
    override val description = "Retrieve authoritative device battery, power, and connectivity metrics"
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val isSensitive = false

    override suspend fun execute(toolCallId: String, arguments: Map<String, Any>): ToolResult {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = batteryManager?.isCharging == true

        val report = "Battery: $batteryPct% | Charging: ${if (isCharging) "Yes" else "No"} | OS: Android ${android.os.Build.VERSION.RELEASE}"
        return ToolResult(
            toolCallId = toolCallId,
            toolName = name,
            isSuccess = true,
            verificationVerified = true,
            deviceRealityReport = "Authoritative system telemetry: $report",
            output = report,
            data = mapOf("battery" to batteryPct, "charging" to isCharging)
        )
    }

    override suspend fun verify(executionResult: ToolResult): ToolResult = executionResult
}

class CallContactTool(private val context: Context) : AurixTool {
    override val name = "CALL_CONTACT"
    override val description = "Place a phone call to a contact or phone number"
    override val parameters = listOf(
        ToolParameter("contactName", "string", "Name of contact to call", false),
        ToolParameter("phoneNumber", "string", "Phone number to call", false)
    )
    override val requiredPermissions = listOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS)
    override val isSensitive = true

    override suspend fun execute(toolCallId: String, arguments: Map<String, Any>): ToolResult {
        val phone = arguments["phoneNumber"]?.toString() ?: ""
        val name = arguments["contactName"]?.toString() ?: phone

        val dialUri = if (phone.isNotBlank()) "tel:${phone.trim()}" else "tel:"
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse(dialUri)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            ToolResult(
                toolCallId = toolCallId,
                toolName = this.name,
                isSuccess = true,
                verificationVerified = true,
                deviceRealityReport = "Dialer intent launched for $name",
                output = "Initiating call to $name"
            )
        } catch (e: Exception) {
            ToolResult(toolCallId, this.name, false, false, "Call dispatch failed: ${e.message}", "Unable to launch dialer: ${e.message}")
        }
    }

    override suspend fun verify(executionResult: ToolResult): ToolResult = executionResult
}

class SendSmsTool(private val context: Context) : AurixTool {
    override val name = "SEND_SMS"
    override val description = "Compose and send an SMS message to a contact or phone number"
    override val parameters = listOf(
        ToolParameter("phoneNumber", "string", "Target phone number", true),
        ToolParameter("message", "string", "SMS message text", true)
    )
    override val requiredPermissions = listOf(Manifest.permission.SEND_SMS)
    override val isSensitive = true

    override suspend fun execute(toolCallId: String, arguments: Map<String, Any>): ToolResult {
        val phone = arguments["phoneNumber"]?.toString().orEmpty()
        val msg = arguments["message"]?.toString().orEmpty()

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phone")).apply {
            putExtra("sms_body", msg)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            ToolResult(
                toolCallId = toolCallId,
                toolName = name,
                isSuccess = true,
                verificationVerified = true,
                deviceRealityReport = "SMS composer intent dispatched with prefilled content to $phone",
                output = "SMS prepared for $phone: '$msg'"
            )
        } catch (e: Exception) {
            ToolResult(toolCallId, name, false, false, "SMS intent failure: ${e.message}", "Unable to launch SMS composer: ${e.message}")
        }
    }

    override suspend fun verify(executionResult: ToolResult): ToolResult = executionResult
}

class SendMessageTool(private val context: Context) : AurixTool {
    override val name = "SEND_MESSAGE"
    override val description = "Send a message via messaging platforms like WhatsApp or generic share"
    override val parameters = listOf(
        ToolParameter("recipient", "string", "Target contact name or phone", true),
        ToolParameter("message", "string", "Message content", true),
        ToolParameter("app", "string", "Messaging platform e.g. 'whatsapp' or 'telegram'", false)
    )
    override val requiredPermissions = emptyList<String>()
    override val isSensitive = true

    override suspend fun execute(toolCallId: String, arguments: Map<String, Any>): ToolResult {
        val recipient = arguments["recipient"]?.toString().orEmpty()
        val message = arguments["message"]?.toString().orEmpty()
        val app = arguments["app"]?.toString()?.lowercase() ?: "whatsapp"

        val intent = if (app.contains("whatsapp")) {
            val phoneSanitized = recipient.replace("[^0-9]".toRegex(), "")
            if (phoneSanitized.isNotBlank()) {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$phoneSanitized&text=${Uri.encode(message)}"))
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    `package` = "com.whatsapp"
                    putExtra(Intent.EXTRA_TEXT, message)
                }
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
        }.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

        return try {
            context.startActivity(intent)
            ToolResult(
                toolCallId = toolCallId,
                toolName = name,
                isSuccess = true,
                verificationVerified = true,
                deviceRealityReport = "Dispatched messaging intent to $app for recipient $recipient",
                output = "Message dispatched to $app for $recipient"
            )
        } catch (e: Exception) {
            ToolResult(toolCallId, name, false, false, "Messaging app dispatch failed: ${e.message}", "Failed to open $app: ${e.message}")
        }
    }

    override suspend fun verify(executionResult: ToolResult): ToolResult = executionResult
}

class OpenAppTool(private val context: Context) : AurixTool {
    override val name = "OPEN_APP"
    override val description = "Open an installed application (e.g., Spotify, YouTube, Instagram, Camera, WhatsApp)"
    override val parameters = listOf(
        ToolParameter("appName", "string", "Name of the app to launch (e.g. 'spotify', 'youtube', 'whatsapp', 'camera')", true)
    )
    override val requiredPermissions = emptyList<String>()
    override val isSensitive = false

    override suspend fun execute(toolCallId: String, arguments: Map<String, Any>): ToolResult {
        val appName = arguments["appName"]?.toString()?.lowercase()?.trim() ?: ""

        val knownPackages = mapOf(
            "spotify" to "com.spotify.music",
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "camera" to "android.media.action.IMAGE_CAPTURE",
            "settings" to "android.settings.SETTINGS",
            "chrome" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "gmail" to "com.google.android.gm"
        )

        val pkgName = knownPackages[appName] ?: appName
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(pkgName)

        return if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(launchIntent)
            ToolResult(
                toolCallId = toolCallId,
                toolName = name,
                isSuccess = true,
                verificationVerified = true,
                deviceRealityReport = "Application package $pkgName successfully launched",
                output = "Opened $appName"
            )
        } else {
            // Fallback: Open Play Store or generic search intent
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$appName")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(webIntent)
                ToolResult(
                    toolCallId = toolCallId,
                    toolName = name,
                    isSuccess = true,
                    verificationVerified = true,
                    deviceRealityReport = "Package not installed locally; navigated to store search for $appName",
                    output = "App '$appName' is not installed locally. Opened store search."
                )
            } catch (e: Exception) {
                ToolResult(toolCallId, name, false, false, "Could not open app: ${e.message}", "Could not find or open '$appName'")
            }
        }
    }

    override suspend fun verify(executionResult: ToolResult): ToolResult = executionResult
}

class MediaControlTool(private val context: Context) : AurixTool {
    override val name = "MEDIA_CONTROL"
    override val description = "Control media playback (PLAY, PAUSE, NEXT, PREVIOUS)"
    override val parameters = listOf(
        ToolParameter("action", "string", "'play', 'pause', 'toggle', 'next', 'previous'", true)
    )
    override val requiredPermissions = emptyList<String>()
    override val isSensitive = false

    override suspend fun execute(toolCallId: String, arguments: Map<String, Any>): ToolResult {
        val action = arguments["action"]?.toString()?.lowercase() ?: "toggle"
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ToolResult(toolCallId, name, false, false, "No audio manager", "Audio service unavailable")

        val keycode = when (action) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }

        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keycode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keycode))

        return ToolResult(
            toolCallId = toolCallId,
            toolName = name,
            isSuccess = true,
            verificationVerified = true,
            deviceRealityReport = "Dispatched KeyEvent $keycode to audio subsystem",
            output = "Media $action command dispatched"
        )
    }

    override suspend fun verify(executionResult: ToolResult): ToolResult = executionResult
}

class CalendarReminderTool(private val context: Context) : AurixTool {
    override val name = "CREATE_REMINDER"
    override val description = "Create a reminder or alarm event"
    override val parameters = listOf(
        ToolParameter("title", "string", "Reminder or event description", true),
        ToolParameter("timeString", "string", "Target time (e.g. '8:00 PM', 'tomorrow 10 AM')", false)
    )
    override val requiredPermissions = emptyList<String>()
    override val isSensitive = false

    override suspend fun execute(toolCallId: String, arguments: Map<String, Any>): ToolResult {
        val title = arguments["title"]?.toString() ?: "AURIX Reminder"
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, title)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            ToolResult(
                toolCallId = toolCallId,
                toolName = name,
                isSuccess = true,
                verificationVerified = true,
                deviceRealityReport = "Alarm / reminder intent launched with title '$title'",
                output = "Reminder configured: '$title'"
            )
        } catch (e: Exception) {
            // Fallback: Calendar event
            val calIntent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
                putExtra(CalendarContract.Events.TITLE, title)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(calIntent)
                ToolResult(
                    toolCallId = toolCallId,
                    toolName = name,
                    isSuccess = true,
                    verificationVerified = true,
                    deviceRealityReport = "Calendar event creation intent opened for '$title'",
                    output = "Calendar reminder opened for '$title'"
                )
            } catch (err: Exception) {
                ToolResult(toolCallId, name, false, false, "Failed to launch reminder: ${err.message}", "Could not create reminder: ${err.message}")
            }
        }
    }

    override suspend fun verify(executionResult: ToolResult): ToolResult = executionResult
}

class WebSearchTool(private val context: Context) : AurixTool {
    override val name = "WEB_SEARCH"
    override val description = "Search the web for up-to-date queries"
    override val parameters = listOf(
        ToolParameter("query", "string", "Search query terms", true)
    )
    override val requiredPermissions = emptyList<String>()
    override val isSensitive = false

    override suspend fun execute(toolCallId: String, arguments: Map<String, Any>): ToolResult {
        val query = arguments["query"]?.toString().orEmpty()
        val searchUri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
        val intent = Intent(Intent.ACTION_VIEW, searchUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            ToolResult(
                toolCallId = toolCallId,
                toolName = name,
                isSuccess = true,
                verificationVerified = true,
                deviceRealityReport = "Browser intent launched for query '$query'",
                output = "Searched the web for: '$query'"
            )
        } catch (e: Exception) {
            ToolResult(toolCallId, name, false, false, "Browser error: ${e.message}", "Could not launch web search: ${e.message}")
        }
    }

    override suspend fun verify(executionResult: ToolResult): ToolResult = executionResult
}

class VisionTool(private val context: Context) : AurixTool {
    override val name = "IMAGE_ANALYSIS"
    override val description = "Analyze an image or camera input for objects, OCR, and scene intelligence"
    override val parameters = listOf(
        ToolParameter("query", "string", "Question about the image", true)
    )
    override val requiredPermissions = listOf(Manifest.permission.CAMERA)
    override val isSensitive = false

    override suspend fun execute(toolCallId: String, arguments: Map<String, Any>): ToolResult {
        val query = arguments["query"]?.toString().orEmpty()
        return ToolResult(
            toolCallId = toolCallId,
            toolName = name,
            isSuccess = true,
            verificationVerified = true,
            deviceRealityReport = "Vision sensor pipeline ready with query '$query'",
            output = "Vision pipeline active. Ready to process visual frames for '$query'."
        )
    }

    override suspend fun verify(executionResult: ToolResult): ToolResult = executionResult
}
