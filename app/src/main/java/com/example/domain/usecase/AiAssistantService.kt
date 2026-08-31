package com.example.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

enum class AiTaskType(val label: String, val promptPrefix: String) {
  SUMMARIZE("Summarize", "Provide a concise bulleted summary of this text:"),
  ACTION_ITEMS("Action Items", "Extract clear checklist/action items from this text:"),
  CLEAN_FORMAT("Clean Format", "Clean and normalize the formatting of this text without changing meaning:"),
  EXPLAIN_CODE("Explain", "Explain what this snippet or text does simply and clearly:"),
  TRANSLATE("Translate to Bengali", "Translate this text into natural Bengali:")
}

data class AiTransformResult(
  val output: String,
  val taskType: AiTaskType,
  val isAiGenerated: Boolean
)

class AiAssistantService {

  suspend fun processText(text: String, taskType: AiTaskType, apiKey: String? = null): AiTransformResult = withContext(Dispatchers.IO) {
    if (!apiKey.isNullOrBlank()) {
      try {
        val geminiResponse = callGeminiApi(text, taskType, apiKey)
        if (geminiResponse.isNotBlank()) {
          return@withContext AiTransformResult(
            output = geminiResponse,
            taskType = taskType,
            isAiGenerated = true
          )
        }
      } catch (_: Exception) {
        // Fallback to local heuristic engine
      }
    }

    // Local smart processing engine
    val localResult = when (taskType) {
      AiTaskType.SUMMARIZE -> localSummarize(text)
      AiTaskType.ACTION_ITEMS -> localExtractActions(text)
      AiTaskType.CLEAN_FORMAT -> localCleanFormat(text)
      AiTaskType.EXPLAIN_CODE -> localExplain(text)
      AiTaskType.TRANSLATE -> localBengaliSummary(text)
    }

    AiTransformResult(
      output = localResult,
      taskType = taskType,
      isAiGenerated = false
    )
  }

  private fun localSummarize(text: String): String {
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.size <= 2) {
      return "• ${text.take(150)}${if (text.length > 150) "..." else ""}\n• Length: ${text.length} chars, ${text.split("\\s+".toRegex()).size} words."
    }
    val sb = StringBuilder()
    sb.append("📌 **Key Takeaways (${lines.size} segments)**:\n")
    lines.take(4).forEach { line ->
      sb.append("• ").append(line.take(100)).append("\n")
    }
    if (lines.size > 4) {
      sb.append("• ...and ${lines.size - 4} more items.")
    }
    return sb.toString().trim()
  }

  private fun localExtractActions(text: String): String {
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val sb = StringBuilder()
    sb.append("☑️ **Checklist Items**:\n")
    var count = 0
    lines.forEach { line ->
      if (line.startsWith("-") || line.startsWith("*") || line.startsWith("[") || line.contains("todo", ignoreCase = true) || line.contains("need to", ignoreCase = true)) {
        sb.append("[ ] ").append(line.removePrefix("-").removePrefix("*").trim()).append("\n")
        count++
      }
    }
    if (count == 0) {
      lines.take(5).forEachIndexed { i, line ->
        sb.append("[ ] Step ${i + 1}: ").append(line).append("\n")
      }
    }
    return sb.toString().trim()
  }

  private fun localCleanFormat(text: String): String {
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    return lines.joinToString("\n")
  }

  private fun localExplain(text: String): String {
    val words = text.split("\\s+".toRegex()).size
    val isUrl = text.startsWith("http://") || text.startsWith("https://")
    val isJson = (text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]"))
    val isCode = text.contains("fun ") || text.contains("class ") || text.contains("import ") || text.contains("const ")

    val type = when {
      isUrl -> "Web Link / URL"
      isJson -> "JSON Structured Payload"
      isCode -> "Source Code / Script Fragment"
      else -> "Plain Text Document"
    }

    return "💡 **Format Analysis**: $type\n• **Word Count**: $words words (${text.length} characters)\n• **Lines**: ${text.lines().size}\n• **Summary**: Captured securely in local vault."
  }

  private fun localBengaliSummary(text: String): String {
    return "📝 **ক্লিপবোর্ড বিবরণী**:\n• দৈর্ঘ্য: ${text.length} অক্ষর, ${text.lines().size} লাইন।\n• সংরক্ষিত তথ্য: ${text.take(80)}${if (text.length > 80) "..." else ""}\n• অবস্থা: লোকাল ভল্টে নিরাপদে সংরক্ষিত।"
  }

  private fun callGeminiApi(text: String, taskType: AiTaskType, apiKey: String): String {
    val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
    val url = URL(endpoint)
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
    conn.doOutput = true
    conn.connectTimeout = 8000
    conn.readTimeout = 8000

    val prompt = "${taskType.promptPrefix}\n\n\"\"\"\n$text\n\"\"\""
    val jsonBody = JSONObject().apply {
      put("contents", JSONArray().apply {
        put(JSONObject().apply {
          put("parts", JSONArray().apply {
            put(JSONObject().apply {
              put("text", prompt)
            })
          })
        })
      })
    }

    OutputStreamWriter(conn.outputStream).use { writer ->
      writer.write(jsonBody.toString())
      writer.flush()
    }

    if (conn.responseCode == 200) {
      val responseText = conn.inputStream.bufferedReader().use { it.readText() }
      val respJson = JSONObject(responseText)
      val candidates = respJson.optJSONArray("candidates")
      if (candidates != null && candidates.length() > 0) {
        val content = candidates.getJSONObject(0).optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        if (parts != null && parts.length() > 0) {
          return parts.getJSONObject(0).optString("text", "")
        }
      }
    }
    return ""
  }
}
