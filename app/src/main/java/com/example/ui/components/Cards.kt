package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ClipItem
import com.example.domain.model.SensitivityLevel
import com.example.domain.model.VaultNote
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultRose
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClipCard(
  clip: ClipItem,
  onClick: () -> Unit,
  onCopy: () -> Unit,
  onTogglePin: () -> Unit,
  onToggleArchive: () -> Unit,
  onDelete: () -> Unit,
  onConvertToNote: () -> Unit,
  modifier: Modifier = Modifier
) {
  var menuExpanded by remember { mutableStateOf(false) }
  val context = LocalContext.current

  val formattedTime = remember(clip.createdAt) {
    SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(clip.createdAt))
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("clip_card_${clip.shortCode}")
      .clip(RoundedCornerShape(16.dp))
      .clickable { onClick() },
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header: Short Code + Source Badge + Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Short Code Tag
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.clip(RoundedCornerShape(6.dp))
          ) {
            Text(
              text = clip.shortCode,
              style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              ),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }

          if (clip.sensitivity == SensitivityLevel.SENSITIVE) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = VaultRose.copy(alpha = 0.15f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = "Sensitive",
                  tint = VaultRose,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "Sensitive",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  color = VaultRose
                )
              }
            }
          }

          if (clip.pinned) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Default.PushPin,
              contentDescription = "Pinned",
              tint = VaultAmber,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Quick Copy Button
          IconButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText(clip.title ?: "Clip", clip.text))
              onCopy()
            },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = "Copy text",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }

          // Options Dropdown
          Box {
            IconButton(
              onClick = { menuExpanded = true },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }
            DropdownMenu(
              expanded = menuExpanded,
              onDismissRequest = { menuExpanded = false }
            ) {
              DropdownMenuItem(
                text = { Text(if (clip.pinned) "Unpin" else "Pin to top") },
                onClick = {
                  menuExpanded = false
                  onTogglePin()
                },
                leadingIcon = {
                  Icon(
                    imageVector = if (clip.pinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                    contentDescription = null
                  )
                }
              )
              DropdownMenuItem(
                text = { Text(if (clip.archived) "Restore from Archive" else "Archive") },
                onClick = {
                  menuExpanded = false
                  onToggleArchive()
                },
                leadingIcon = {
                  Icon(
                    imageVector = if (clip.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                    contentDescription = null
                  )
                }
              )
              DropdownMenuItem(
                text = { Text("Convert to Note") },
                onClick = {
                  menuExpanded = false
                  onConvertToNote()
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.NoteAdd,
                    contentDescription = null
                  )
                }
              )
              DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                  menuExpanded = false
                  onDelete()
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                  )
                }
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Title (if present)
      if (!clip.title.isNullOrBlank()) {
        Text(
          text = clip.title,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
      }

      // Content Preview
      val isSensitive = clip.sensitivity == SensitivityLevel.SENSITIVE
      var showSensitiveText by remember { mutableStateOf(false) }

      if (isSensitive && !showSensitiveText) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = VaultRose.copy(alpha = 0.1f),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showSensitiveText = true }
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = VaultRose, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "•••••••••••••••• (Sensitive Data - Tap to Reveal)",
              style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
              color = VaultRose
            )
          }
        }
      } else {
        Text(
          text = clip.text,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = if (isSensitive) FontFamily.Monospace else null
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = if (isSensitive) 10 else 3,
          overflow = TextOverflow.Ellipsis,
          modifier = if (isSensitive) Modifier.clickable { showSensitiveText = false } else Modifier
        )
      }

      // Tags Row
      if (clip.tags.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          clip.tags.take(4).forEach { tag ->
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.clip(RoundedCornerShape(4.dp))
            ) {
              Text(
                text = "#$tag",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          if (clip.tags.size > 4) {
            Text(
              text = "+${clip.tags.size - 4}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 2.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Footer: Time and Word / Char Count
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = formattedTime,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline
        )
        Text(
          text = "${clip.text.length} chars • ${clip.source.name.lowercase(Locale.ROOT)}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline
        )
      }
    }
  }
}

@Composable
fun NoteCard(
  note: VaultNote,
  onClick: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val formattedTime = remember(note.updatedAt) {
    SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(note.updatedAt))
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable { onClick() },
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = note.title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        if (note.pinned) {
          Icon(
            imageVector = Icons.Default.PushPin,
            contentDescription = "Pinned",
            tint = VaultAmber,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = note.content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = formattedTime,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline
        )
        Text(
          text = "${note.content.length} chars",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline
        )
      }
    }
  }
}
