package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {

  @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC")
  fun getAllClipboardItems(): Flow<List<ClipboardItem>>

  @Query("SELECT * FROM clipboard_items WHERE id = :id LIMIT 1")
  fun getClipboardItemById(id: Long): Flow<ClipboardItem?>

  @Query("""
    SELECT * FROM clipboard_items 
    WHERE content LIKE '%' || :query || '%' 
       OR category LIKE '%' || :query || '%' 
    ORDER BY timestamp DESC
  """)
  fun searchClipboardItems(query: String): Flow<List<ClipboardItem>>

  @Query("SELECT * FROM clipboard_items WHERE category = :category ORDER BY timestamp DESC")
  fun getClipboardItemsByCategory(category: String): Flow<List<ClipboardItem>>

  @Query("SELECT DISTINCT category FROM clipboard_items ORDER BY category ASC")
  fun getAllCategories(): Flow<List<String>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertClipboardItem(item: ClipboardItem): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertClipboardItems(items: List<ClipboardItem>)

  @Update
  suspend fun updateClipboardItem(item: ClipboardItem)

  @Delete
  suspend fun deleteClipboardItem(item: ClipboardItem)

  @Query("DELETE FROM clipboard_items WHERE id = :id")
  suspend fun deleteClipboardItemById(id: Long)

  @Query("DELETE FROM clipboard_items")
  suspend fun clearAllClipboardItems()
}
