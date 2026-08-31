package com.example.data.repository

import com.example.data.local.ClipDao
import com.example.data.local.ClipEntity
import com.example.domain.model.ClipItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ClipRepository(private val clipDao: ClipDao) {

  val allClips: Flow<List<ClipItem>> = clipDao.getAllClips().map { list ->
    list.map { it.toDomain() }
  }

  val activeClips: Flow<List<ClipItem>> = clipDao.getActiveClips().map { list ->
    list.map { it.toDomain() }
  }

  val archivedClips: Flow<List<ClipItem>> = clipDao.getArchivedClips().map { list ->
    list.map { it.toDomain() }
  }

  val pinnedClips: Flow<List<ClipItem>> = clipDao.getPinnedClips().map { list ->
    list.map { it.toDomain() }
  }

  val clipCount: Flow<Int> = clipDao.getClipCount()

  fun getClipById(id: String): Flow<ClipItem?> = clipDao.getClipById(id).map { it?.toDomain() }

  suspend fun findByNormalizedHash(hash: String): ClipItem? {
    return clipDao.findByNormalizedHash(hash)?.toDomain()
  }

  fun searchClips(query: String): Flow<List<ClipItem>> = clipDao.searchClips(query).map { list ->
    list.map { it.toDomain() }
  }

  suspend fun insertClip(clip: ClipItem): Long {
    return clipDao.insertClip(ClipEntity.fromDomain(clip))
  }

  suspend fun insertClips(clips: List<ClipItem>) {
    clipDao.insertClips(clips.map { ClipEntity.fromDomain(it) })
  }

  suspend fun updateClip(clip: ClipItem) {
    clipDao.updateClip(ClipEntity.fromDomain(clip))
  }

  suspend fun deleteClip(id: String) {
    clipDao.deleteClipById(id)
  }

  suspend fun togglePin(id: String, currentPinned: Boolean) {
    clipDao.setPinned(id, !currentPinned)
  }

  suspend fun toggleArchive(id: String, currentArchived: Boolean) {
    clipDao.setArchived(id, !currentArchived)
  }

  suspend fun updateSyncState(id: String, syncState: com.example.domain.model.SyncState) {
    clipDao.updateSyncState(id, syncState)
  }

  suspend fun getAllSnapshot(): List<ClipItem> {
    return clipDao.getAllClipsSnapshot().map { it.toDomain() }
  }

  suspend fun clearAll() {
    clipDao.clearAllClips()
  }
}
