package com.example.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
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

class ClipboardObserver(
  private val context: Context,
  private val onClipCaptured: ((String) -> Unit)? = null
) : DefaultLifecycleObserver {

  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private var clipboardManager: ClipboardManager? = null
  private val captureUseCase: CaptureClipUseCase

  private var lastProcessedText: String? = null
  private var lastTimestamp: Long = 0L

  private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
    checkAndSavePrimaryClip()
  }

  init {
    val db = AppDatabase.getDatabase(context.applicationContext)
    val clipRepo = ClipRepository(db.clipDao())
    captureUseCase = CaptureClipUseCase(clipRepo)
    clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
  }

  override fun onResume(owner: LifecycleOwner) {
    clipboardManager?.addPrimaryClipChangedListener(clipListener)
    // Check if new content was copied while app was in background
    checkAndSavePrimaryClip()
  }

  override fun onPause(owner: LifecycleOwner) {
    clipboardManager?.removePrimaryClipChangedListener(clipListener)
  }

  override fun onDestroy(owner: LifecycleOwner) {
    scope.cancel()
  }

  private fun checkAndSavePrimaryClip() {
    val cm = clipboardManager ?: return
    if (!cm.hasPrimaryClip()) return

    val clip: ClipData = cm.primaryClip ?: return
    if (clip.itemCount == 0) return

    val text = clip.getItemAt(0).coerceToText(context)?.toString()?.trim()
    if (!text.isNullOrBlank()) {
      val now = System.currentTimeMillis()
      if (text != lastProcessedText || (now - lastTimestamp) > 1500L) {
        lastProcessedText = text
        lastTimestamp = now

        scope.launch {
          val result = captureUseCase.execute(
            rawText = text,
            source = ClipSource.CLIPBOARD,
            allowDuplicate = false
          )
          if (result is CaptureResult.Success) {
            onClipCaptured?.invoke(result.clip.text)
          }
        }
      }
    }
  }
}
