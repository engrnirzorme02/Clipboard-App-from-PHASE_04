package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ClipboardItem
import com.example.data.repository.ClipboardRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClipboardUiState(
  val searchQuery: String = "",
  val selectedCategory: String? = null,
  val contentInput: String = "",
  val categoryInput: String = "General",
  val editingItem: ClipboardItem? = null,
  val isAddCardExpanded: Boolean = false,
  val snackbarMessage: String? = null
)

class ClipboardViewModel(application: Application) : AndroidViewModel(application) {

  private val database = AppDatabase.getDatabase(application)
  private val repository = ClipboardRepository(database.clipboardDao())

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedCategory = MutableStateFlow<String?>(null)
  val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

  private val _uiState = MutableStateFlow(ClipboardUiState())
  val uiState: StateFlow<ClipboardUiState> = _uiState.asStateFlow()

  val allItems: StateFlow<List<ClipboardItem>> = repository.allClipboardItems.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val predefinedCategories = listOf("All", "General", "Code", "Link", "Note", "Password", "Snippets")

  init {
    viewModelScope.launch {
      val existing = repository.allClipboardItems.first()
      if (existing.isEmpty()) {
        repository.insertAll(
          listOf(
            ClipboardItem(
              content = "git commit -m 'feat: implement Room database and ClipboardViewModel with Compose LazyColumn'",
              category = "Code",
              timestamp = System.currentTimeMillis() - 600000
            ),
            ClipboardItem(
              content = "https://developer.android.com/training/data-storage/room",
              category = "Link",
              timestamp = System.currentTimeMillis() - 3600000
            ),
            ClipboardItem(
              content = "Meeting agenda: Discuss offline-first Room database migration and search indexing.",
              category = "Note",
              timestamp = System.currentTimeMillis() - 7200000
            ),
            ClipboardItem(
              content = "auth-token-prod-2026-xyz-encrypted-key",
              category = "Password",
              timestamp = System.currentTimeMillis() - 14400000
            ),
            ClipboardItem(
              content = "Welcome to Clipboard Vault! You can search clips, filter by category, add new items, and copy with one tap.",
              category = "General",
              timestamp = System.currentTimeMillis() - 86400000
            )
          )
        )
      }
    }
  }

  // Combine items, search query, and category filter for reactive filtering
  val filteredItems: StateFlow<List<ClipboardItem>> = combine(
    repository.allClipboardItems,
    _searchQuery,
    _selectedCategory
  ) { items, query, category ->
    items.filter { item ->
      val matchesQuery = query.isBlank() ||
        item.content.contains(query, ignoreCase = true) ||
        item.category.contains(query, ignoreCase = true)

      val matchesCategory = category == null || category == "All" ||
        item.category.equals(category, ignoreCase = true)

      matchesQuery && matchesCategory
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
    _uiState.value = _uiState.value.copy(searchQuery = query)
  }

  fun setSelectedCategory(category: String?) {
    val effectiveCategory = if (category == "All") null else category
    _selectedCategory.value = effectiveCategory
    _uiState.value = _uiState.value.copy(selectedCategory = effectiveCategory)
  }

  fun setContentInput(text: String) {
    _uiState.value = _uiState.value.copy(contentInput = text)
  }

  fun setCategoryInput(category: String) {
    _uiState.value = _uiState.value.copy(categoryInput = category)
  }

  fun setAddCardExpanded(expanded: Boolean) {
    _uiState.value = _uiState.value.copy(isAddCardExpanded = expanded)
  }

  fun addOrUpdateClipboardItem(
    content: String = _uiState.value.contentInput,
    category: String = _uiState.value.categoryInput
  ) {
    if (content.isBlank()) {
      showSnackbar("Content cannot be empty")
      return
    }

    viewModelScope.launch {
      val editing = _uiState.value.editingItem
      if (editing != null) {
        val updated = editing.copy(
          content = content.trim(),
          category = category.trim().ifBlank { "General" },
          timestamp = System.currentTimeMillis()
        )
        repository.update(updated)
        _uiState.value = _uiState.value.copy(
          contentInput = "",
          categoryInput = "General",
          editingItem = null,
          isAddCardExpanded = false,
          snackbarMessage = "Clipboard item updated"
        )
      } else {
        val newItem = ClipboardItem(
          content = content.trim(),
          category = category.trim().ifBlank { "General" },
          timestamp = System.currentTimeMillis()
        )
        repository.insert(newItem)
        _uiState.value = _uiState.value.copy(
          contentInput = "",
          categoryInput = "General",
          isAddCardExpanded = false,
          snackbarMessage = "Item saved to Clipboard Database"
        )
      }
    }
  }

  fun deleteClipboardItem(item: ClipboardItem) {
    viewModelScope.launch {
      repository.delete(item)
      showSnackbar("Deleted clipboard item")
    }
  }

  fun startEditing(item: ClipboardItem) {
    _uiState.value = _uiState.value.copy(
      editingItem = item,
      contentInput = item.content,
      categoryInput = item.category,
      isAddCardExpanded = true
    )
  }

  fun cancelEditing() {
    _uiState.value = _uiState.value.copy(
      editingItem = null,
      contentInput = "",
      categoryInput = "General",
      isAddCardExpanded = false
    )
  }

  fun showSnackbar(message: String) {
    _uiState.value = _uiState.value.copy(snackbarMessage = message)
  }

  fun clearSnackbar() {
    _uiState.value = _uiState.value.copy(snackbarMessage = null)
  }
}
