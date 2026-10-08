package com.example

import com.example.aurix.brain.ConversationActionCompiler
import com.example.aurix.intelligence.NotificationClassifier
import com.example.aurix.security.SecureStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {

    @Test
    fun testIntentFusionMultilingual() {
        // Multi-intent prompt in Hinglish
        val input = "Mummy ko call karke bol de main late hoon aur 8 baje reminder laga dena"
        val plan = ConversationActionCompiler.compile(input)

        assertTrue("Should detect multi-step mission", plan.isMultiStepMission)
        assertTrue("Should have at least 2 steps", plan.steps.size >= 2)

        val toolNames = plan.steps.map { it.toolName }
        assertTrue("Should contain messaging tool", toolNames.contains("SEND_MESSAGE") || toolNames.contains("SEND_SMS"))
        assertTrue("Should contain reminder tool", toolNames.contains("CREATE_REMINDER"))
    }

    @Test
    fun testSingleIntentFlashlight() {
        val input = "Flashlight on kar do"
        val plan = ConversationActionCompiler.compile(input)

        assertFalse("Single intent should not be multi-step", plan.isMultiStepMission)
        assertEquals(1, plan.steps.size)
        assertEquals("FLASHLIGHT", plan.steps[0].toolName)
    }

    @Test
    fun testSecureStorageMaskingAndRedaction() {
        val rawKey = "sk-proj-1234567890abcdef"
        val masked = SecureStorage.maskKey(rawKey)
        assertTrue("Masked key should hide center bytes", masked.contains("••••"))

        val obfuscated = SecureStorage.obfuscateKey(rawKey)
        val restored = SecureStorage.deobfuscateKey(obfuscated)
        assertEquals("Obfuscation should be reversible", rawKey, restored)

        val sensitiveLog = "Request with token AIzaSyD9876543210zyxwvutsrqponmlkjihgfed and sk-12345678901234567890"
        val redacted = SecureStorage.redactSecrets(sensitiveLog)
        assertFalse("Redacted string should not contain raw AIza key", redacted.contains("AIzaSyD9876543210"))
    }

    @Test
    fun testNotificationClassifierUrgentAndPromo() {
        val urgent = NotificationClassifier.classify(
            "com.google.android.apps.messaging",
            "Bank Alert",
            "Your OTP is 894123 for transaction"
        )
        assertEquals(NotificationClassifier.PriorityCategory.URGENT, urgent.category)
        assertTrue(urgent.summary.contains("Urgent"))

        val promo = NotificationClassifier.classify(
            "com.swiggy.consumer",
            "Special Offer",
            "50% discount on food delivery today!"
        )
        assertEquals(NotificationClassifier.PriorityCategory.PROMOTIONAL, promo.category)

        val important = NotificationClassifier.classify(
            "com.whatsapp",
            "Priya",
            "Meeting at 4pm today"
        )
        assertEquals(NotificationClassifier.PriorityCategory.IMPORTANT, important.category)
    }

    @Test
    fun testApiKeyDetectionHints() {
        val geminiKey = "AIzaSyD9876543210zyxwvutsrqponmlkjihgfed"
        assertEquals(com.example.aurix.data.model.ProviderType.GEMINI, com.example.aurix.providers.autodetect.ApiKeyDetector.detectProviderHint(geminiKey))

        val groqKey = "gsk_1234567890abcdef1234567890abcdef"
        assertEquals(com.example.aurix.data.model.ProviderType.GROQ, com.example.aurix.providers.autodetect.ApiKeyDetector.detectProviderHint(groqKey))

        val openRouterKey = "sk-or-v1-abcdef1234567890"
        assertEquals(com.example.aurix.data.model.ProviderType.OPENROUTER, com.example.aurix.providers.autodetect.ApiKeyDetector.detectProviderHint(openRouterKey))

        val openAiKey = "sk-proj-9876543210zyxwvutsrqponmlkjih"
        assertEquals(com.example.aurix.data.model.ProviderType.OPENAI_COMPATIBLE, com.example.aurix.providers.autodetect.ApiKeyDetector.detectProviderHint(openAiKey))
    }

    @Test
    fun testModelCapabilityResolverAndSelectionEngine() {
        val resolver = com.example.aurix.providers.autodetect.ModelCapabilityResolver
        assertTrue(resolver.isCompatibleForAgent("llama-3.3-70b-versatile", com.example.aurix.data.model.ProviderType.GROQ))
        assertFalse(resolver.isCompatibleForAgent("text-embedding-ada-002", com.example.aurix.data.model.ProviderType.OPENAI_COMPATIBLE))
        assertFalse(resolver.isCompatibleForAgent("whisper-large-v3", com.example.aurix.data.model.ProviderType.GROQ))

        val engine = com.example.aurix.providers.autodetect.ModelSelectionEngine()
        val models = listOf(
            resolver.resolveCapabilities("llama-3.1-8b-instant", com.example.aurix.data.model.ProviderType.GROQ),
            resolver.resolveCapabilities("llama-3.3-70b-versatile", com.example.aurix.data.model.ProviderType.GROQ),
            resolver.resolveCapabilities("gemma2-9b-it", com.example.aurix.data.model.ProviderType.GROQ)
        )
        val selection = engine.selectBestModel(models, com.example.aurix.data.model.ProviderType.GROQ)
        assertEquals("llama-3.3-70b-versatile", selection.bestModel.id)
        assertTrue(selection.bestModel.supportsFunctionCalling)
    }
}
