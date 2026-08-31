package com.example.data.repository

import com.example.data.local.ClipboardDao
import com.example.data.local.ClipboardItem
import kotlinx.coroutines.flow.Flow

class ClipboardRepository(private val clipboardDao: ClipboardDao) {
  val allClipboardItems: Flow<List<ClipboardItem>> = clipboardDao.getAllClipboardItems()
  val allCategories: Flow<List<String>> = clipboardDao.getAllCategories()

  fun searchItems(query: String): Flow<List<ClipboardItem>> = clipboardDao.searchClipboardItems(query)

  fun getItemsByCategory(category: String): Flow<List<ClipboardItem>> = clipboardDao.getClipboardItemsByCategory(category)

  fun getItemById(id: Long): Flow<ClipboardItem?> = clipboardDao.getClipboardItemById(id)

  suspend fun insert(item: ClipboardItem): Long = clipboardDao.insertClipboardItem(item)

  suspend fun insertAll(items: List<ClipboardItem>) = clipboardDao.insertClipboardItems(items)

  suspend fun update(item: ClipboardItem) = clipboardDao.updateClipboardItem(item)

  suspend fun togglePin(item: ClipboardItem) {
    clipboardDao.updatePinStatus(item.id, !item.isPinned)
  }

  suspend fun captureNewClipboardText(text: String, customCategory: String? = null): ClipboardItem {
    val trimmed = text.trim()
    val category = customCategory ?: ClipboardItem.inferCategory(trimmed)
    val existing = clipboardDao.getItemByContent(trimmed)
    return if (existing != null) {
      val updated = existing.copy(timestamp = System.currentTimeMillis())
      clipboardDao.updateClipboardItem(updated)
      updated
    } else {
      val newItem = ClipboardItem(
        content = trimmed,
        category = category,
        timestamp = System.currentTimeMillis(),
        isPinned = false
      )
      val newId = clipboardDao.insertClipboardItem(newItem)
      newItem.copy(id = newId)
    }
  }

  suspend fun delete(item: ClipboardItem) = clipboardDao.deleteClipboardItem(item)

  suspend fun deleteById(id: Long) = clipboardDao.deleteClipboardItemById(id)

  suspend fun clearAll() = clipboardDao.clearAllClipboardItems()
}
