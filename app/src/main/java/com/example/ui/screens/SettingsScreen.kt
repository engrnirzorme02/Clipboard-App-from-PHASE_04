package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.CloudSyncState
import com.example.data.firebase.FirebaseSyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DuplicateStrategy
import com.example.domain.model.EnvironmentType
import com.example.domain.model.UserRole
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultRose
import com.example.ui.viewmodel.VaultUiState
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: VaultViewModel,
  uiState: VaultUiState,
  clipCount: Int,
  noteCount: Int,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val syncStatus by viewModel.firebaseSyncStatus.collectAsStateWithLifecycle()

  var showExportDialog by remember { mutableStateOf(false) }
  var exportedJsonText by remember { mutableStateOf("") }
  var showImportDialog by remember { mutableStateOf(false) }
  var importInputJson by remember { mutableStateOf("") }
  var selectedImportStrategy by remember { mutableStateOf(DuplicateStrategy.SKIP_DUPLICATES) }
  var showWipeConfirmDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
      .verticalScroll(rememberScrollState())
  ) {
    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = "Environment & Vault Settings",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 1. Multi-Environment Configuration Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SettingsSuggest, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Active Environment", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
          }

          val envBadgeColor = when (uiState.environmentConfig.type) {
            EnvironmentType.DEVELOPMENT -> VaultAmber
            EnvironmentType.STAGING -> Color(0xFF6366F1)
            EnvironmentType.PRODUCTION -> VaultEmerald
          }

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = envBadgeColor.copy(alpha = 0.15f)
          ) {
            Text(
              text = uiState.environmentConfig.type.displayName.uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
              color = envBadgeColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Switch configuration profiles with environment-tailored sync intervals, retention policies, and validation gates.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Environment Selector Chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          EnvironmentType.values().forEach { env ->
            val isSelected = uiState.environmentConfig.type == env
            FilterChip(
              selected = isSelected,
              onClick = { viewModel.switchEnvironment(env) },
              label = { Text(env.displayName, style = MaterialTheme.typography.labelSmall) },
              modifier = Modifier.weight(1f),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.primary
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Environment Details Specs
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("API Endpoint: ${uiState.environmentConfig.apiBaseUrl}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
            Text("Sync Interval: ${uiState.environmentConfig.syncIntervalSeconds}s • Retention: ${uiState.environmentConfig.autoScrubbingRetentionDays} days", style = MaterialTheme.typography.labelSmall)
            Text("Strict Payload Validation: ${if (uiState.environmentConfig.strictPayloadValidation) "ENABLED" else "DISABLED"}", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 2. Role-Based Access Control (RBAC) Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("User Access & Security Role", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
          }

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
          ) {
            Text(
              text = uiState.currentRole.name,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = uiState.currentRole.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Role Selector Chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          UserRole.values().forEach { role ->
            val isSelected = uiState.currentRole == role
            FilterChip(
              selected = isSelected,
              onClick = { viewModel.switchRole(role) },
              label = { Text(role.name, style = MaterialTheme.typography.labelSmall) },
              modifier = Modifier.weight(1f),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.primary
              )
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 3. Firebase Cloud Auto-Save & Synchronization Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("firebase_autosave_card"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.CloudSync,
              contentDescription = "Firebase Sync",
              tint = VaultCyan,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Firebase Cloud Auto-Save",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Real-time Cloud Firestore Sync",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Switch(
            checked = syncStatus.isAutoSaveEnabled,
            onCheckedChange = { viewModel.toggleFirebaseAutoSave(it) },
            colors = SwitchDefaults.colors(
              checkedThumbColor = VaultCyan,
              checkedTrackColor = VaultCyan.copy(alpha = 0.3f)
            ),
            modifier = Modifier.testTag("firebase_autosave_switch")
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Cloud Status Banner
        val (stateBgColor, stateIcon, stateText) = when (syncStatus.state) {
          CloudSyncState.SYNCED -> Triple(VaultEmerald, Icons.Default.CloudDone, "Cloud Synchronized")
          CloudSyncState.SYNCING -> Triple(VaultCyan, Icons.Default.CloudSync, "Syncing to Cloud...")
          CloudSyncState.DISCONNECTED_LOCAL_FIRST -> Triple(VaultAmber, Icons.Default.CloudQueue, "Local-First Mode (Offline Safe)")
          CloudSyncState.DISABLED -> Triple(Color.Gray, Icons.Default.CloudOff, "Cloud Sync Paused")
          CloudSyncState.ERROR -> Triple(VaultRose, Icons.Default.CloudOff, "Sync Notice: Local Fallback Active")
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = stateBgColor.copy(alpha = 0.12f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = stateIcon,
              contentDescription = null,
              tint = stateBgColor,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stateText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = stateBgColor
              )
              Text(
                text = "Last Cloud Event: ${syncStatus.formattedLastSync} • ${syncStatus.statusMessage}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Synced Counts Chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text(
                text = "Clips Synced",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "${syncStatus.syncedClipsCount}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text(
                text = "Notes Synced",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "${syncStatus.syncedNotesCount}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text(
                text = "Storage Layer",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "Firestore",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = VaultCyan
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cloud Push / Pull action buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = { viewModel.pushAllToFirebase() },
            enabled = uiState.currentRole.canEdit,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("push_firebase_button")
          ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Push to Cloud", style = MaterialTheme.typography.labelMedium)
          }

          OutlinedButton(
            onClick = { viewModel.pullAllFromFirebase() },
            enabled = uiState.currentRole.canImport,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("pull_firebase_button")
          ) {
            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Pull Cloud", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 4. Automated Task Execution Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CleaningServices, contentDescription = null, tint = VaultEmerald)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Automated Task Execution", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Execute maintenance routines (Auto-Scrubbing expired unpinned clips, content format tagging, credential clearance checks).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = { viewModel.runMaintenanceAutomation() },
          enabled = !uiState.isAutomationRunning && uiState.currentRole.canRunAutomation,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          if (uiState.isAutomationRunning) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Running Maintenance...")
          } else {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Run Maintenance Automation")
          }
        }

        if (uiState.automationSummary != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = VaultEmerald.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("Maintenance Summary:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = VaultEmerald)
              Text("Rules evaluated: ${uiState.automationSummary.rulesEvaluated} • Items scrubbed: ${uiState.automationSummary.itemsScrubbed}", style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 4. Diagnostic & Error Logging Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.BugReport, contentDescription = null, tint = VaultCyan)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Diagnostic & Error Logs", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "View structured diagnostic events, stack traces, security audits, and export diagnostic JSON reports.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
          onClick = { viewModel.openDiagnosticModal() },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("view_diagnostic_logs_button")
        ) {
          Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Inspect Diagnostic Logs")
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 5. Storage Statistics Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Local Persistence Stats", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          StatBox("Total Clips", "$clipCount", Modifier.weight(1f))
          Spacer(modifier = Modifier.width(8.dp))
          StatBox("Vault Notes", "$noteCount", Modifier.weight(1f))
          Spacer(modifier = Modifier.width(8.dp))
          StatBox("Status", "Authoritative", Modifier.weight(1f))
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 6. JSON Backup & Restore Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("JSON Backup & Disaster Recovery", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Export vault records as JSON with checksum integrity verification or restore from backup file.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = {
              scope.launch {
                exportedJsonText = viewModel.generateBackupJson()
                showExportDialog = true
              }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("export_backup_button")
          ) {
            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Export JSON")
          }

          OutlinedButton(
            onClick = {
              importInputJson = ""
              showImportDialog = true
            },
            enabled = uiState.currentRole.canImport,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("import_backup_button")
          ) {
            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Import JSON")
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 7. Danger Zone
    if (uiState.currentRole.canWipe) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VaultRose.copy(alpha = 0.08f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = VaultRose)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Danger Zone", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = VaultRose)
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Permanently wipe all captured clips and notes from this device's local database.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = { showWipeConfirmDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("wipe_data_button")
          ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Wipe All Vault Data")
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(32.dp))
  }

  // Export Dialog / Bottom Sheet
  if (showExportDialog) {
    ModalBottomSheet(
      onDismissRequest = { showExportDialog = false },
      containerColor = MaterialTheme.colorScheme.surface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(bottom = 32.dp)
      ) {
        Text("Vault JSON Backup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(4.dp))
        Text("Payload includes $clipCount clips and $noteCount notes with SHA-256 checksum.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
        ) {
          Text(
            text = exportedJsonText,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
            modifier = Modifier
              .padding(12.dp)
              .verticalScroll(rememberScrollState())
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(onClick = { showExportDialog = false }) {
            Text("Close")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("Vault Backup", exportedJsonText))
              viewModel.showSnackbar("Backup JSON copied to clipboard!")
              showExportDialog = false
            }
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy JSON")
          }
        }
      }
    }
  }

  // Import Dialog / Bottom Sheet
  if (showImportDialog) {
    ModalBottomSheet(
      onDismissRequest = {
        showImportDialog = false
        viewModel.dismissImportPreview()
      },
      containerColor = MaterialTheme.colorScheme.surface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(bottom = 32.dp)
      ) {
        Text("Restore from JSON Backup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(6.dp))

        if (uiState.importPreview == null) {
          OutlinedTextField(
            value = importInputJson,
            onValueChange = { importInputJson = it },
            placeholder = { Text("Paste valid Vault JSON backup string here...") },
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp)
              .testTag("import_json_input"),
            shape = RoundedCornerShape(12.dp)
          )

          if (uiState.importMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = uiState.importMessage,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            OutlinedButton(onClick = { showImportDialog = false }) {
              Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = { viewModel.previewImportJson(importInputJson) },
              enabled = importInputJson.isNotBlank()
            ) {
              Text("Verify & Preview")
            }
          }
        } else {
          // Preview and Duplicate Resolution Strategy
          val preview = uiState.importPreview
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text("✅ Schema v${preview.manifest.schemaVersion} Validated", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
              Text("Clips found: ${preview.validClips.size} (${preview.duplicateClipsCount} existing duplicates)", style = MaterialTheme.typography.bodySmall)
              Text("Notes found: ${preview.validNotes.size}", style = MaterialTheme.typography.bodySmall)
              Text("Checksum: ${preview.manifest.checksum}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Text("Duplicate Resolution Policy:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))

          DuplicateStrategy.values().forEach { strategy ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = selectedImportStrategy == strategy,
                onClick = { selectedImportStrategy = strategy }
              )
              Text(
                text = when (strategy) {
                  DuplicateStrategy.SKIP_DUPLICATES -> "Skip duplicates (keep existing items)"
                  DuplicateStrategy.OVERWRITE -> "Overwrite matching existing items"
                  DuplicateStrategy.KEEP_BOTH -> "Keep both (assign new IDs)"
                },
                style = MaterialTheme.typography.bodySmall
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            OutlinedButton(onClick = { viewModel.dismissImportPreview() }) {
              Text("Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                viewModel.executeImport(selectedImportStrategy)
                showImportDialog = false
              }
            ) {
              Text("Confirm Restore")
            }
          }
        }
      }
    }
  }

  // Wipe confirmation dialog
  if (showWipeConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showWipeConfirmDialog = false },
      icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = VaultRose) },
      title = { Text("Wipe All Vault Data?") },
      text = { Text("This will permanently delete all captured clips, notes, and local configurations. This action cannot be undone.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearAllVaultData()
            showWipeConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Yes, Wipe All")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showWipeConfirmDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
      Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
