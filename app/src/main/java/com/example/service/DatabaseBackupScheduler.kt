package com.example.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class EncryptedBackupStatus(
  val isAutoBackupEnabled: Boolean = true,
  val intervalHours: Long = 6,
  val lastBackupTimestamp: Long = 0L,
  val formattedLastBackup: String = "Never",
  val lastBackupPath: String = "",
  val lastBackupSize: String = "0 KB",
  val lastStatus: String = "Idle",
  val totalItemsBackedUp: Int = 0,
  val customLocationPath: String = ""
)

object DatabaseBackupScheduler {

  private val _statusFlow = MutableStateFlow(EncryptedBackupStatus())
  val statusFlow: StateFlow<EncryptedBackupStatus> = _statusFlow.asStateFlow()

  fun refreshStatus(context: Context) {
    val prefs = context.getSharedPreferences(DatabaseEncryptionWorker.PREFS_NAME, Context.MODE_PRIVATE)
    val isAutoEnabled = prefs.getBoolean(DatabaseEncryptionWorker.KEY_AUTO_BACKUP_ENABLED, true)
    val intervalHours = prefs.getLong(DatabaseEncryptionWorker.KEY_BACKUP_INTERVAL_HOURS, 6)
    val lastTime = prefs.getLong(DatabaseEncryptionWorker.KEY_LAST_BACKUP_TIME, 0L)
    val lastPath = prefs.getString(DatabaseEncryptionWorker.KEY_LAST_BACKUP_PATH, "") ?: ""
    val lastSize = prefs.getString(DatabaseEncryptionWorker.KEY_LAST_BACKUP_SIZE, "0 KB") ?: "0 KB"
    val lastStatus = prefs.getString(DatabaseEncryptionWorker.KEY_LAST_BACKUP_STATUS, "Idle") ?: "Idle"
    val itemsCount = prefs.getInt(DatabaseEncryptionWorker.KEY_LAST_ITEMS_COUNT, 0)
    val customLoc = prefs.getString(DatabaseEncryptionWorker.KEY_CUSTOM_EXPORT_DIR, "") ?: ""

    val formattedTime = if (lastTime > 0) {
      SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(lastTime))
    } else {
      "Never"
    }

    _statusFlow.value = EncryptedBackupStatus(
      isAutoBackupEnabled = isAutoEnabled,
      intervalHours = intervalHours,
      lastBackupTimestamp = lastTime,
      formattedLastBackup = formattedTime,
      lastBackupPath = lastPath,
      lastBackupSize = lastSize,
      lastStatus = lastStatus,
      totalItemsBackedUp = itemsCount,
      customLocationPath = customLoc
    )
  }

  fun setAutoBackupEnabled(context: Context, enabled: Boolean, intervalHours: Long = 6) {
    val prefs = context.getSharedPreferences(DatabaseEncryptionWorker.PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
      .putBoolean(DatabaseEncryptionWorker.KEY_AUTO_BACKUP_ENABLED, enabled)
      .putLong(DatabaseEncryptionWorker.KEY_BACKUP_INTERVAL_HOURS, intervalHours)
      .apply()

    if (enabled) {
      schedulePeriodicEncryptedBackup(context, intervalHours)
    } else {
      cancelPeriodicBackup(context)
    }
    refreshStatus(context)
  }

  fun setCustomExportDirectory(context: Context, path: String) {
    val prefs = context.getSharedPreferences(DatabaseEncryptionWorker.PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
      .putString(DatabaseEncryptionWorker.KEY_CUSTOM_EXPORT_DIR, path.trim())
      .apply()
    refreshStatus(context)
  }

  fun schedulePeriodicEncryptedBackup(context: Context, intervalHours: Long = 6) {
    val constraints = Constraints.Builder()
      .setRequiresBatteryNotLow(true)
      .build()

    val periodicWorkRequest = PeriodicWorkRequestBuilder<DatabaseEncryptionWorker>(
      intervalHours,
      TimeUnit.HOURS
    )
      .setConstraints(constraints)
      .addTag(DatabaseEncryptionWorker.TAG)
      .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
      DatabaseEncryptionWorker.WORK_NAME_PERIODIC,
      ExistingPeriodicWorkPolicy.UPDATE,
      periodicWorkRequest
    )
  }

  fun cancelPeriodicBackup(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(DatabaseEncryptionWorker.WORK_NAME_PERIODIC)
  }

  fun triggerImmediateEncryptedBackup(context: Context) {
    val oneTimeRequest = OneTimeWorkRequestBuilder<DatabaseEncryptionWorker>()
      .addTag(DatabaseEncryptionWorker.TAG)
      .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
      DatabaseEncryptionWorker.WORK_NAME_ONE_TIME,
      ExistingWorkPolicy.REPLACE,
      oneTimeRequest
    )
  }
}
