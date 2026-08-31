package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ClipboardItem
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultIndigo
import com.example.ui.theme.VaultRose
import com.example.ui.viewmodel.ClipboardUiState
import com.example.ui.viewmodel.ClipboardViewModel

@Composable
fun ClipboardScreen(
  viewModel: ClipboardViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val filteredItems by viewModel.filteredItems.collectAsStateWithLifecycle()

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(6.dp))

    // 1. Search Bar at the Top to filter by content or category
    OutlinedTextField(
      value = uiState.searchQuery,
      onValueChange = { viewModel.setSearchQuery(it) },
      placeholder = { Text("Search by content or category...") },
      leadingIcon = {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = "Search",
          tint = MaterialTheme.colorScheme.primary
        )
      },
      trailingIcon = {
        if (uiState.searchQuery.isNotEmpty()) {
          IconButton(onClick = { viewModel.setSearchQuery("") }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear search")
          }
        }
      },
      shape = RoundedCornerShape(16.dp),
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("clipboard_search_bar")
    )

    Spacer(modifier = Modifier.height(10.dp))

    // 2. Category Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      viewModel.predefinedCategories.forEach { categoryName ->
        val isSelected = if (categoryName == "All") {
          uiState.selectedCategory == null
        } else {
          uiState.selectedCategory.equals(categoryName, ignoreCase = true)
        }

        FilterChip(
          selected = isSelected,
          onClick = {
            if (categoryName == "All") {
              viewModel.setSelectedCategory(null)
            } else {
              viewModel.setSelectedCategory(if (isSelected) null else categoryName)
            }
          },
          label = {
            Text(categoryName, style = MaterialTheme.typography.labelMedium)
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primary
          ),
          shape = RoundedCornerShape(10.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3. Quick-Add / Edit Clipboard Item Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("add_clipboard_item_card"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (uiState.editingItem != null) "Edit Clipboard Item" else "New Clipboard Item",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
          )

          if (!uiState.isAddCardExpanded && uiState.editingItem == null) {
            Button(
              onClick = { viewModel.setAddCardExpanded(true) },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("expand_add_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add Item")
            }
          }
        }

        AnimatedVisibility(
          visible = uiState.isAddCardExpanded || uiState.editingItem != null,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          Column(modifier = Modifier.padding(top = 10.dp)) {
            OutlinedTextField(
              value = uiState.contentInput,
              onValueChange = { viewModel.setContentInput(it) },
              label = { Text("Clipboard Text Content") },
              placeholder = { Text("Enter text, code snippet, or link...") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("clipboard_content_input"),
              shape = RoundedCornerShape(12.dp),
              minLines = 2,
              maxLines = 4
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
              value = uiState.categoryInput,
              onValueChange = { viewModel.setCategoryInput(it) },
              label = { Text("Category") },
              placeholder = { Text("General, Code, Links, Notes, etc.") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("clipboard_category_input"),
              shape = RoundedCornerShape(12.dp),
              singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              OutlinedButton(
                onClick = { viewModel.cancelEditing() },
                shape = RoundedCornerShape(10.dp)
              ) {
                Text("Cancel")
              }

              Spacer(modifier = Modifier.width(8.dp))

              Button(
                onClick = { viewModel.addOrUpdateClipboardItem() },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_clipboard_button")
              ) {
                Text(if (uiState.editingItem != null) "Update" else "Save to Database")
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 4. Header with Count
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Saved Items",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
      Text(
        text = "${filteredItems.size} items",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 5. LazyColumn to display the list of saved clipboard items
    if (filteredItems.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.ContentPaste,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(54.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null)
              "No clipboard items match your filter."
            else
              "No clipboard items yet. Add your first item above!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .testTag("clipboard_lazy_column"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(filteredItems, key = { it.id }) { item ->
          ClipboardItemCard(
            item = item,
            onCopy = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("Copied Text", item.content)
              clipboard.setPrimaryClip(clip)
              viewModel.showSnackbar("Copied to clipboard!")
            },
            onEdit = { viewModel.startEditing(item) },
            onDelete = { viewModel.deleteClipboardItem(item) }
          )
        }
        item {
          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }
  }
}

@Composable
fun ClipboardItemCard(
  item: ClipboardItem,
  onCopy: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val categoryColor = when (item.category.lowercase()) {
    "code", "snippets" -> VaultCyan
    "link", "links", "url" -> VaultIndigo
    "notes", "note" -> VaultAmber
    "password", "passwords", "auth" -> VaultRose
    else -> VaultEmerald
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("clipboard_item_card_${item.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Category Badge
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = categoryColor.copy(alpha = 0.15f),
          modifier = Modifier.clip(RoundedCornerShape(20.dp))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Bookmark,
              contentDescription = null,
              tint = categoryColor,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = item.category.uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
              color = categoryColor
            )
          }
        }

        // Timestamp
        Text(
          text = item.formattedTimestamp,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Content Preview
      Text(
        text = item.content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 6,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Action Buttons Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onCopy,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy Content",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
        }

        IconButton(
          onClick = onEdit,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit Item",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp)
          )
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete Item",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
