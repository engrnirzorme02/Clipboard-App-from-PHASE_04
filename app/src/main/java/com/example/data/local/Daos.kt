package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
  @Query("SELECT * FROM clips ORDER BY pinned DESC, createdAt DESC")
  fun getAllClips(): Flow<List<ClipEntity>>

  @Query("SELECT * FROM clips WHERE archived = 0 ORDER BY pinned DESC, createdAt DESC")
  fun getActiveClips(): Flow<List<ClipEntity>>

  @Query("SELECT * FROM clips WHERE archived = 1 ORDER BY createdAt DESC")
  fun getArchivedClips(): Flow<List<ClipEntity>>

  @Query("SELECT * FROM clips WHERE pinned = 1 AND archived = 0 ORDER BY createdAt DESC")
  fun getPinnedClips(): Flow<List<ClipEntity>>

  @Query("SELECT * FROM clips WHERE id = :id LIMIT 1")
  fun getClipById(id: String): Flow<ClipEntity?>

  @Query("SELECT * FROM clips WHERE normalizedHash = :hash LIMIT 1")
  suspend fun findByNormalizedHash(hash: String): ClipEntity?

  @Query("SELECT * FROM clips WHERE shortCode = :shortCode LIMIT 1")
  suspend fun findByShortCode(shortCode: String): ClipEntity?

  @Query("SELECT * FROM clips WHERE text LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR shortCode LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY pinned DESC, createdAt DESC")
  fun searchClips(query: String): Flow<List<ClipEntity>>

  @Query("SELECT COUNT(*) FROM clips")
  fun getClipCount(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertClip(clip: ClipEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertClips(clips: List<ClipEntity>)

  @Update
  suspend fun updateClip(clip: ClipEntity)

  @Query("DELETE FROM clips WHERE id = :id")
  suspend fun deleteClipById(id: String)

  @Query("UPDATE clips SET pinned = :pinned WHERE id = :id")
  suspend fun setPinned(id: String, pinned: Boolean)

  @Query("UPDATE clips SET archived = :archived WHERE id = :id")
  suspend fun setArchived(id: String, archived: Boolean)

  @Query("UPDATE clips SET syncState = :syncState WHERE id = :id")
  suspend fun updateSyncState(id: String, syncState: com.example.domain.model.SyncState)

  @Query("SELECT * FROM clips")
  suspend fun getAllClipsSnapshot(): List<ClipEntity>

  @Query("DELETE FROM clips")
  suspend fun clearAllClips()
}

@Dao
interface NoteDao {
  @Query("SELECT * FROM notes WHERE archived = 0 ORDER BY pinned DESC, updatedAt DESC")
  fun getActiveNotes(): Flow<List<NoteEntity>>

  @Query("SELECT * FROM notes WHERE archived = 1 ORDER BY updatedAt DESC")
  fun getArchivedNotes(): Flow<List<NoteEntity>>

  @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
  fun getNoteById(id: String): Flow<NoteEntity?>

  @Query("SELECT COUNT(*) FROM notes")
  fun getNoteCount(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNote(note: NoteEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNotes(notes: List<NoteEntity>)

  @Update
  suspend fun updateNote(note: NoteEntity)

  @Query("DELETE FROM notes WHERE id = :id")
  suspend fun deleteNoteById(id: String)

  @Query("SELECT * FROM notes")
  suspend fun getAllNotesSnapshot(): List<NoteEntity>

  @Query("DELETE FROM notes")
  suspend fun clearAllNotes()
}

@Dao
interface TagDao {
  @Query("SELECT * FROM tags ORDER BY name ASC")
  fun getAllTags(): Flow<List<TagEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTag(tag: TagEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTags(tags: List<TagEntity>)

  @Query("DELETE FROM tags WHERE id = :id")
  suspend fun deleteTagById(id: String)

  @Query("SELECT * FROM tags")
  suspend fun getAllTagsSnapshot(): List<TagEntity>

  @Query("DELETE FROM tags")
  suspend fun clearAllTags()
}
