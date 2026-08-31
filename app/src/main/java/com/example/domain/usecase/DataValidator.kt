package com.example.domain.usecase

import com.example.domain.model.DetectedFormatType
import com.example.domain.model.FormatValidationResult
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.Locale
import java.util.regex.Pattern

object DataValidator {

  private val EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$")
  private val JWT_REGEX = Pattern.compile("^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*$")

  fun inspectAndValidate(input: String): FormatValidationResult {
    val trimmed = input.trim()
    val byteSize = input.toByteArray(Charsets.UTF_8).size
    val lineCount = if (input.isEmpty()) 0 else input.lines().size
    val wordCount = if (input.isBlank()) 0 else input.split("\\s+".toRegex()).count { it.isNotBlank() }

    if (trimmed.isEmpty()) {
      return FormatValidationResult(
        formatType = DetectedFormatType.PLAINTEXT,
        isValid = true,
        validationMessage = "Ready for input",
        byteSize = 0,
        lineCount = 0,
        wordCount = 0
      )
    }

    // 1. JSON Check
    if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
      val isArray = trimmed.startsWith("[")
      return try {
        if (isArray) {
          JSONArray(trimmed)
          FormatValidationResult(
            formatType = DetectedFormatType.JSON_ARRAY,
            isValid = true,
            validationMessage = "Valid JSON Array structure",
            byteSize = byteSize,
            lineCount = lineCount,
            wordCount = wordCount
          )
        } else {
          JSONObject(trimmed)
          FormatValidationResult(
            formatType = DetectedFormatType.JSON_OBJECT,
            isValid = true,
            validationMessage = "Valid JSON Object structure",
            byteSize = byteSize,
            lineCount = lineCount,
            wordCount = wordCount
          )
        }
      } catch (e: Exception) {
        FormatValidationResult(
          formatType = if (isArray) DetectedFormatType.JSON_ARRAY else DetectedFormatType.JSON_OBJECT,
          isValid = false,
          validationMessage = "Malformed JSON syntax: ${e.message?.take(60)}",
          byteSize = byteSize,
          lineCount = lineCount,
          wordCount = wordCount
        )
      }
    }

    // 2. URL Check
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) || trimmed.startsWith("ftp://", ignoreCase = true)) {
      return try {
        val uri = URI(trimmed)
        val hasHost = !uri.host.isNullOrBlank()
        FormatValidationResult(
          formatType = DetectedFormatType.URL_LINK,
          isValid = hasHost,
          validationMessage = if (hasHost) "Valid Web URI (${uri.scheme?.uppercase(Locale.ROOT)})" else "Invalid URL: missing host",
          byteSize = byteSize,
          lineCount = lineCount,
          wordCount = wordCount,
          detectedLanguageOrProtocol = uri.scheme
        )
      } catch (e: Exception) {
        FormatValidationResult(
          formatType = DetectedFormatType.URL_LINK,
          isValid = false,
          validationMessage = "Malformed URL structure",
          byteSize = byteSize,
          lineCount = lineCount,
          wordCount = wordCount
        )
      }
    }

    // 3. Email Check
    if (EMAIL_REGEX.matcher(trimmed).matches()) {
      return FormatValidationResult(
        formatType = DetectedFormatType.EMAIL,
        isValid = true,
        validationMessage = "Valid RFC Email address",
        byteSize = byteSize,
        lineCount = lineCount,
        wordCount = wordCount
      )
    }

    // 4. Secret / Token / Credential check
    val lower = trimmed.lowercase(Locale.ROOT)
    val secretKeywords = listOf("api_key", "bearer ", "token=", "private_key", "password=", "secret=", "sk-ant-", "sk-proj-", "ghp_")
    val hasSecretKeyword = secretKeywords.any { lower.contains(it) }
    val isJwt = JWT_REGEX.matcher(trimmed).matches() && trimmed.length > 50

    if (hasSecretKeyword || isJwt) {
      return FormatValidationResult(
        formatType = DetectedFormatType.SECRET_CREDENTIAL,
        isValid = true,
        validationMessage = if (isJwt) "JWT Authentication Token detected" else "Sensitive secret/credential signature detected",
        byteSize = byteSize,
        lineCount = lineCount,
        wordCount = wordCount,
        isSensitiveCandidate = true
      )
    }

    // 5. Code Snippet check
    val codeIndicators = listOf("class ", "fun ", "def ", "function ", "import ", "const ", "var ", "val ", "public static", "#include", "SELECT ", "CREATE TABLE")
    val matchesCode = codeIndicators.any { trimmed.contains(it) } || trimmed.startsWith("```")
    if (matchesCode) {
      val lang = when {
        trimmed.contains("fun ") || trimmed.contains("val ") -> "Kotlin"
        trimmed.contains("def ") -> "Python"
        trimmed.contains("function ") || trimmed.contains("const ") -> "JavaScript"
        trimmed.contains("SELECT ") || trimmed.contains("CREATE TABLE") -> "SQL"
        else -> "Source Code"
      }
      return FormatValidationResult(
        formatType = DetectedFormatType.CODE_SNIPPET,
        isValid = true,
        validationMessage = "Formatted $lang Snippet",
        byteSize = byteSize,
        lineCount = lineCount,
        wordCount = wordCount,
        detectedLanguageOrProtocol = lang
      )
    }

    // 6. Markdown Check
    if (trimmed.startsWith("#") || trimmed.contains("## ") || trimmed.contains("- [ ]") || trimmed.contains("- [x]")) {
      return FormatValidationResult(
        formatType = DetectedFormatType.MARKDOWN,
        isValid = true,
        validationMessage = "Structured Markdown Document",
        byteSize = byteSize,
        lineCount = lineCount,
        wordCount = wordCount
      )
    }

    // Default Plaintext
    return FormatValidationResult(
      formatType = DetectedFormatType.PLAINTEXT,
      isValid = true,
      validationMessage = "Plain Text",
      byteSize = byteSize,
      lineCount = lineCount,
      wordCount = wordCount
    )
  }
}
