package com.example.domain.usecase

import com.example.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

enum class AiMode(val label: String) {
    FAST("Fast (Flash Lite)"),
    THINK("Think (Reasoning)"),
    SEARCH("Search (Grounded)"),
    TTS("Voice (TTS)")
}

data class ChatMessage(
    val role: String,
    val text: String,
    val isError: Boolean = false,
    val audioBase64: String? = null
)

class AiAssistantService {
    suspend fun processText(text: String, taskType: AiTaskType, apiKey: String? = null): AiTransformResult = withContext(Dispatchers.IO) {
        if (!apiKey.isNullOrBlank()) {
            try {
                val prompt = "${taskType.promptPrefix}\n\n\"\"\"\n$text\n\"\"\""
                val request = GenerateContentRequest(
                    contents = listOf(Content(role = "user", parts = listOf(Part(text = prompt))))
                )
                val response = GeminiApiClient.service.generateContent("gemini-2.5-flash", apiKey, request)
                val geminiResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                
                if (geminiResponse.isNotBlank()) {
                    return@withContext AiTransformResult(
                        output = geminiResponse,
                        taskType = taskType,
                        isAiGenerated = true
                    )
                }
            } catch (e: Exception) {
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
    
    suspend fun generateChatResponse(
        history: List<ChatMessage>,
        prompt: String,
        mode: AiMode,
        contextText: String?,
        apiKey: String
    ): ChatMessage = withContext(Dispatchers.IO) {
        try {
            var model = "gemini-3.5-flash"
            var tools: List<Tool>? = null
            var generationConfig: GenerationConfig? = null
            
            when (mode) {
                AiMode.FAST -> model = "gemini-3.1-flash-lite"
                AiMode.THINK -> {
                    model = "gemini-3.1-pro-preview"
                    generationConfig = GenerationConfig(thinkingConfig = ThinkingConfig("HIGH"))
                }
                AiMode.SEARCH -> {
                    model = "gemini-3.5-flash"
                    tools = listOf(Tool(googleSearch = GoogleSearch()))
                }
                AiMode.TTS -> {
                    model = "gemini-3.1-flash-tts-preview"
                    generationConfig = GenerationConfig(
                        responseModalities = listOf("AUDIO"),
                        speechConfig = SpeechConfig(VoiceConfig(PrebuiltVoiceConfig("Kore")))
                    )
                }
            }
            
            val systemInstruction = Content(
                parts = listOf(Part(text = "You are Gemini AI Copilot. Answer in Bengali or English as appropriate."))
            )
            
            val contents = history.filter { !it.isError && it.audioBase64 == null }.map {
                Content(role = it.role, parts = listOf(Part(text = it.text)))
            }.toMutableList()
            
            var userText = prompt
            if (!contextText.isNullOrBlank()) {
                userText = "Context:\n\"\"\"\n$contextText\n\"\"\"\n\nUser Question:\n$prompt"
            }
            contents.add(Content(role = "user", parts = listOf(Part(text = userText))))
            
            val request = GenerateContentRequest(
                contents = contents,
                systemInstruction = systemInstruction,
                tools = tools,
                generationConfig = generationConfig
            )
            
            val response = GeminiApiClient.service.generateContent(model, apiKey, request)
            
            if (mode == AiMode.TTS) {
                val base64Audio = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.inlineData?.data
                if (base64Audio != null) {
                    return@withContext ChatMessage("model", "TTS Audio Generated \uD83C\uDFB5", audioBase64 = base64Audio)
                } else {
                    return@withContext ChatMessage("model", "Failed to generate TTS audio.", true)
                }
            } else {
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (responseText != null) {
                    return@withContext ChatMessage("model", responseText)
                } else {
                    return@withContext ChatMessage("model", "No response from Gemini.", true)
                }
            }
            
        } catch (e: Exception) {
            return@withContext ChatMessage("model", "Error: ${e.message}", true)
        }
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
}
