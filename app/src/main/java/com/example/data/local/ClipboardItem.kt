package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "clipboard_items")
data class ClipboardItem(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  val content: String,
  val timestamp: Long = System.currentTimeMillis(),
  val category: String = "General"
) {
  val formattedTimestamp: String
    get() {
      val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
      return sdf.format(Date(timestamp))
    }
}
