package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.ClipItem
import com.example.domain.model.ClipSource
import com.example.domain.model.SensitivityLevel
import com.example.domain.model.SyncState
import com.example.domain.model.TagItem
import com.example.domain.model.VaultNote

@Entity(
  tableName = "clips",
  indices = [
    Index(value = ["normalizedHash"]),
    Index(value = ["shortCode"], unique = true),
    Index(value = ["createdAt"]),
    Index(value = ["pinned"]),
    Index(value = ["archived"])
  ]
)
data class ClipEntity(
  @PrimaryKey val id: String,
  val shortCode: String,
  val text: String,
  val normalizedHash: String,
  val title: String?,
  val tags: List<String>,
  val categoryId: String?,
  val source: ClipSource,
  val createdAt: Long,
  val updatedAt: Long,
  val pinned: Boolean,
  val archived: Boolean,
  val sensitivity: SensitivityLevel,
  val syncState: SyncState,
  val expiresAt: Long?
) {
  fun toDomain(): ClipItem = ClipItem(
    id = id,
    shortCode = shortCode,
    text = text,
    normalizedHash = normalizedHash,
    title = title,
    tags = tags,
    categoryId = categoryId,
    source = source,
    createdAt = createdAt,
    updatedAt = updatedAt,
    pinned = pinned,
    archived = archived,
    sensitivity = sensitivity,
    syncState = syncState,
    expiresAt = expiresAt
  )

  companion object {
    fun fromDomain(domain: ClipItem): ClipEntity = ClipEntity(
      id = domain.id,
      shortCode = domain.shortCode,
      text = domain.text,
      normalizedHash = domain.normalizedHash,
      title = domain.title,
      tags = domain.tags,
      categoryId = domain.categoryId,
      source = domain.source,
      createdAt = domain.createdAt,
      updatedAt = domain.updatedAt,
      pinned = domain.pinned,
      archived = domain.archived,
      sensitivity = domain.sensitivity,
      syncState = domain.syncState,
      expiresAt = domain.expiresAt
    )
  }
}

@Entity(tableName = "notes")
data class NoteEntity(
  @PrimaryKey val id: String,
  val title: String,
  val content: String,
  val tags: List<String>,
  val sourceClipId: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val pinned: Boolean,
  val archived: Boolean,
  val expiresAt: Long?
) {
  fun toDomain(): VaultNote = VaultNote(
    id = id,
    title = title,
    content = content,
    tags = tags,
    sourceClipId = sourceClipId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    pinned = pinned,
    archived = archived,
    expiresAt = expiresAt
  )

  companion object {
    fun fromDomain(domain: VaultNote): NoteEntity = NoteEntity(
      id = domain.id,
      title = domain.title,
      content = domain.content,
      tags = domain.tags,
      sourceClipId = domain.sourceClipId,
      createdAt = domain.createdAt,
      updatedAt = domain.updatedAt,
      pinned = domain.pinned,
      archived = domain.archived,
      expiresAt = domain.expiresAt
    )
  }
}

@Entity(tableName = "tags")
data class TagEntity(
  @PrimaryKey val id: String,
  val name: String,
  val colorHex: String,
  val createdAt: Long
) {
  fun toDomain(): TagItem = TagItem(
    id = id,
    name = name,
    colorHex = colorHex,
    createdAt = createdAt
  )

  companion object {
    fun fromDomain(domain: TagItem): TagEntity = TagEntity(
      id = domain.id,
      name = domain.name,
      colorHex = domain.colorHex,
      createdAt = domain.createdAt
    )
  }
}
