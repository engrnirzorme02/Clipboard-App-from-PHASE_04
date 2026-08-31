package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ClipSource
import com.example.ui.components.BottomVaultNavigation
import com.example.ui.components.TopVaultBar
import com.example.ui.screens.CaptureScreen
import com.example.ui.screens.DiagnosticLogsModal
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VaultScreen as ScreenEnum
import com.example.ui.viewmodel.VaultViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: VaultViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    handleIncomingShareIntent(intent)

    setContent {
      MyApplicationTheme {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val allClips by viewModel.allClips.collectAsStateWithLifecycle()
        val activeClips by viewModel.activeClips.collectAsStateWithLifecycle()
        val archivedClips by viewModel.archivedClips.collectAsStateWithLifecycle()
        val activeNotes by viewModel.activeNotes.collectAsStateWithLifecycle()
        val clipCount by viewModel.clipCount.collectAsStateWithLifecycle()
        val noteCount by viewModel.noteCount.collectAsStateWithLifecycle()
        val diagnosticLogs by viewModel.diagnosticLogs.collectAsStateWithLifecycle()
        val firebaseSyncStatus by viewModel.firebaseSyncStatus.collectAsStateWithLifecycle()

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(uiState.snackbarMessage) {
          uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
          }
        }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = MaterialTheme.colorScheme.background,
          topBar = {
            TopVaultBar(
              currentScreen = uiState.currentScreen,
              clipCount = clipCount,
              environmentConfig = uiState.environmentConfig,
              userRole = uiState.currentRole,
              firebaseSyncStatus = firebaseSyncStatus
            )
          },
          bottomBar = {
            BottomVaultNavigation(
              currentScreen = uiState.currentScreen,
              onNavigate = { viewModel.navigateTo(it) }
            )
          },
          snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            Crossfade(
              targetState = uiState.currentScreen,
              label = "ScreenTransition"
            ) { targetScreen ->
              when (targetScreen) {
                ScreenEnum.CAPTURE -> {
                  CaptureScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    recentClips = activeClips
                  )
                }
                ScreenEnum.VAULT -> {
                  VaultScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    allClips = allClips,
                    activeClips = activeClips,
                    archivedClips = archivedClips
                  )
                }
                ScreenEnum.NOTES -> {
                  NotesScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    notes = activeNotes
                  )
                }
                ScreenEnum.SEARCH -> {
                  SearchScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    allClips = allClips
                  )
                }
                ScreenEnum.SETTINGS -> {
                  SettingsScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    clipCount = clipCount,
                    noteCount = noteCount
                  )
                }
              }
            }

            // Diagnostic Logs Bottom Sheet Modal
            if (uiState.isDiagnosticModalOpen) {
              DiagnosticLogsModal(
                viewModel = viewModel,
                uiState = uiState,
                logs = diagnosticLogs
              )
            }
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIncomingShareIntent(intent)
  }

  private fun handleIncomingShareIntent(intent: Intent?) {
    if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
      val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
      if (!sharedText.isNullOrBlank()) {
        viewModel.updateCaptureInput(sharedText)
        viewModel.saveCapture(ClipSource.SHARE_INTENT)
      }
    }
  }
}
