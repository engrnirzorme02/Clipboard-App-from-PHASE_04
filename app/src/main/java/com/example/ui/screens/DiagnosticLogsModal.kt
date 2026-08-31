package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DiagnosticLogEntry
import com.example.domain.model.LogSeverity
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultRose
import com.example.ui.viewmodel.VaultUiState
import com.example.ui.viewmodel.VaultViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticLogsModal(
  viewModel: VaultViewModel,
  uiState: VaultUiState,
  logs: List<DiagnosticLogEntry>
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current

  val filteredLogs = logs.filter { entry ->
    val matchesSeverity = uiState.selectedSeverityFilter == null || entry.severity == uiState.selectedSeverityFilter
    val matchesQuery = uiState.diagnosticSearchQuery.isBlank() ||
      entry.message.contains(uiState.diagnosticSearchQuery, ignoreCase = true) ||
      entry.component.name.contains(uiState.diagnosticSearchQuery, ignoreCase = true) ||
      (entry.details?.contains(uiState.diagnosticSearchQuery, ignoreCase = true) == true)
    matchesSeverity && matchesQuery
  }

  ModalBottomSheet(
    onDismissRequest = { viewModel.closeDiagnosticModal() },
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.9f)
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Diagnostic & Error Logs",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${filteredLogs.size} events recorded (${logs.size} total)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(onClick = { viewModel.closeDiagnosticModal() }) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Search Field
      OutlinedTextField(
        value = uiState.diagnosticSearchQuery,
        onValueChange = { viewModel.updateDiagnosticSearchQuery(it) },
        placeholder = { Text("Filter logs by keyword...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("diagnostic_search_input"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Severity Filters
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        FilterChip(
          selected = uiState.selectedSeverityFilter == null,
          onClick = { viewModel.setDiagnosticSeverityFilter(null) },
          label = { Text("ALL (${logs.size})", style = MaterialTheme.typography.labelSmall) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primary
          )
        )
        FilterChip(
          selected = uiState.selectedSeverityFilter == LogSeverity.ERROR,
          onClick = { viewModel.setDiagnosticSeverityFilter(if (uiState.selectedSeverityFilter == LogSeverity.ERROR) null else LogSeverity.ERROR) },
          label = { Text("ERRORS (${logs.count { it.severity == LogSeverity.ERROR }})", style = MaterialTheme.typography.labelSmall) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VaultRose.copy(alpha = 0.2f),
            selectedLabelColor = VaultRose
          )
        )
        FilterChip(
          selected = uiState.selectedSeverityFilter == LogSeverity.WARN,
          onClick = { viewModel.setDiagnosticSeverityFilter(if (uiState.selectedSeverityFilter == LogSeverity.WARN) null else LogSeverity.WARN) },
          label = { Text("WARNS", style = MaterialTheme.typography.labelSmall) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VaultAmber.copy(alpha = 0.2f),
            selectedLabelColor = VaultAmber
          )
        )
        FilterChip(
          selected = uiState.selectedSeverityFilter == LogSeverity.AUDIT,
          onClick = { viewModel.setDiagnosticSeverityFilter(if (uiState.selectedSeverityFilter == LogSeverity.AUDIT) null else LogSeverity.AUDIT) },
          label = { Text("AUDIT", style = MaterialTheme.typography.labelSmall) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VaultCyan.copy(alpha = 0.2f),
            selectedLabelColor = VaultCyan
          )
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Action Bar (Copy JSON, Clear Logs)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = {
            val json = viewModel.exportDiagnosticLogs()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("Diagnostic Logs JSON", json)
            clipboard.setPrimaryClip(clipData)
            viewModel.showSnackbar("Diagnostic report copied to clipboard")
          },
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Export JSON", style = MaterialTheme.typography.labelMedium)
        }

        TextButton(
          onClick = { viewModel.clearDiagnosticLogs() },
          colors = ButtonDefaults.textButtonColors(contentColor = VaultRose)
        ) {
          Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Clear Logs")
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Log Entries List
      if (filteredLogs.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "No diagnostic events match current filter.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(filteredLogs, key = { it.id }) { log ->
            DiagnosticLogItem(log = log)
          }
        }
      }
    }
  }
}

@Composable
fun DiagnosticLogItem(log: DiagnosticLogEntry) {
  val severityColor = when (log.severity) {
    LogSeverity.INFO -> VaultCyan
    LogSeverity.WARN -> VaultAmber
    LogSeverity.ERROR -> VaultRose
    LogSeverity.AUDIT -> Color(0xFF8B5CF6)
  }

  val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
  val timeString = timeFormatter.format(Date(log.timestamp))

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    )
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = severityColor.copy(alpha = 0.15f)
          ) {
            Text(
              text = log.severity.name,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = severityColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = log.component.name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Text(
          text = timeString,
          style = MaterialTheme.typography.labelSmall,
          fontFamily = FontFamily.Monospace,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = log.message,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface
      )

      if (!log.details.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = log.details,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
          )
        }
      }
    }
  }
}
