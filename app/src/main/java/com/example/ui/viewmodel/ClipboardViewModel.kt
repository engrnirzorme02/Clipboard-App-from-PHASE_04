package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ClipboardItem
import com.example.data.repository.ClipboardRepository
import com.example.service.ClipboardMonitorService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
  val selectedTag: String? = null,
  val rapidInput: String = "",
  val contentInput: String = "",
  val categoryInput: String = "General",
  val tagsInput: String = "",
  val editingItem: ClipboardItem? = null,
  val isAddCardExpanded: Boolean = false,
  val isMonitoringActive: Boolean = false,
  val lastDeletedItem: ClipboardItem? = null,
  val snackbarMessage: String? = null,
  val isExportingPlainText: Boolean = false,
  val exportedPlainText: String? = null
)

class ClipboardViewModel(application: Application) : AndroidViewModel(application) {

  private val database = AppDatabase.getDatabase(application)
  private val repository = ClipboardRepository(database.clipboardDao())

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedCategory = MutableStateFlow<String?>(null)
  val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

  private val _selectedTag = MutableStateFlow<String?>(null)
  val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

  private val _uiState = MutableStateFlow(
    ClipboardUiState(isMonitoringActive = ClipboardMonitorService.isServiceRunning)
  )
  val uiState: StateFlow<ClipboardUiState> = _uiState.asStateFlow()

