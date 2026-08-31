package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.VaultNote
import com.example.ui.components.NoteCard
import com.example.ui.viewmodel.VaultUiState
import com.example.ui.viewmodel.VaultViewModel

@Composable
fun NotesScreen(
  viewModel: VaultViewModel,
  uiState: VaultUiState,
  notes: List<VaultNote>,
  modifier: Modifier = Modifier
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      FloatingActionButton(
        onClick = { viewModel.openCreateNote() },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("create_note_fab")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Note")
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Vault Notes & Documents",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "${notes.size} notes",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (notes.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.EditNote,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.outline,
              modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "No notes created yet.",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Convert clips into formatted notes or create a new note with the + button.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
              onClick = { viewModel.openCreateNote() },
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Create First Note")
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(notes, key = { it.id }) { note ->
            NoteCard(
              note = note,
              onClick = { viewModel.selectNoteDetail(note) },
              onDelete = { viewModel.deleteNote(note) }
            )
          }
          item {
            Spacer(modifier = Modifier.height(80.dp))
          }
        }
      }
    }
  }

  // Create Note Dialog
  if (uiState.isCreatingNote) {
    CreateNoteModal(
      onSave = { title, content, tags -> viewModel.saveNote(title, content, tags) },
      onDismiss = { viewModel.closeCreateNote() }
    )
  }

  // Note Detail Modal
  if (uiState.activeNoteDetail != null) {
    NoteDetailModal(
      note = uiState.activeNoteDetail,
      onUpdate = { title, content, tags -> viewModel.updateNote(uiState.activeNoteDetail, title, content, tags) },
      onDelete = { viewModel.deleteNote(uiState.activeNoteDetail) },
      onDismiss = { viewModel.selectNoteDetail(null) },
      onSnackbar = { viewModel.showSnackbar(it) }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoteModal(
  onSave: (String, String, List<String>) -> Unit,
  onDismiss: () -> Unit
) {
  var title by remember { mutableStateOf("") }
  var content by remember { mutableStateOf("") }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp)
    ) {
      Text(
        text = "Create Vault Note",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Note Title") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("note_title_input"),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = content,
        onValueChange = { content = it },
        label = { Text("Note Content") },
        modifier = Modifier
          .fillMaxWidth()
          .height(180.dp)
          .testTag("note_content_input"),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(18.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.padding(end = 8.dp)
        ) {
          Text("Cancel")
        }

        Button(
          onClick = { onSave(title, content, emptyList()) },
          enabled = content.isNotBlank(),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("save_note_button")
        ) {
          Icon(Icons.Default.Save, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Save Note")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailModal(
  note: VaultNote,
  onUpdate: (String, String, List<String>) -> Unit,
  onDelete: () -> Unit,
  onDismiss: () -> Unit,
  onSnackbar: (String) -> Unit
) {
  val context = LocalContext.current
  var isEditing by remember { mutableStateOf(false) }
  var editedTitle by remember { mutableStateOf(note.title) }
  var editedContent by remember { mutableStateOf(note.content) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.85f)
        .padding(horizontal = 20.dp)
        .verticalScroll(rememberScrollState())
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (isEditing) "Edit Note" else "Note View",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText(note.title, note.content))
              onSnackbar("Copied note content to clipboard")
            }
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
          }

          IconButton(
            onClick = { isEditing = !isEditing }
          ) {
            Icon(
              imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
              contentDescription = "Toggle Edit"
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (isEditing) {
        OutlinedTextField(
          value = editedTitle,
          onValueChange = { editedTitle = it },
          label = { Text("Title") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = editedContent,
          onValueChange = { editedContent = it },
          label = { Text("Content") },
          modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
          onClick = {
            onUpdate(editedTitle, editedContent, note.tags)
            isEditing = false
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.Save, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Save Changes")
        }
      } else {
        Text(
          text = note.title,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = note.content,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        Button(
          onClick = {
            onDelete()
            onDismiss()
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Icon(Icons.Default.Delete, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Delete Note")
        }
      }

      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}
