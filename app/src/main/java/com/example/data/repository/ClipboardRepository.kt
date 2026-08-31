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

  suspend fun delete(item: ClipboardItem) = clipboardDao.deleteClipboardItem(item)

  suspend fun deleteById(id: Long) = clipboardDao.deleteClipboardItemById(id)

  suspend fun clearAll() = clipboardDao.clearAllClipboardItems()
}
