package com.example.domain.model

enum class DetectedFormatType(val label: String, val iconName: String) {
  JSON_OBJECT("JSON Object", "code"),
  JSON_ARRAY("JSON Array", "data_array"),
  URL_LINK("Web URL", "link"),
  EMAIL("Email Address", "email"),
  CODE_SNIPPET("Code Snippet", "terminal"),
  SECRET_CREDENTIAL("Secret Token / Key", "lock"),
  MARKDOWN("Markdown Text", "description"),
  PLAINTEXT("Plain Text", "notes")
}

data class FormatValidationResult(
  val formatType: DetectedFormatType,
  val isValid: Boolean,
  val validationMessage: String,
  val byteSize: Int,
  val lineCount: Int,
  val wordCount: Int,
  val detectedLanguageOrProtocol: String? = null,
  val isSensitiveCandidate: Boolean = false
)

enum class LogSeverity {
  INFO,
  WARN,
  ERROR,
  AUDIT
}

enum class LogComponent {
  CAPTURE,
  STORAGE,
  SECURITY,
  AUTOMATION,
  ENVIRONMENT,
  BACKUP,
  FIREBASE_SYNC,
  UI
}

data class DiagnosticLogEntry(
  val id: String = java.util.UUID.randomUUID().toString(),
  val timestamp: Long = System.currentTimeMillis(),
  val severity: LogSeverity,
  val component: LogComponent,
  val message: String,
  val details: String? = null
)

data class AutomationRule(
  val id: String,
  val name: String,
  val description: String,
  val isEnabled: Boolean = true,
  val triggerType: String = "ON_CAPTURE"
)
