package com.example.domain.model

import java.util.UUID

enum class ClipSource {
  KEYBOARD,
  CLIPBOARD,
  SHARE_INTENT,
  ACCESSIBILITY
}

enum class SensitivityLevel {
  NORMAL,
  SENSITIVE,
  EXCLUDED
}

enum class SyncState {
  LOCAL_ONLY,
  QUEUED,
  SYNCED,
  CONFLICT,
  FAILED
}

data class ClipItem(
  val id: String = UUID.randomUUID().toString(),
  val shortCode: String = generateShortCode(),
  val text: String,
  val normalizedHash: String,
  val title: String? = null,
  val tags: List<String> = emptyList(),
  val categoryId: String? = null,
  val source: ClipSource = ClipSource.KEYBOARD,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val pinned: Boolean = false,
  val archived: Boolean = false,
  val sensitivity: SensitivityLevel = SensitivityLevel.NORMAL,
  val syncState: SyncState = SyncState.LOCAL_ONLY,
  val expiresAt: Long? = null
) {
  companion object {
    fun generateShortCode(): String {
      val randomChars = UUID.randomUUID().toString().replace("-", "").take(5).uppercase()
      return "CLP-$randomChars"
    }
  }
}

data class VaultNote(
  val id: String = UUID.randomUUID().toString(),
  val title: String,
  val content: String,
  val tags: List<String> = emptyList(),
  val sourceClipId: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val pinned: Boolean = false,
  val archived: Boolean = false,
  val expiresAt: Long? = null
)

data class TagItem(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val colorHex: String = "#0284C7",
  val createdAt: Long = System.currentTimeMillis()
)

sealed class CaptureResult {
  data class Success(val clip: ClipItem) : CaptureResult()
  data class Duplicate(val existingClip: ClipItem, val submittedText: String) : CaptureResult()
  data class Rejected(val reason: String) : CaptureResult()
}
