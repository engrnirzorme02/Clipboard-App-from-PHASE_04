package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ClipItem
import com.example.domain.model.ClipSource
import com.example.domain.model.DetectedFormatType
import com.example.ui.components.ClipCard
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultRose
import com.example.ui.viewmodel.VaultUiState
import com.example.ui.viewmodel.VaultViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaptureScreen(
  viewModel: VaultViewModel,
  uiState: VaultUiState,
  recentClips: List<ClipItem>,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var showTitleField by remember { mutableStateOf(false) }

  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(Unit) {
    if (uiState.currentRole.canCapture) {
      focusRequester.requestFocus()
    }
  }

  val commonTags = listOf("work", "personal", "code", "link", "todo", "important")
  val isAuditorReadOnly = !uiState.currentRole.canCapture

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(4.dp))

      // Auditor Mode Warning Banner if applicable
      if (isAuditorReadOnly) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Visibility,
              contentDescription = "Read-Only",
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Security Auditor Profile (Read-Only)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onTertiaryContainer
              )
              Text(
                text = "Data capture and modification are disabled for this session role.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      // Main Capture Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Card Header with Paste action
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Rapid Text Capture",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Profile: ${uiState.environmentConfig.type.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }

            // Paste from System Clipboard
            OutlinedButton(
              onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                  val text = clip.getItemAt(0).text?.toString() ?: ""
                  viewModel.pasteFromClipboard(text)
                }
              },
              enabled = !isAuditorReadOnly,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("paste_clipboard_button")
            ) {
              Icon(
                imageVector = Icons.Default.ContentPaste,
                contentDescription = "Paste",
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Paste", style = MaterialTheme.typography.labelMedium)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Optional Title Field
          AnimatedVisibility(visible = showTitleField) {
            Column {
              OutlinedTextField(
                value = uiState.captureTitle,
                onValueChange = { viewModel.updateCaptureTitle(it) },
                placeholder = { Text("Title (Optional)") },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("capture_title_input"),
                enabled = !isAuditorReadOnly,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
              )
              Spacer(modifier = Modifier.height(10.dp))
            }
          }

          // Main Multiline Input
          OutlinedTextField(
            value = uiState.captureInput,
            onValueChange = { viewModel.updateCaptureInput(it) },
            placeholder = { Text("👉 পেস্ট করতে কার্সর রাখুন (Auto-Capture), or type here...") },
            enabled = !isAuditorReadOnly,
            modifier = Modifier
              .fillMaxWidth()
              .height(150.dp)
              .focusRequester(focusRequester)
              .testTag("capture_text_input"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Real-time Format Inspector & Validation Pill
          if (uiState.captureInput.isNotBlank()) {
            val format = uiState.formatValidation
            val formatColor = if (format.isValid) {
              if (format.isSensitiveCandidate) VaultRose else VaultCyan
            } else {
              VaultAmber
            }

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = formatColor.copy(alpha = 0.12f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  val iconVector = when (format.formatType) {
                    DetectedFormatType.JSON_OBJECT -> Icons.Default.Code
                    DetectedFormatType.JSON_ARRAY -> Icons.Default.DataArray
                    DetectedFormatType.URL_LINK -> Icons.Default.Link
                    DetectedFormatType.EMAIL -> Icons.Default.Email
                    DetectedFormatType.CODE_SNIPPET -> Icons.Default.Terminal
                    DetectedFormatType.SECRET_CREDENTIAL -> Icons.Default.Lock
                    DetectedFormatType.MARKDOWN -> Icons.Default.Description
                    DetectedFormatType.PLAINTEXT -> Icons.Default.Notes
                  }
                  Icon(
                    imageVector = iconVector,
                    contentDescription = format.formatType.label,
                    tint = formatColor,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "${format.formatType.label}: ${format.validationMessage}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = formatColor
                  )
                }

                Text(
                  text = "${format.byteSize} B",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
          }

          // Text metrics + Sensitivity & Title Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "${uiState.captureInput.length} chars • ${uiState.formatValidation.lineCount} lines • ${uiState.formatValidation.wordCount} words",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              TextButton(
                onClick = { showTitleField = !showTitleField }
              ) {
                Text(if (showTitleField) "Hide Title" else "+ Add Title", style = MaterialTheme.typography.labelSmall)
              }

              IconButton(
                onClick = { viewModel.toggleCaptureSensitivity() },
                enabled = !isAuditorReadOnly,
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  imageVector = if (uiState.captureIsSensitive) Icons.Default.Lock else Icons.Default.LockOpen,
                  contentDescription = "Toggle sensitive",
                  tint = if (uiState.captureIsSensitive) VaultRose else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }

          // Quick Tags
          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            commonTags.forEach { tag ->
              val isSelected = uiState.captureTags.contains(tag)
              FilterChip(
                selected = isSelected,
                onClick = { if (!isAuditorReadOnly) viewModel.toggleCaptureTag(tag) },
                label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.primary
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Save Action Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            if (uiState.captureInput.isNotBlank()) {
              TextButton(
                onClick = { viewModel.updateCaptureInput("") },
                modifier = Modifier.padding(end = 8.dp)
              ) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear")
              }
            }

            Button(
              onClick = { viewModel.saveCapture(ClipSource.KEYBOARD) },
              enabled = uiState.captureInput.isNotBlank() && !isAuditorReadOnly,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
              ),
              modifier = Modifier.testTag("save_capture_button")
            ) {
              Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Save to Vault", fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }

    // Feedback Message or Duplicate Alert Banner
    if (uiState.duplicateDetectedClip != null) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = VaultAmber.copy(alpha = 0.12f)
          )
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Duplicate Warning",
                tint = VaultAmber,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Duplicate Clip Detected",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = VaultAmber
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "This content matches existing vault item [${uiState.duplicateDetectedClip.shortCode}].",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = { viewModel.selectClipDetail(uiState.duplicateDetectedClip) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text("View Existing")
              }
              Button(
                onClick = { viewModel.saveCapture(ClipSource.KEYBOARD, forceAllowDuplicate = true) },
                enabled = !isAuditorReadOnly,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VaultAmber),
                modifier = Modifier.weight(1f)
              ) {
                Text("Save Duplicate")
              }
            }
          }
        }
      }
    } else if (uiState.lastCaptureMessage != null) {
      item {
        val isError = uiState.lastCaptureMessage.startsWith("Validation Error") || uiState.lastCaptureMessage.startsWith("Capture rejected")
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isError) VaultRose.copy(alpha = 0.15f) else VaultEmerald.copy(alpha = 0.15f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.Check,
              contentDescription = null,
              tint = if (isError) VaultRose else VaultEmerald,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = uiState.lastCaptureMessage,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = if (isError) VaultRose else VaultEmerald
            )
          }
        }
      }
    }

    // Recent Captures Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Recent Captures",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "${recentClips.size} items",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    if (recentClips.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No clips in vault yet. Type or paste above to capture!",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    } else {
      items(recentClips.take(6), key = { it.id }) { clip ->
        ClipCard(
          clip = clip,
          onClick = { viewModel.selectClipDetail(clip) },
          onCopy = { viewModel.showSnackbar("Copied [${clip.shortCode}] to clipboard") },
          onTogglePin = { viewModel.togglePinClip(clip) },
          onToggleArchive = { viewModel.toggleArchiveClip(clip) },
          onDelete = { viewModel.deleteClip(clip) },
          onConvertToNote = { viewModel.convertClipToNote(clip) }
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
