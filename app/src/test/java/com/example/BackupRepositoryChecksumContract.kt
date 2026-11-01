package com.example

import com.example.data.local.ClipDao
import com.example.data.local.ClipEntity
import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import com.example.data.repository.BackupRepository
import com.example.data.repository.ClipRepository
import com.example.data.repository.ImportResult
import com.example.data.repository.NoteRepository
import com.example.domain.model.ClipItem
import com.example.domain.model.VaultNote
import java.lang.reflect.Proxy
import java.util.Locale
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Shared contract: Robolectric uses Android JSON; the standalone runner uses Android-derived JSON. */
abstract class BackupRepositoryChecksumContract {
  private lateinit var repository: BackupRepository
  private var clipSnapshotReads = 0

  private val clips = listOf(
    ClipItem(id = "clip-1", shortCode = "CLP-00001", text = "বাংলা লেখা 🐟\n\"quoted\"", normalizedHash = "hash-1", tags = listOf("বাংলা", "test")),
    ClipItem(id = "clip-2", shortCode = "CLP-00002", text = "https://example.com/a?b=1", normalizedHash = "hash-2")
  )
  private val notes = listOf(
    VaultNote(id = "note-1", title = "নোট", content = "Content\\path\nsecond line", sourceClipId = "clip-1")
  )

  @Before
  fun setUp() {
    clipSnapshotReads = 0
    // Only reads are supported. Any attempted write fails the test immediately.
    val clipDao = Proxy.newProxyInstance(ClipDao::class.java.classLoader, arrayOf(ClipDao::class.java)) { _, method, _ ->
      when (method.name) {
        "getAllClipsSnapshot" -> {
          clipSnapshotReads++
          clips.map(ClipEntity::fromDomain)
        }
        "getClipCount" -> flowOf(clips.size)
        "getAllClips", "getActiveClips", "getArchivedClips", "getPinnedClips" -> flowOf(emptyList<ClipEntity>())
        else -> throw AssertionError("Unexpected DAO call during preview: ${method.name}")
      }
    } as ClipDao
    val noteDao = Proxy.newProxyInstance(NoteDao::class.java.classLoader, arrayOf(NoteDao::class.java)) { _, method, _ ->
      when (method.name) {
        "getAllNotesSnapshot" -> notes.map(NoteEntity::fromDomain)
        "getNoteCount" -> flowOf(notes.size)
        "getActiveNotes", "getArchivedNotes" -> flowOf(emptyList<NoteEntity>())
        else -> throw AssertionError("Unexpected DAO call during preview: ${method.name}")
      }
    } as NoteDao
    repository = BackupRepository(ClipRepository(clipDao), NoteRepository(noteDao))
  }

  private fun exported(): JSONObject = runBlocking { JSONObject(repository.exportToJsonString()) }

  private fun preview(root: JSONObject): ImportResult = runBlocking { repository.validateAndPreview(root.toString()) }

  private fun assertRejectedBeforeDatabaseRead(root: JSONObject) {
    clipSnapshotReads = 0
    assertTrue(preview(root) is ImportResult.Error)
    assertEquals("Invalid backups must fail before reading database contents", 0, clipSnapshotReads)
  }

  @Test
  fun exportedBackupRoundTripsWithoutWriting() = runBlocking {
    val result = repository.validateAndPreview(repository.exportToJsonString())
    assertTrue(result is ImportResult.Preview)
    result as ImportResult.Preview
    assertEquals(clips, result.validClips)
    assertEquals(notes, result.validNotes)
    assertEquals(2, result.duplicateClipsCount)
    assertTrue(result.manifest.checksum.matches(Regex("[0-9a-f]{16}")))
  }

  @Test
  fun changedClipTextIsRejected() {
    val root = exported()
    root.getJSONArray("clips").getJSONObject(0).put("text", "changed")
    assertRejectedBeforeDatabaseRead(root)
  }

  @Test
  fun changedNoteContentIsRejected() {
    val root = exported()
    root.getJSONArray("notes").getJSONObject(0).put("content", "changed")
    assertRejectedBeforeDatabaseRead(root)
  }

  @Test
  fun changedTagsAreRejected() {
    val root = exported()
    root.getJSONArray("clips").getJSONObject(0).getJSONArray("tags").put("changed")
    assertRejectedBeforeDatabaseRead(root)
  }

  @Test
  fun removedClipIsRejected() {
    val root = exported()
    root.put("clips", JSONArray())
    assertRejectedBeforeDatabaseRead(root)
  }

  @Test
  fun legacyBackupWithoutChecksumStillPreviews() {
    val root = exported()
    root.remove("checksum")
    val result = preview(root)
    assertTrue(result is ImportResult.Preview)
    assertEquals(clips, (result as ImportResult.Preview).validClips)
    assertEquals(notes, result.validNotes)
  }

  @Test
  fun malformedChecksumsAreRejected() {
    for (checksum in listOf<Any>("", " ", "not-a-checksum", "0123456789abcdef0", JSONObject.NULL, 123, true, JSONArray(), JSONObject())) {
      assertRejectedBeforeDatabaseRead(exported().put("checksum", checksum))
    }
  }

  @Test
  fun hexadecimalChecksumCaseDoesNotMatter() {
    val root = exported()
    root.put("checksum", root.getString("checksum").uppercase(Locale.ROOT))
    assertTrue(preview(root) is ImportResult.Preview)
  }

  @Test
  fun indentationAndOuterWhitespaceDoNotMatter() = runBlocking {
    assertTrue(repository.validateAndPreview("\n  ${exported().toString(4)}\t") is ImportResult.Preview)
  }

  @Test
  fun emptyAndMalformedJsonAreRejected() = runBlocking {
    for (value in listOf("", "   ", "{", "[]", "null")) {
      assertTrue(repository.validateAndPreview(value) is ImportResult.Error)
    }
  }

  @Test
  fun unsupportedSchemaIsRejected() {
    assertRejectedBeforeDatabaseRead(exported().put("schemaVersion", 2))
  }

  @Test
  fun correctlyChecksummedEmptyBackupStillPreviews() {
    // Known vector: first 8 bytes of SHA-256 of UTF-8 "[][]".
    val root = JSONObject().put("clips", JSONArray()).put("notes", JSONArray()).put("checksum", "821bf06b4dcb406e")
    val result = preview(root)
    assertTrue(result is ImportResult.Preview)
    assertTrue((result as ImportResult.Preview).validClips.isEmpty())
    assertTrue(result.validNotes.isEmpty())
  }

  @Test
  fun checksummedBackupRequiresBothPayloadArrays() {
    for (missing in listOf("clips", "notes")) {
      val root = JSONObject().put("clips", JSONArray()).put("notes", JSONArray()).put("checksum", "821bf06b4dcb406e")
      root.remove(missing)
      assertRejectedBeforeDatabaseRead(root)
    }
  }

  @Test
  fun wrongPayloadTypesCannotMasqueradeAsEmptyArrays() {
    for (field in listOf("clips", "notes")) {
      for (value in listOf<Any>(JSONObject.NULL, "[]", JSONObject(), 42, true)) {
        val root = JSONObject().put("clips", JSONArray()).put("notes", JSONArray()).put("checksum", "821bf06b4dcb406e")
        root.put(field, value)
        assertRejectedBeforeDatabaseRead(root)
      }
    }
  }

  @Test
  fun malformedLegacyPayloadTypesAreRejected() {
    assertRejectedBeforeDatabaseRead(JSONObject().put("clips", "not an array"))
    assertRejectedBeforeDatabaseRead(JSONObject().put("notes", JSONObject.NULL))
  }
}
