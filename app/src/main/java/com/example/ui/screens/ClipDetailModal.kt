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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ClipItem
import com.example.domain.usecase.AiTaskType
import com.example.domain.usecase.DataValidator
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultRose
import com.example.ui.viewmodel.VaultUiState
import com.example.ui.viewmodel.VaultViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClipDetailModal(
  clip: ClipItem,
  viewModel: VaultViewModel,
  uiState: VaultUiState,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var isEditing by remember { mutableStateOf(false) }
  var editedTitle by remember { mutableStateOf(clip.title ?: "") }
  var editedText by remember { mutableStateOf(clip.text) }
  var newTagInput by remember { mutableStateOf("") }
  var currentTags by remember { mutableStateOf(clip.tags.toMutableList()) }

  val formatResult = remember(clip.text) {
    DataValidator.inspectAndValidate(clip.text)
  }

  val formattedTime = remember(clip.createdAt) {
    SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.getDefault()).format(Date(clip.createdAt))
  }

  val canEdit = uiState.currentRole.canEdit
  val canDelete = uiState.currentRole.canDelete

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.9f)
        .padding(horizontal = 20.dp)
        .verticalScroll(rememberScrollState())
    ) {
      // Header: Short Code & Top Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer
        ) {
          Text(
            text = clip.shortCode,
            style = MaterialTheme.typography.titleSmall.copy(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { viewModel.togglePinClip(clip) },
            enabled = canEdit
          ) {
            Icon(
              imageVector = if (clip.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
              contentDescription = "Pin",
              tint = if (clip.pinned) VaultAmber else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          IconButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText(clip.title ?: "Clip", clip.text))
              viewModel.showSnackbar("Copied [${clip.shortCode}] to clipboard")
            }
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
          }

          IconButton(
            onClick = {
              if (canEdit) {
                isEditing = !isEditing
                editedTitle = clip.title ?: ""
                editedText = clip.text
                currentTags = clip.tags.toMutableList()
              } else {
                viewModel.showSnackbar("Permission Denied: Read-only access.")
              }
            }
          ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Format Inspection Badge
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Format: ${formatResult.formatType.label} (${formatResult.validationMessage})",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "${formatResult.byteSize} bytes",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Content or Edit Fields
      if (isEditing) {
        OutlinedTextField(
          value = editedTitle,
          onValueChange = { editedTitle = it },
          label = { Text("Title") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = editedText,
          onValueChange = { editedText = it },
          label = { Text("Content") },
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tag management in edit mode
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = newTagInput,
            onValueChange = { newTagInput = it },
            placeholder = { Text("Add tag (e.g. work)") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
          )
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (newTagInput.isNotBlank()) {
                val clean = newTagInput.trim().lowercase(Locale.ROOT)
                if (!currentTags.contains(clean)) {
                  currentTags.add(clean)
                }
                newTagInput = ""
              }
            },
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Add")
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          currentTags.forEach { tag ->
            FilterChip(
              selected = true,
              onClick = { currentTags.remove(tag) },
              label = { Text("#$tag ✕", style = MaterialTheme.typography.labelSmall) }
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = {
            viewModel.updateClipText(clip.id, editedTitle, editedText, currentTags)
            isEditing = false
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.Save, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Save Changes")
        }
      } else {
        if (!clip.title.isNullOrBlank()) {
          Text(
            text = clip.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(8.dp))
        }

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = clip.text,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp)
          )
        }

        if (clip.tags.isNotEmpty()) {
          Spacer(modifier = Modifier.height(10.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            clip.tags.forEach { tag ->
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
              ) {
                Text(
                  text = "#$tag",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))
      Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      Spacer(modifier = Modifier.height(14.dp))

      // AI Transformation Suite
      Text(
        text = "AI Transformation & Analysis",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(8.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        AiTaskType.values().forEach { task ->
          OutlinedButton(
            onClick = { viewModel.runAiTransformation(clip.text, task) },
            enabled = !uiState.isAiLoading && canEdit,
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(task.label, style = MaterialTheme.typography.labelSmall)
          }
        }
      }

      // AI Output Card
      AnimatedVisibility(visible = uiState.isAiLoading || uiState.aiTransformResult != null) {
        Column(modifier = Modifier.padding(top = 14.dp)) {
          if (uiState.isAiLoading) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Text("Processing with AI...", style = MaterialTheme.typography.bodyMedium)
            }
          } else if (uiState.aiTransformResult != null) {
            val res = uiState.aiTransformResult
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "AI Output: ${res.taskType.label}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                  )
                  IconButton(
                    onClick = {
                      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                      clipboard.setPrimaryClip(ClipData.newPlainText("AI Result", res.output))
                      viewModel.showSnackbar("AI output copied to clipboard")
                    },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy AI Result", modifier = Modifier.size(16.dp))
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = res.output, style = MaterialTheme.typography.bodyMedium)
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))
      Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      Spacer(modifier = Modifier.height(14.dp))

      // Metadata Audit Card
      Text(
        text = "Audit & Technical Metadata",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(8.dp))

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        MetadataRow("Short Code", clip.shortCode)
        MetadataRow("Record UUID", clip.id.take(16) + "...")
        MetadataRow("Created At", formattedTime)
        MetadataRow("Source", clip.source.name)
        MetadataRow("Sensitivity", clip.sensitivity.name)
        MetadataRow("Sync State", clip.syncState.name)
        MetadataRow("Hash", clip.normalizedHash.take(16) + "...")
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Bottom Row Actions: Convert to Note, Archive, Delete
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = {
            viewModel.convertClipToNote(clip)
            onDismiss()
          },
          enabled = canEdit,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Note", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
          onClick = {
            viewModel.toggleArchiveClip(clip)
            onDismiss()
          },
          enabled = canEdit,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = if (clip.archived) Icons.Default.Unarchive else Icons.Default.Archive,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (clip.archived) "Restore" else "Archive", style = MaterialTheme.typography.labelMedium)
        }

        Button(
          onClick = {
            viewModel.deleteClip(clip)
            onDismiss()
          },
          enabled = canDelete,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Delete", style = MaterialTheme.typography.labelMedium)
        }
      }

      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}

@Composable
fun MetadataRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text = value, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
  }
}
