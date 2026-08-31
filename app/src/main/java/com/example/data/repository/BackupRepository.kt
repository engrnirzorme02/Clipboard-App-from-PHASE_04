package com.example.data.repository

import com.example.domain.model.ClipItem
import com.example.domain.model.ClipSource
import com.example.domain.model.SensitivityLevel
import com.example.domain.model.SyncState
import com.example.domain.model.VaultNote
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

data class BackupManifest(
  val schemaVersion: Int = 1,
  val app: String = "Clipboard Vault",
  val exportedAt: Long = System.currentTimeMillis(),
  val clipCount: Int,
  val noteCount: Int,
  val checksum: String
)

sealed class ImportResult {
  data class Preview(
    val manifest: BackupManifest,
    val validClips: List<ClipItem>,
    val validNotes: List<VaultNote>,
    val duplicateClipsCount: Int
  ) : ImportResult()

  data class Success(val importedClips: Int, val importedNotes: Int) : ImportResult()
  data class Error(val message: String) : ImportResult()
}

enum class DuplicateStrategy {
  SKIP_DUPLICATES,
  OVERWRITE,
  KEEP_BOTH
}

class BackupRepository(
  private val clipRepository: ClipRepository,
  private val noteRepository: NoteRepository
) {

  suspend fun exportToJsonString(): String {
    val clips = clipRepository.getAllSnapshot()
    val notes = noteRepository.getAllSnapshot()

    val root = JSONObject()
    root.put("schemaVersion", 1)
    root.put("app", "Clipboard Vault")
    root.put("exportedAt", System.currentTimeMillis())
    root.put("clipCount", clips.size)
    root.put("noteCount", notes.size)

    val clipsArray = JSONArray()
    for (c in clips) {
      val clipObj = JSONObject().apply {
        put("id", c.id)
        put("shortCode", c.shortCode)
        put("text", c.text)
        put("normalizedHash", c.normalizedHash)
        put("title", c.title ?: JSONObject.NULL)
        put("tags", JSONArray(c.tags))
        put("source", c.source.name)
        put("createdAt", c.createdAt)
        put("updatedAt", c.updatedAt)
        put("pinned", c.pinned)
        put("archived", c.archived)
        put("sensitivity", c.sensitivity.name)
        put("syncState", c.syncState.name)
        put("expiresAt", c.expiresAt ?: JSONObject.NULL)
      }
      clipsArray.put(clipObj)
    }
    root.put("clips", clipsArray)

    val notesArray = JSONArray()
    for (n in notes) {
      val noteObj = JSONObject().apply {
        put("id", n.id)
        put("title", n.title)
        put("content", n.content)
        put("tags", JSONArray(n.tags))
        put("sourceClipId", n.sourceClipId ?: JSONObject.NULL)
        put("createdAt", n.createdAt)
        put("updatedAt", n.updatedAt)
        put("pinned", n.pinned)
        put("archived", n.archived)
        put("expiresAt", n.expiresAt ?: JSONObject.NULL)
      }
      notesArray.put(noteObj)
    }
    root.put("notes", notesArray)

    // Compute Checksum of the content
    val payloadString = clipsArray.toString() + notesArray.toString()
    val checksum = computeChecksum(payloadString)
    root.put("checksum", checksum)

    return root.toString(2)
  }

  suspend fun validateAndPreview(jsonContent: String): ImportResult {
    return try {
      val trimmed = jsonContent.trim()
      if (trimmed.isEmpty()) {
        return ImportResult.Error("JSON backup payload is empty.")
      }

      val root = JSONObject(trimmed)
      val schemaVersion = root.optInt("schemaVersion", 1)
      if (schemaVersion > 1) {
        return ImportResult.Error("Unsupported schema version ($schemaVersion). Please update the app.")
      }

      val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
      val clipsArray = root.optJSONArray("clips") ?: JSONArray()
      val notesArray = root.optJSONArray("notes") ?: JSONArray()

      val parsedClips = mutableListOf<ClipItem>()
      for (i in 0 until clipsArray.length()) {
        val obj = clipsArray.getJSONObject(i)
        val text = obj.getString("text")
        val tags = mutableListOf<String>()
        val tagsArr = obj.optJSONArray("tags")
        if (tagsArr != null) {
          for (t in 0 until tagsArr.length()) {
            tags.add(tagsArr.getString(t))
          }
        }

        val clip = ClipItem(
          id = obj.optString("id").takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString(),
          shortCode = obj.optString("shortCode").takeIf { it.isNotBlank() } ?: ClipItem.generateShortCode(),
          text = text,
          normalizedHash = obj.optString("normalizedHash").takeIf { it.isNotBlank() } ?: computeChecksum(text),
          title = if (obj.isNull("title")) null else obj.optString("title"),
          tags = tags,
          source = parseSource(obj.optString("source")),
          createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
          updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
          pinned = obj.optBoolean("pinned", false),
          archived = obj.optBoolean("archived", false),
          sensitivity = parseSensitivity(obj.optString("sensitivity")),
          syncState = SyncState.LOCAL_ONLY
        )
        parsedClips.add(clip)
      }

      val parsedNotes = mutableListOf<VaultNote>()
      for (i in 0 until notesArray.length()) {
        val obj = notesArray.getJSONObject(i)
        val tags = mutableListOf<String>()
        val tagsArr = obj.optJSONArray("tags")
        if (tagsArr != null) {
          for (t in 0 until tagsArr.length()) {
            tags.add(tagsArr.getString(t))
          }
        }

        val note = VaultNote(
          id = obj.optString("id").takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString(),
          title = obj.getString("title"),
          content = obj.getString("content"),
          tags = tags,
          sourceClipId = if (obj.isNull("sourceClipId")) null else obj.optString("sourceClipId"),
          createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
          updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
          pinned = obj.optBoolean("pinned", false),
          archived = obj.optBoolean("archived", false)
        )
        parsedNotes.add(note)
      }

      val existingClips = clipRepository.getAllSnapshot()
      val existingHashes = existingClips.map { it.normalizedHash }.toSet()
      val duplicatesCount = parsedClips.count { existingHashes.contains(it.normalizedHash) }

      val manifest = BackupManifest(
        schemaVersion = schemaVersion,
        exportedAt = exportedAt,
        clipCount = parsedClips.size,
        noteCount = parsedNotes.size,
        checksum = root.optString("checksum", "unknown")
      )

      ImportResult.Preview(
        manifest = manifest,
        validClips = parsedClips,
        validNotes = parsedNotes,
        duplicateClipsCount = duplicatesCount
      )
    } catch (e: Exception) {
      ImportResult.Error("Failed to parse JSON backup: ${e.localizedMessage ?: "Invalid format"}")
    }
  }

  suspend fun commitImport(
    clips: List<ClipItem>,
    notes: List<VaultNote>,
    strategy: DuplicateStrategy
  ): ImportResult {
    return try {
      val existingClips = clipRepository.getAllSnapshot()
      val existingMap = existingClips.associateBy { it.normalizedHash }

      val clipsToInsert = mutableListOf<ClipItem>()
      for (clip in clips) {
        val existing = existingMap[clip.normalizedHash]
        if (existing != null) {
          when (strategy) {
            DuplicateStrategy.SKIP_DUPLICATES -> {
              // skip
            }
            DuplicateStrategy.OVERWRITE -> {
              clipsToInsert.add(clip.copy(id = existing.id, shortCode = existing.shortCode))
            }
            DuplicateStrategy.KEEP_BOTH -> {
              clipsToInsert.add(clip.copy(id = java.util.UUID.randomUUID().toString(), shortCode = ClipItem.generateShortCode()))
            }
          }
        } else {
          clipsToInsert.add(clip)
        }
      }

      if (clipsToInsert.isNotEmpty()) {
        clipRepository.insertClips(clipsToInsert)
      }
      if (notes.isNotEmpty()) {
        noteRepository.insertNotes(notes)
      }

      ImportResult.Success(clipsToInsert.size, notes.size)
    } catch (e: Exception) {
      ImportResult.Error("Import failed: ${e.localizedMessage}")
    }
  }

  private fun parseSource(name: String?): ClipSource {
    return try {
      if (name != null) ClipSource.valueOf(name) else ClipSource.KEYBOARD
    } catch (_: Exception) {
      ClipSource.KEYBOARD
    }
  }

  private fun parseSensitivity(name: String?): SensitivityLevel {
    return try {
      if (name != null) SensitivityLevel.valueOf(name) else SensitivityLevel.NORMAL
    } catch (_: Exception) {
      SensitivityLevel.NORMAL
    }
  }

  private fun computeChecksum(data: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(data.toByteArray(Charsets.UTF_8))
    return digest.take(8).joinToString("") { "%02x".format(it) }
  }
}
