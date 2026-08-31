package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
  entities = [
    ClipEntity::class,
    NoteEntity::class,
    TagEntity::class,
    ClipboardItem::class
  ],
  version = 1,
  exportSchema = false
)
@TypeConverters(VaultTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun clipDao(): ClipDao
  abstract fun noteDao(): NoteDao
  abstract fun tagDao(): TagDao
  abstract fun clipboardDao(): ClipboardDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "clipboard_vault.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