  val allItems: StateFlow<List<ClipboardItem>> = repository.allClipboardItems.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val allTags: StateFlow<List<String>> = repository.allClipboardItems.map { items ->
    items.flatMap { it.tags }.distinct().sorted()
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val predefinedCategories = listOf("All", "General", "Code", "Link", "Note", "Password", "Contact", "Snippets")

  init {
    viewModelScope.launch {
      val existing = repository.allClipboardItems.first()
      if (existing.isEmpty()) {
        repository.insertAll(
          listOf(
            ClipboardItem(
              content = "git commit -m 'feat: implement Room database and ClipboardViewModel with Compose LazyColumn'",
              category = "Code",
              timestamp = System.currentTimeMillis() - 600000,
              isPinned = true,
              tags = listOf("git", "terminal", "workflow")
            ),
            ClipboardItem(
              content = "https://developer.android.com/training/data-storage/room",
              category = "Link",
              timestamp = System.currentTimeMillis() - 3600000,
              isPinned = true,
              tags = listOf("docs", "android", "room")
            ),
            ClipboardItem(
              content = "Meeting agenda: Discuss offline-first Room database migration and search indexing.",
              category = "Note",
              timestamp = System.currentTimeMillis() - 7200000,
              isPinned = false,
              tags = listOf("meeting", "work")
            ),
            ClipboardItem(
              content = "auth-token-prod-2026-xyz-encrypted-key",
              category = "Password",
              timestamp = System.currentTimeMillis() - 14400000,
              isPinned = false,
              tags = listOf("security", "tokens")
            ),
            ClipboardItem(
              content = "Welcome to Clipboard Vault! You can search clips, filter by category and custom tags, pin important items, swipe to delete, and auto-capture with the background service.",
              category = "General",
              timestamp = System.currentTimeMillis() - 86400000,
              isPinned = false,
              tags = listOf("guide", "intro")
            )
          )
        )
      }
    }
  }

  // Combine items, search query, category, and tag filter for reactive filtering
  val filteredItems: StateFlow<List<ClipboardItem>> = combine(
    repository.allClipboardItems,
    _searchQuery,
    _selectedCategory,
    _selectedTag
  ) { items, query, category, tag ->
    items.filter { item ->
      val matchesQuery = query.isBlank() ||
        item.content.contains(query, ignoreCase = true) ||
        item.category.contains(query, ignoreCase = true) ||
        item.tags.any { it.contains(query, ignoreCase = true) }

      val matchesCategory = category == null || category == "All" ||
        item.category.equals(category, ignoreCase = true)

      val matchesTag = tag == null || tag == "All" ||
        item.tags.any { it.equals(tag, ignoreCase = true) }

      matchesQuery && matchesCategory && matchesTag
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

  fun setSelectedTag(tag: String?) {
    val effectiveTag = if (tag == "All") null else tag
    _selectedTag.value = effectiveTag
    _uiState.value = _uiState.value.copy(selectedTag = effectiveTag)
  }

  fun handleRapidCapture(text: String) {
    if (text.isBlank()) {
      _uiState.value = _uiState.value.copy(rapidInput = text)
      return
    }

    val trimmed = text.trim()
    val existing = allItems.value.find { it.content.trim() == trimmed }
    
    viewModelScope.launch {
      if (existing != null) {
        showSnackbar("⚠️ Duplicate: Already saved in Vault")
      } else {
        repository.captureNewClipboardText(trimmed)
        showSnackbar("✅ Saved new item to Vault")
      }
      _uiState.value = _uiState.value.copy(rapidInput = "")
    }
  }

  fun setContentInput(text: String) {
    val autoCat = if (_uiState.value.categoryInput == "General" && text.isNotBlank()) {
      ClipboardItem.inferCategory(text)
    } else {
      _uiState.value.categoryInput
    }
    _uiState.value = _uiState.value.copy(contentInput = text, categoryInput = autoCat)
  }

  fun setCategoryInput(category: String) {
    _uiState.value = _uiState.value.copy(categoryInput = category)
  }

  fun setTagsInput(tags: String) {
    _uiState.value = _uiState.value.copy(tagsInput = tags)
  }

  fun setAddCardExpanded(expanded: Boolean) {
    _uiState.value = _uiState.value.copy(isAddCardExpanded = expanded)
  }

  fun toggleMonitoring(context: Context) {
    val newActive = !_uiState.value.isMonitoringActive
    if (newActive) {
      ClipboardMonitorService.start(context)
      showSnackbar("Clipboard background monitoring started")
    } else {
      ClipboardMonitorService.stop(context)
      showSnackbar("Clipboard background monitoring stopped")
    }
    _uiState.value = _uiState.value.copy(isMonitoringActive = newActive)
  }

  fun togglePin(item: ClipboardItem) {
    viewModelScope.launch {
      repository.togglePin(item)
      val msg = if (!item.isPinned) "Pinned to top" else "Unpinned"
      showSnackbar(msg)
    }
  }

  fun addOrUpdateClipboardItem(
    content: String = _uiState.value.contentInput,
    category: String = _uiState.value.categoryInput,
    tagsString: String = _uiState.value.tagsInput
  ) {
    if (content.isBlank()) {
      showSnackbar("Content cannot be empty")
      return
    }

    val parsedTags = ClipboardItem.parseTags(tagsString)

    viewModelScope.launch {
      val editing = _uiState.value.editingItem
      if (editing != null) {
        val updated = editing.copy(
          content = content.trim(),
          category = category.trim().ifBlank { ClipboardItem.inferCategory(content) },
          tags = parsedTags,
          timestamp = System.currentTimeMillis()
        )
        repository.update(updated)
        _uiState.value = _uiState.value.copy(
          contentInput = "",
          categoryInput = "General",
          tagsInput = "",
          editingItem = null,
          isAddCardExpanded = false,
          snackbarMessage = "Clipboard item updated"
        )
      } else {
        val newItem = ClipboardItem(
          content = content.trim(),
          category = category.trim().ifBlank { ClipboardItem.inferCategory(content) },
          tags = parsedTags,
          timestamp = System.currentTimeMillis(),
          isPinned = false
        )
        repository.insert(newItem)
        _uiState.value = _uiState.value.copy(
          contentInput = "",
          categoryInput = "General",
          tagsInput = "",
          isAddCardExpanded = false,
          snackbarMessage = "Item saved to Clipboard Database"
        )
      }
    }
  }

  fun deleteClipboardItem(item: ClipboardItem) {
    viewModelScope.launch {
      repository.delete(item)
      _uiState.value = _uiState.value.copy(
        lastDeletedItem = item,
        snackbarMessage = "Item deleted. Swipe action completed."
      )
    }
  }

  fun undoDelete() {
    val deleted = _uiState.value.lastDeletedItem ?: return
    viewModelScope.launch {
      repository.insert(deleted)
      _uiState.value = _uiState.value.copy(
        lastDeletedItem = null,
        snackbarMessage = "Item restored"
      )
    }
  }

  fun copyToSystemClipboard(context: Context, item: ClipboardItem) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("Vault Copied Text", item.content)
    clipboard?.setPrimaryClip(clip)
    showSnackbar("Copied to system clipboard")
  }

  fun pasteFromClipboard(context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = clipboard?.primaryClip
    if (clip != null && clip.itemCount > 0) {
      val text = clip.getItemAt(0).coerceToText(context)?.toString()
      if (!text.isNullOrBlank()) {
        setContentInput(text)
        setAddCardExpanded(true)
        showSnackbar("Pasted from system clipboard")
      } else {
        showSnackbar("Clipboard is empty")
      }
    } else {
      showSnackbar("Clipboard is empty")
    }
  }

  fun startEditing(item: ClipboardItem) {
    _uiState.value = _uiState.value.copy(
      editingItem = item,
      contentInput = item.content,
      categoryInput = item.category,
      tagsInput = item.tags.joinToString(", "),
      isAddCardExpanded = true
    )
  }

  fun cancelEditing() {
    _uiState.value = _uiState.value.copy(
      editingItem = null,
      contentInput = "",
      categoryInput = "General",
      tagsInput = "",
      isAddCardExpanded = false
    )
  }

  suspend fun generatePlainTextExport(): String {
    return repository.exportToPlainText()
  }

  fun showSnackbar(message: String) {
    _uiState.value = _uiState.value.copy(snackbarMessage = message)
  }

  fun clearSnackbar() {
    _uiState.value = _uiState.value.copy(snackbarMessage = null)
  }
}

