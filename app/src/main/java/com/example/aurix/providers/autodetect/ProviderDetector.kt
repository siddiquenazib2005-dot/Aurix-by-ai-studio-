package com.example.aurix.providers.autodetect

import com.example.aurix.data.model.ProviderType

class ProviderDetector(
    private val validator: ProviderValidator = ProviderValidator()
) {
    data class DetectionResult(
        val isSuccess: Boolean,
        val provider: ProviderType? = null,
        val validationResult: ProviderValidator.ValidationResult? = null,
        val failureReason: String? = null
    )

    suspend fun detectAndValidate(apiKey: String): DetectionResult {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            return DetectionResult(false, failureReason = "API key cannot be empty.")
        }

        val hint = ApiKeyDetector.detectProviderHint(trimmed)

        // Probe priority order: hinted provider first, then others
        val probeList = mutableListOf<ProviderType>()
        if (hint != null) probeList.add(hint)
        ProviderType.values().forEach { p ->
            if (!probeList.contains(p)) probeList.add(p)
        }

        var lastError: String? = null
        for (provider in probeList) {
            val result = validator.validate(provider, trimmed)
            if (result.isConfirmed) {
                return DetectionResult(
                    isSuccess = true,
                    provider = provider,
                    validationResult = result
                )
            } else {
                lastError = result.errorMessage
            }
        }

        return DetectionResult(
            isSuccess = false,
            failureReason = "Provider could not be detected automatically. $lastError"
        )
    }
}
