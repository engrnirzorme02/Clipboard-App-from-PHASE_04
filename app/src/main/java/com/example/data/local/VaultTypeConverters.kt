package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.ClipSource
import com.example.domain.model.SensitivityLevel
import com.example.domain.model.SyncState

class VaultTypeConverters {

  @TypeConverter
  fun fromStringList(value: List<String>?): String {
    if (value.isNullOrEmpty()) return ""
    return value.joinToString("|||")
  }

  @TypeConverter
  fun toStringList(value: String?): List<String> {
    if (value.isNullOrEmpty()) return emptyList()
    return value.split("|||").filter { it.isNotEmpty() }
  }

  @TypeConverter
  fun fromClipSource(source: ClipSource): String = source.name

  @TypeConverter
  fun toClipSource(value: String?): ClipSource {
    return try {
      if (value != null) ClipSource.valueOf(value) else ClipSource.KEYBOARD
    } catch (_: Exception) {
      ClipSource.KEYBOARD
    }
  }

  @TypeConverter
  fun fromSensitivity(level: SensitivityLevel): String = level.name

  @TypeConverter
  fun toSensitivity(value: String?): SensitivityLevel {
    return try {
      if (value != null) SensitivityLevel.valueOf(value) else SensitivityLevel.NORMAL
    } catch (_: Exception) {
      SensitivityLevel.NORMAL
    }
  }

  @TypeConverter
  fun fromSyncState(state: SyncState): String = state.name

  @TypeConverter
  fun toSyncState(value: String?): SyncState {
    return try {
      if (value != null) SyncState.valueOf(value) else SyncState.LOCAL_ONLY
    } catch (_: Exception) {
      SyncState.LOCAL_ONLY
    }
  }
}
