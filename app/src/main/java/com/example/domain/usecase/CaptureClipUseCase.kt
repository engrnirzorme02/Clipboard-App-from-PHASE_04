package com.example.domain.usecase

import com.example.data.repository.ClipRepository
import com.example.domain.model.CaptureResult
import com.example.domain.model.ClipItem
import com.example.domain.model.ClipSource
import com.example.domain.model.LogComponent
import com.example.domain.model.LogSeverity
import com.example.domain.model.SensitivityLevel
import com.example.domain.model.SyncState
import java.security.MessageDigest
import java.util.Locale
import java.util.regex.Pattern

class CaptureClipUseCase(
  private val clipRepository: ClipRepository
) {

  suspend fun execute(
    rawText: String,
    title: String? = null,
    source: ClipSource = ClipSource.KEYBOARD,
    userTags: List<String> = emptyList(),
    allowDuplicate: Boolean = false,
    forceSensitivity: SensitivityLevel? = null
  ): CaptureResult {
    val trimmedText = rawText.trim()

    // 1. Validation
    if (trimmedText.isEmpty()) {
      DiagnosticLogger.log(LogSeverity.WARN, LogComponent.CAPTURE, "Rejected empty capture input")
      return CaptureResult.Rejected("Input cannot be empty or whitespace only.")
    }
    if (trimmedText.length > 100_000) {
      DiagnosticLogger.log(LogSeverity.WARN, LogComponent.CAPTURE, "Rejected input exceeding size limit")
      return CaptureResult.Rejected("Text exceeds maximum supported size (100,000 characters).")
    }

    // 2. Normalized Hash
    val normalizedHash = computeNormalizedHash(trimmedText)

    // 3. Duplicate Check
    if (!allowDuplicate) {
      val existing = clipRepository.findByNormalizedHash(normalizedHash)
      if (existing != null && existing.text.trim() == trimmedText) {
        DiagnosticLogger.log(LogSeverity.INFO, LogComponent.CAPTURE, "Duplicate clip detected: ${existing.shortCode}")
        return CaptureResult.Duplicate(existingClip = existing, submittedText = trimmedText)
      }
    }

    // 4. Auto classification
    val detectedTitle = if (!title.isNullOrBlank()) {
      title.trim()
    } else {
      extractFirstLineTitle(trimmedText)
    }

    val autoTags = detectAutoTags(trimmedText)
    val combinedTags = (userTags + autoTags).map { it.trim().lowercase(Locale.ROOT) }.distinct()
    val detectedSensitivity = forceSensitivity ?: detectSensitivity(trimmedText)

    val newClip = ClipItem(
      text = trimmedText,
      normalizedHash = normalizedHash,
      title = detectedTitle,
      tags = combinedTags,
      source = source,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis(),
      pinned = false,
      archived = false,
      sensitivity = detectedSensitivity,
      syncState = SyncState.LOCAL_ONLY
    )

    clipRepository.insertClip(newClip)
    DiagnosticLogger.log(
      severity = LogSeverity.INFO,
      component = LogComponent.CAPTURE,
      message = "Clip saved [${newClip.shortCode}]",
      details = "Source: $source, Tags: ${combinedTags.joinToString()}, Sensitivity: $detectedSensitivity"
    )
    return CaptureResult.Success(newClip)
  }

  private fun computeNormalizedHash(text: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  private fun extractFirstLineTitle(text: String): String {
    val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: text
    val clean = firstLine.trim()
    return if (clean.length > 45) {
      clean.take(45) + "..."
    } else {
      clean
    }
  }

  private fun detectAutoTags(text: String): List<String> {
    val tags = mutableListOf<String>()
    if (text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)) {
      tags.add("link")
    }
    if (text.contains("@") && text.matches(Regex(".*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*"))) {
      tags.add("email")
    }
    if (text.startsWith("{") && text.endsWith("}") || text.startsWith("[") && text.endsWith("]")) {
      tags.add("json")
    }
    if (text.contains("fun ") || text.contains("class ") || text.contains("import ") || text.contains("const ") || text.contains("def ")) {
      tags.add("code")
    }
    if (text.lines().size > 4) {
      tags.add("multiline")
    }
    return tags
  }

  private fun detectSensitivity(text: String): SensitivityLevel {
    val lower = text.lowercase(Locale.ROOT)
    val sensitiveKeywords = listOf("password", "api_key", "bearer ", "secret", "private_key", "token=", "passphrase", "cvv", "pin:")
    val hasKeyword = sensitiveKeywords.any { lower.contains(it) }
    val matchesJwt = text.matches(Regex("^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*$")) && text.length > 50

    return if (hasKeyword || matchesJwt) {
      SensitivityLevel.SENSITIVE
    } else {
      SensitivityLevel.NORMAL
    }
  }
}
