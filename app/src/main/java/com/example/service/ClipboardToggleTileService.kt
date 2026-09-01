package com.example.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class ClipboardToggleTileService : TileService() {

  override fun onStartListening() {
    super.onStartListening()
    updateTileState()
  }

  override fun onClick() {
    super.onClick()
    if (ClipboardMonitorService.isServiceRunning) {
      ClipboardMonitorService.stop(this)
    } else {
      ClipboardMonitorService.start(this)
    }
    // Update tile after a short delay since service start/stop is async
    qsTile?.state = Tile.STATE_UNAVAILABLE
    qsTile?.updateTile()
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
      updateTileState()
    }, 500)
  }

  private fun updateTileState() {
    val tile = qsTile ?: return
    if (ClipboardMonitorService.isServiceRunning) {
      tile.state = Tile.STATE_ACTIVE
      tile.label = "Auto-Capture On"
    } else {
      tile.state = Tile.STATE_INACTIVE
      tile.label = "Auto-Capture Off"
    }
    tile.updateTile()
  }
}
