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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ClipItem
import com.example.domain.model.ClipSource
import com.example.ui.components.ClipCard
import com.example.ui.viewmodel.VaultUiState
import com.example.ui.viewmodel.VaultViewModel

@Composable
fun SearchScreen(
  viewModel: VaultViewModel,
  uiState: VaultUiState,
  allClips: List<ClipItem>,
  modifier: Modifier = Modifier
) {
  var selectedSourceFilter by remember { mutableStateOf<ClipSource?>(null) }

  val filteredResults = remember(allClips, uiState.searchQuery, selectedSourceFilter) {
    if (uiState.searchQuery.isBlank() && selectedSourceFilter == null) {
      emptyList()
    } else {
      allClips.filter { clip ->
        val matchesQuery = uiState.searchQuery.isBlank() ||
          clip.text.contains(uiState.searchQuery, ignoreCase = true) ||
          (clip.title?.contains(uiState.searchQuery, ignoreCase = true) == true) ||
          clip.shortCode.contains(uiState.searchQuery, ignoreCase = true) ||
          clip.tags.any { it.contains(uiState.searchQuery, ignoreCase = true) }

        val matchesSource = selectedSourceFilter == null || clip.source == selectedSourceFilter
        matchesQuery && matchesSource
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(4.dp))

    // Search Input Field
    OutlinedTextField(
      value = uiState.searchQuery,
      onValueChange = { viewModel.updateSearchQuery(it) },
      placeholder = { Text("Search by content, title, short ID, or #tag...") },
      leadingIcon = {
        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      },
      trailingIcon = {
        if (uiState.searchQuery.isNotEmpty()) {
          IconButton(onClick = { viewModel.updateSearchQuery("") }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear")
          }
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("global_search_input"),
      shape = RoundedCornerShape(14.dp),
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
      )
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Source Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterChip(
        selected = selectedSourceFilter == null,
        onClick = { selectedSourceFilter = null },
        label = { Text("All Sources") },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
          selectedLabelColor = MaterialTheme.colorScheme.primary
        )
      )
      ClipSource.values().forEach { source ->
        FilterChip(
          selected = selectedSourceFilter == source,
          onClick = {
            selectedSourceFilter = if (selectedSourceFilter == source) null else source
          },
          label = { Text(source.name) }
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (uiState.searchQuery.isBlank() && selectedSourceFilter == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.TravelExplore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(54.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Instant Vault Search",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Type any keyword, short ID (e.g. CLP-9A4B), or tag to search through your local vault.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
        }
      }
    } else if (filteredResults.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "No results found for \"${uiState.searchQuery}\"",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Matching Vault Items",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "${filteredResults.size} found",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(filteredResults, key = { it.id }) { clip ->
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

  if (uiState.activeClipDetail != null) {
    ClipDetailModal(
      clip = uiState.activeClipDetail,
      viewModel = viewModel,
      uiState = uiState,
      onDismiss = { viewModel.selectClipDetail(null) }
    )
  }
}
