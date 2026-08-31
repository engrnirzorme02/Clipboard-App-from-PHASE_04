package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ClipItem
import com.example.domain.model.SensitivityLevel
import com.example.ui.components.ClipCard
import com.example.ui.viewmodel.VaultFilter
import com.example.ui.viewmodel.VaultUiState
import com.example.ui.viewmodel.VaultViewModel

@Composable
fun VaultScreen(
  viewModel: VaultViewModel,
  uiState: VaultUiState,
  allClips: List<ClipItem>,
  activeClips: List<ClipItem>,
  archivedClips: List<ClipItem>,
  modifier: Modifier = Modifier
) {
  var localSearchQuery by remember { mutableStateOf("") }

  // Extract all distinct tags
  val allTags = remember(allClips) {
    allClips.flatMap { it.tags }.distinct().sorted()
  }

  // Filter clips
  val baseList = when (uiState.vaultFilter) {
    VaultFilter.ALL -> activeClips
    VaultFilter.PINNED -> activeClips.filter { it.pinned }
    VaultFilter.SENSITIVE -> activeClips.filter { it.sensitivity == SensitivityLevel.SENSITIVE }
    VaultFilter.ARCHIVED -> archivedClips
  }

  val filteredList = remember(baseList, uiState.selectedTagFilter, localSearchQuery) {
    baseList.filter { clip ->
      val matchesTag = uiState.selectedTagFilter == null || clip.tags.contains(uiState.selectedTagFilter)
      val matchesSearch = localSearchQuery.isBlank() ||
        clip.text.contains(localSearchQuery, ignoreCase = true) ||
        (clip.title?.contains(localSearchQuery, ignoreCase = true) == true) ||
        clip.shortCode.contains(localSearchQuery, ignoreCase = true)
      matchesTag && matchesSearch
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(4.dp))

    // Search bar within vault
    OutlinedTextField(
      value = localSearchQuery,
      onValueChange = { localSearchQuery = it },
      placeholder = { Text("Filter vault items by text, title or ID...") },
      leadingIcon = {
        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
      },
      trailingIcon = {
        if (localSearchQuery.isNotEmpty()) {
          IconButton(onClick = { localSearchQuery = "" }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear")
          }
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("vault_search_filter_input"),
      shape = RoundedCornerShape(14.dp),
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
      )
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Status Filter Chips Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      VaultFilter.values().forEach { filter ->
        val count = when (filter) {
          VaultFilter.ALL -> activeClips.size
          VaultFilter.PINNED -> activeClips.count { it.pinned }
          VaultFilter.SENSITIVE -> activeClips.count { it.sensitivity == SensitivityLevel.SENSITIVE }
          VaultFilter.ARCHIVED -> archivedClips.size
        }
        FilterChip(
          selected = uiState.vaultFilter == filter,
          onClick = { viewModel.setVaultFilter(filter) },
          label = { Text("${filter.label} ($count)", style = MaterialTheme.typography.labelMedium) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primary
          )
        )
      }
    }

    // Tag Filter Chips (if any exist)
    if (allTags.isNotEmpty()) {
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        FilterChip(
          selected = uiState.selectedTagFilter == null,
          onClick = { viewModel.setVaultTagFilter(null) },
          label = { Text("All Tags", style = MaterialTheme.typography.labelSmall) }
        )
        allTags.forEach { tag ->
          FilterChip(
            selected = uiState.selectedTagFilter == tag,
            onClick = {
              viewModel.setVaultTagFilter(if (uiState.selectedTagFilter == tag) null else tag)
            },
            label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) }
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Vault Clips List
    if (filteredList.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = if (localSearchQuery.isNotBlank() || uiState.selectedTagFilter != null)
              "No clips match the selected filters."
            else
              "No clips in this vault category.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(filteredList, key = { it.id }) { clip ->
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
        item {
          Spacer(modifier = Modifier.height(20.dp))
        }
      }
    }
  }

  // Clip Detail Modal
  if (uiState.activeClipDetail != null) {
    ClipDetailModal(
      clip = uiState.activeClipDetail,
      viewModel = viewModel,
      uiState = uiState,
      onDismiss = { viewModel.selectClipDetail(null) }
    )
  }
}
