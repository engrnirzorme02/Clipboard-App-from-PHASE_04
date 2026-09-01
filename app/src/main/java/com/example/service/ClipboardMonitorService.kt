package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.repository.ClipRepository
import com.example.domain.model.CaptureResult
import com.example.domain.model.ClipSource
import com.example.domain.usecase.CaptureClipUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ClipboardMonitorService : Service() {

  private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private var clipboardManager: ClipboardManager? = null
  private lateinit var captureUseCase: CaptureClipUseCase
  private var lastCopiedText: String? = null
  private var lastCopiedTimestamp: Long = 0L

  private val clipChangedListener = ClipboardManager.OnPrimaryClipChangedListener {
    processCurrentClipboard()
  }

  override fun onCreate() {
    super.onCreate()
    val db = AppDatabase.getDatabase(applicationContext)
    val clipRepo = ClipRepository(db.clipDao())
    captureUseCase = CaptureClipUseCase(clipRepo)
    clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    createNotificationChannel()
    startForeground(NOTIFICATION_ID, buildForegroundNotification())

    clipboardManager?.addPrimaryClipChangedListener(clipChangedListener)
    isServiceRunning = true
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
      }
      else -> {
        // Initial check on service start
        processCurrentClipboard()
      }
    }
    return START_STICKY
  }

  private fun processCurrentClipboard() {
    val cm = clipboardManager ?: return
    if (!cm.hasPrimaryClip()) return

    val primaryClip: ClipData = cm.primaryClip ?: return
    if (primaryClip.itemCount == 0) return

    val item = primaryClip.getItemAt(0)
    val text = item.coerceToText(this)?.toString()?.trim()

    if (!text.isNullOrBlank()) {
      val now = System.currentTimeMillis()
      // Cooldown & deduplication: Avoid re-capturing if identical text was captured within 2 seconds
      if (text != lastCopiedText || (now - lastCopiedTimestamp) > 2000L) {
        lastCopiedText = text
        lastCopiedTimestamp = now

        serviceScope.launch {
          captureUseCase.execute(
            rawText = text,
            source = ClipSource.CLIPBOARD,
            allowDuplicate = false
          )
        }
      }
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Clipboard Vault Monitor",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Background service monitoring clipboard entries"
        setShowBadge(false)
      }
      val manager = getSystemService(NotificationManager::class.java)
      manager?.createNotificationChannel(channel)
    }
  }

  private fun buildForegroundNotification(): Notification {
    val intent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      intent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val stopIntent = Intent(this, ClipboardMonitorService::class.java).apply {
      action = ACTION_STOP
    }
    val stopPendingIntent = PendingIntent.getService(
      this,
      1,
      stopIntent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Clipboard Monitor Active")
      .setContentText("Saving copied text automatically to local Vault")
      .setSmallIcon(android.R.drawable.ic_menu_save)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setContentIntent(pendingIntent)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Monitoring", stopPendingIntent)
      .build()
  }

  override fun onDestroy() {
    clipboardManager?.removePrimaryClipChangedListener(clipChangedListener)
    serviceScope.cancel()
    isServiceRunning = false
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  companion object {
    const val CHANNEL_ID = "clipboard_monitor_channel"
    const val NOTIFICATION_ID = 9012
    const val ACTION_START = "com.example.action.START_MONITOR"
    const val ACTION_STOP = "com.example.action.STOP_MONITOR"

    var isServiceRunning: Boolean = false
      private set

    fun start(context: Context) {
      val intent = Intent(context, ClipboardMonitorService::class.java).apply {
        action = ACTION_START
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stop(context: Context) {
      val intent = Intent(context, ClipboardMonitorService::class.java).apply {
        action = ACTION_STOP
      }
      context.startService(intent)
    }
  }
}
