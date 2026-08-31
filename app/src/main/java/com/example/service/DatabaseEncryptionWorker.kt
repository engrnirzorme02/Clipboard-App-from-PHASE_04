package com.example.service

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class DatabaseEncryptionWorker(
  private val appContext: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

  companion object {
    const val TAG = "DatabaseEncryptionWorker"
    const val WORK_NAME_PERIODIC = "periodic_encrypted_vault_backup"
    const val WORK_NAME_ONE_TIME = "onetime_encrypted_vault_backup"
    const val PREFS_NAME = "encrypted_backup_prefs"
    const val KEY_LAST_BACKUP_TIME = "last_backup_time"
    const val KEY_LAST_BACKUP_SIZE = "last_backup_size"
    const val KEY_LAST_BACKUP_PATH = "last_backup_path"
    const val KEY_LAST_BACKUP_STATUS = "last_backup_status"
    const val KEY_LAST_ITEMS_COUNT = "last_items_count"
    const val KEY_CUSTOM_EXPORT_DIR = "custom_export_dir"
    const val KEY_BACKUP_INTERVAL_HOURS = "backup_interval_hours"
    const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"

    // Default 256-bit AES key derivation secret
    private const val DEFAULT_PASSPHRASE = "ClipboardVault#SecureRoomDbEncryptionKey#2026"

    fun deriveAesKey(passphrase: String = DEFAULT_PASSPHRASE): SecretKeySpec {
      val sha = MessageDigest.getInstance("SHA-256")
      val keyBytes = sha.digest(passphrase.toByteArray(StandardCharsets.UTF_8))
      return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptBytes(data: ByteArray, secretKey: SecretKeySpec): ByteArray {
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      val iv = ByteArray(12) // 96-bit IV for GCM
      SecureRandom().nextBytes(iv)
      val spec = GCMParameterSpec(128, iv)
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
      val cipherText = cipher.doFinal(data)

      // Prefix IV to cipherText
      val combined = ByteArray(iv.size + cipherText.size)
      System.arraycopy(iv, 0, combined, 0, iv.size)
      System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
      return combined
    }

    fun decryptBytes(combined: ByteArray, secretKey: SecretKeySpec): ByteArray {
      val iv = ByteArray(12)
      System.arraycopy(combined, 0, iv, 0, 12)
      val cipherText = ByteArray(combined.size - 12)
      System.arraycopy(combined, 12, cipherText, 0, cipherText.size)

      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      val spec = GCMParameterSpec(128, iv)
      cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
      return cipher.doFinal(cipherText)
    }
  }

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    try {
      Log.i(TAG, "Starting periodic encrypted Room database export...")

      val db = AppDatabase.getDatabase(appContext)
      val clipboardItems = db.clipboardDao().getAllClipboardItemsSnapshot()
      val clips = db.clipDao().getAllClipsSnapshot()
      val notes = db.noteDao().getAllNotesSnapshot()
      val tags = db.tagDao().getAllTagsSnapshot()

      val rootJson = JSONObject()
      rootJson.put("version", 2)
      rootJson.put("format", "ROOM_DATABASE_ENCRYPTED_VAULT_BACKUP")
      rootJson.put("timestamp", System.currentTimeMillis())
      rootJson.put("formattedDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
      rootJson.put("totalClipboardItems", clipboardItems.size)
      rootJson.put("totalClips", clips.size)
      rootJson.put("totalNotes", notes.size)
      rootJson.put("totalTags", tags.size)

      // 1. Clipboard Items table
      val clipboardArr = JSONArray()
      clipboardItems.forEach { item ->
        val obj = JSONObject().apply {
          put("id", item.id)
          put("content", item.content)
          put("timestamp", item.timestamp)
          put("category", item.category)
          put("isPinned", item.isPinned)
          put("tags", JSONArray(item.tags))
        }
        clipboardArr.put(obj)
      }
      rootJson.put("clipboard_items", clipboardArr)

      // 2. Clips table
      val clipsArr = JSONArray()
      clips.forEach { clip ->
        val obj = JSONObject().apply {
          put("id", clip.id)
          put("shortCode", clip.shortCode)
          put("text", clip.text)
          put("normalizedHash", clip.normalizedHash)
          put("title", clip.title ?: JSONObject.NULL)
          put("tags", JSONArray(clip.tags))
          put("source", clip.source.name)
          put("createdAt", clip.createdAt)
          put("updatedAt", clip.updatedAt)
          put("pinned", clip.pinned)
          put("archived", clip.archived)
          put("sensitivity", clip.sensitivity.name)
        }
        clipsArr.put(obj)
      }
      rootJson.put("clips", clipsArr)

      // 3. Notes table
      val notesArr = JSONArray()
      notes.forEach { note ->
        val obj = JSONObject().apply {
          put("id", note.id)
          put("title", note.title)
          put("content", note.content)
          put("tags", JSONArray(note.tags))
          put("sourceClipId", note.sourceClipId ?: JSONObject.NULL)
          put("createdAt", note.createdAt)
          put("updatedAt", note.updatedAt)
          put("pinned", note.pinned)
          put("archived", note.archived)
        }
        notesArr.put(obj)
      }
      rootJson.put("notes", notesArr)

      // 4. Tags table
      val tagsArr = JSONArray()
      tags.forEach { tag ->
        val obj = JSONObject().apply {
          put("id", tag.id)
          put("name", tag.name)
          put("colorHex", tag.colorHex)
          put("createdAt", tag.createdAt)
        }
        tagsArr.put(obj)
      }
      rootJson.put("tags", tagsArr)

      val rawJsonBytes = rootJson.toString().toByteArray(StandardCharsets.UTF_8)

      // Encrypt raw payload with AES-256 GCM
      val secretKey = deriveAesKey()
      val encryptedPayload = encryptBytes(rawJsonBytes, secretKey)

      // Determine secure export location
      val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val customDir = prefs.getString(KEY_CUSTOM_EXPORT_DIR, null)

      val exportDir = if (!customDir.isNullOrBlank()) {
        File(customDir).apply { if (!exists()) mkdirs() }
      } else {
        File(appContext.filesDir, "secure_vault_backups").apply { if (!exists()) mkdirs() }
      }

      val fileName = "encrypted_vault_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.enc"
      val outputFile = File(exportDir, fileName)

      FileOutputStream(outputFile).use { fos ->
        fos.write(encryptedPayload)
        fos.flush()
      }

      // Also maintain latest copy as latest_encrypted_vault.enc
      val latestFile = File(exportDir, "latest_encrypted_vault.enc")
      FileOutputStream(latestFile).use { fos ->
        fos.write(encryptedPayload)
        fos.flush()
      }

      val totalItems = clipboardItems.size + clips.size + notes.size
      val fileSizeKb = outputFile.length() / 1024.0

      // Update Shared Preferences for UI status
      prefs.edit()
        .putLong(KEY_LAST_BACKUP_TIME, System.currentTimeMillis())
        .putString(KEY_LAST_BACKUP_PATH, outputFile.absolutePath)
        .putString(KEY_LAST_BACKUP_SIZE, String.format(Locale.getDefault(), "%.1f KB", fileSizeKb))
        .putString(KEY_LAST_BACKUP_STATUS, "SUCCESS (Encrypted AES-256)")
        .putInt(KEY_LAST_ITEMS_COUNT, totalItems)
        .apply()

      Log.i(TAG, "Successfully exported encrypted backup to: ${outputFile.absolutePath} ($totalItems items, ${fileSizeKb}KB)")
      Result.success()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to encrypt and export Room database", e)
      val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit()
        .putString(KEY_LAST_BACKUP_STATUS, "FAILED: ${e.localizedMessage ?: "Unknown error"}")
        .apply()
      Result.retry()
    }
  }
}
