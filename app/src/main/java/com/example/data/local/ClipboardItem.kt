package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

@Entity(tableName = "clipboard_items")
data class ClipboardItem(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  val content: String,
  val timestamp: Long = System.currentTimeMillis(),
  val category: String = "General",
  val isPinned: Boolean = false,
  val tags: List<String> = emptyList()
) {
  val formattedTimestamp: String
    get() {
      val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
      return sdf.format(Date(timestamp))
    }

  companion object {
    private val URL_PATTERN = Pattern.compile(
      "^(https?|ftp)://[^\n\\s]+$",
      Pattern.CASE_INSENSITIVE
    )
    private val EMAIL_PATTERN = Pattern.compile(
      "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
      Pattern.CASE_INSENSITIVE
    )

    fun parseTags(input: String): List<String> {
      if (input.isBlank()) return emptyList()
      return input
        .split(",", " ", "#", ";")
        .map { it.trim().lowercase().removePrefix("#") }
        .filter { it.isNotBlank() }
        .distinct()
    }

    fun inferCategory(text: String): String {
      val trimmed = text.trim()
      return when {
        trimmed.isBlank() -> "General"
        URL_PATTERN.matcher(trimmed).matches() || trimmed.startsWith("http://") || trimmed.startsWith("https://") -> "Link"
        EMAIL_PATTERN.matcher(trimmed).matches() -> "Contact"
        trimmed.startsWith("{") && trimmed.endsWith("}") || trimmed.startsWith("[") && trimmed.endsWith("]") -> "Code"
        trimmed.contains("fun ") || trimmed.contains("val ") || trimmed.contains("const ") ||
          trimmed.contains("import ") || trimmed.contains("class ") || trimmed.contains("SELECT ") ||
          trimmed.contains("git ") || trimmed.contains("def ") -> "Code"
        trimmed.length in 16..64 && !trimmed.contains(" ") && (trimmed.contains("_") || trimmed.contains("-") || trimmed.any { it.isDigit() }) -> "Password"
        trimmed.lines().size > 2 || trimmed.length > 120 -> "Note"
        else -> "General"
      }
    }
  }
}

