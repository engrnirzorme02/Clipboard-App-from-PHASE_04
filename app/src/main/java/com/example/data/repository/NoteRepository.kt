package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import com.example.domain.model.VaultNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepository(private val noteDao: NoteDao) {

  val activeNotes: Flow<List<VaultNote>> = noteDao.getActiveNotes().map { list ->
    list.map { it.toDomain() }
  }

  val archivedNotes: Flow<List<VaultNote>> = noteDao.getArchivedNotes().map { list ->
    list.map { it.toDomain() }
  }

  val noteCount: Flow<Int> = noteDao.getNoteCount()

  fun getNoteById(id: String): Flow<VaultNote?> = noteDao.getNoteById(id).map { it?.toDomain() }

  suspend fun insertNote(note: VaultNote): Long {
    return noteDao.insertNote(NoteEntity.fromDomain(note))
  }

  suspend fun insertNotes(notes: List<VaultNote>) {
    noteDao.insertNotes(notes.map { NoteEntity.fromDomain(it) })
  }

  suspend fun updateNote(note: VaultNote) {
    noteDao.updateNote(NoteEntity.fromDomain(note))
  }

  suspend fun deleteNote(id: String) {
    noteDao.deleteNoteById(id)
  }

  suspend fun togglePin(note: VaultNote) {
    val updated = note.copy(pinned = !note.pinned, updatedAt = System.currentTimeMillis())
    noteDao.updateNote(NoteEntity.fromDomain(updated))
  }

  suspend fun toggleArchive(note: VaultNote) {
    val updated = note.copy(archived = !note.archived, updatedAt = System.currentTimeMillis())
    noteDao.updateNote(NoteEntity.fromDomain(updated))
  }

  suspend fun getAllSnapshot(): List<VaultNote> {
    return noteDao.getAllNotesSnapshot().map { it.toDomain() }
  }

  suspend fun clearAll() {
    noteDao.clearAllNotes()
  }
}
