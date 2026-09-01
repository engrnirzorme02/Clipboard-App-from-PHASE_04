package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseAutoSaveManager
import com.example.data.firebase.FirebaseSyncStatus
import com.example.data.local.AppDatabase
import com.example.data.repository.BackupRepository
import com.example.data.repository.ClipRepository
import com.example.data.repository.DuplicateStrategy
import com.example.data.repository.ImportResult
import com.example.data.repository.NoteRepository
import com.example.domain.model.AutomationRule
import com.example.domain.model.CaptureResult
import com.example.domain.model.ClipItem
import com.example.domain.model.ClipSource
import com.example.domain.model.DiagnosticLogEntry
import com.example.domain.model.EnvironmentConfig
import com.example.domain.model.EnvironmentType
import com.example.domain.model.FormatValidationResult
import com.example.domain.model.LogComponent
import com.example.domain.model.LogSeverity
import com.example.domain.model.SensitivityLevel
import com.example.domain.model.SyncState
import com.example.domain.model.UserRole
import com.example.domain.model.VaultNote
import com.example.domain.usecase.AiAssistantService
import com.example.domain.usecase.AiTaskType
import com.example.domain.usecase.AiTransformResult
import com.example.domain.usecase.AutomationEngine
import com.example.domain.usecase.CaptureClipUseCase
import com.example.domain.usecase.DataValidator
import com.example.domain.usecase.DiagnosticLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VaultScreen(val title: String) {
  VAULT("Vault"),
  CAPTURE("Capture"),
  NOTES("Notes"),
  SEARCH("Search"),
  SETTINGS("Settings"),
  CLIPBOARD("Clipboard")
}

enum class VaultFilter(val label: String) {
  ALL("All Clips"),
  PINNED("Pinned"),
  SENSITIVE("Sensitive"),
  ARCHIVED("Archived")
}

data class VaultUiState(
  val currentScreen: VaultScreen = VaultScreen.CLIPBOARD,
  val captureInput: String = "",
  val captureTitle: String = "",
  val captureTags: List<String> = emptyList(),
  val captureIsSensitive: Boolean = false,
  val lastCaptureMessage: String? = null,
  val duplicateDetectedClip: ClipItem? = null,
  val vaultFilter: VaultFilter = VaultFilter.ALL,
  val selectedTagFilter: String? = null,
  val searchQuery: String = "",
  val activeClipDetail: ClipItem? = null,
  val activeNoteDetail: VaultNote? = null,
  val isCreatingNote: Boolean = false,
  val snackbarMessage: String? = null,
  val aiTransformResult: AiTransformResult? = null,
  val isAiLoading: Boolean = false,
  val importPreview: ImportResult.Preview? = null,
  val importMessage: String? = null,
  val currentRole: UserRole = UserRole.ADMIN,
  val environmentConfig: EnvironmentConfig = EnvironmentConfig.DEV_PROFILE,
  val formatValidation: FormatValidationResult = DataValidator.inspectAndValidate(""),
  val isDiagnosticModalOpen: Boolean = false,
  val selectedSeverityFilter: LogSeverity? = null,
  val diagnosticSearchQuery: String = "",
  val automationSummary: AutomationEngine.AutomationExecutionSummary? = null,
  val isAutomationRunning: Boolean = false
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

  private val database = AppDatabase.getDatabase(application)
  private val clipRepository = ClipRepository(database.clipDao())
  private val noteRepository = NoteRepository(database.noteDao())
  private val captureUseCase = CaptureClipUseCase(clipRepository)
  private val backupRepository = BackupRepository(clipRepository, noteRepository)
  private val aiService = AiAssistantService()
  private val firebaseAutoSaveManager = FirebaseAutoSaveManager(
    context = application,
    clipRepository = clipRepository,
    noteRepository = noteRepository,
    scope = viewModelScope
  )

  private val _uiState = MutableStateFlow(VaultUiState())
  val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

  val firebaseSyncStatus: StateFlow<FirebaseSyncStatus> = firebaseAutoSaveManager.syncStatus
  val diagnosticLogs: StateFlow<List<DiagnosticLogEntry>> = DiagnosticLogger.logs

  val allClips: StateFlow<List<ClipItem>> = clipRepository.allClips.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val activeClips: StateFlow<List<ClipItem>> = clipRepository.activeClips.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val archivedClips: StateFlow<List<ClipItem>> = clipRepository.archivedClips.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val activeNotes: StateFlow<List<VaultNote>> = noteRepository.activeNotes.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val clipCount: StateFlow<Int> = clipRepository.clipCount.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = 0
  )

  val noteCount: StateFlow<Int> = noteRepository.noteCount.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = 0
  )

  init {
    DiagnosticLogger.log(
      severity = LogSeverity.INFO,
      component = LogComponent.STORAGE,
      message = "VaultViewModel ready with profile: ${_uiState.value.environmentConfig.type.displayName}",
      details = "Current role: ${_uiState.value.currentRole.title}"
    )

    viewModelScope.launch {
      val existing = clipRepository.getAllSnapshot()
      if (existing.isEmpty()) {
        clipRepository.insertClips(
          listOf(
            ClipItem(
              title = "Git Commit Template",
              text = "git commit -m 'feat: implement Room database and Compose LazyColumn'",
              normalizedHash = "hash_sample_code_1",
              tags = listOf("code", "git"),
              source = ClipSource.KEYBOARD,
              pinned = true,
              sensitivity = SensitivityLevel.NORMAL
            ),
            ClipItem(
              title = "Android Architecture Guide",
              text = "https://developer.android.com/topic/architecture/data-layer",
              normalizedHash = "hash_sample_link_1",
              tags = listOf("link", "android", "docs"),
              source = ClipSource.CLIPBOARD,
              pinned = false,
              sensitivity = SensitivityLevel.NORMAL
            ),
            ClipItem(
              title = "API Secret Key (Dev)",
              text = "sk_test_51MzXk098a87b6c5d4e3f2a1z_token",
              normalizedHash = "hash_sample_secret_1",
              tags = listOf("secret", "api"),
              source = ClipSource.KEYBOARD,
              pinned = false,
              sensitivity = SensitivityLevel.SENSITIVE
            ),
            ClipItem(
              title = "Welcome to Clipboard Vault",
              text = "Welcome to your personal Clipboard Vault! You can rapidly capture text, organize with tags, filter sensitive items, convert clips into formatted notes, and export JSON backups.",
              normalizedHash = "hash_sample_general_1",
              tags = listOf("guide", "important"),
              source = ClipSource.KEYBOARD,
              pinned = false,
              sensitivity = SensitivityLevel.NORMAL
            )
          )
        )
      }

      val existingNotes = noteRepository.getAllSnapshot()
      if (existingNotes.isEmpty()) {
        noteRepository.insertNote(
          VaultNote(
            title = "Getting Started with Vault",
            content = "1. Use Capture tab or Android Share menu to save text instantly.\n2. Pin important clips to keep them at the top.\n3. Mark items as Sensitive for restricted display.\n4. Use AI Assistant inside Clip Details to summarize or extract action items.",
            tags = listOf("guide", "notes")
          )
        )
      }
    }
  }

  // Navigation
  fun navigateTo(screen: VaultScreen) {
    _uiState.value = _uiState.value.copy(currentScreen = screen)
  }

  // Role Switching & Access Control
  fun switchRole(newRole: UserRole) {
    _uiState.value = _uiState.value.copy(currentRole = newRole)
    DiagnosticLogger.logAudit(
      action = "SWITCH_ROLE",
      role = newRole.name,
      details = "Switched active security profile to ${newRole.title}"
    )
    showSnackbar("Active role set to: ${newRole.title}")
  }

  // Environment Profile Switching
  fun switchEnvironment(envType: EnvironmentType) {
    if (!_uiState.value.currentRole.canChangeEnv) {
      showSnackbar("Permission Denied: Administrator role required to switch environments.")
      DiagnosticLogger.log(
        severity = LogSeverity.WARN,
        component = LogComponent.SECURITY,
        message = "Unauthorized environment switch attempt by [${_uiState.value.currentRole.name}]"
      )
      return
    }
    val config = EnvironmentConfig.forType(envType)
    _uiState.value = _uiState.value.copy(environmentConfig = config)
    DiagnosticLogger.log(
      severity = LogSeverity.INFO,
      component = LogComponent.ENVIRONMENT,
      message = "Environment switched to ${envType.displayName}",
      details = "Base URL: ${config.apiBaseUrl}, Retention: ${config.autoScrubbingRetentionDays}d, Sync: ${config.syncIntervalSeconds}s"
    )
    showSnackbar("Environment switched to ${envType.displayName}")
  }

  // Capture & Input Validation
  fun updateCaptureInput(text: String) {
    val previousText = _uiState.value.captureInput
    // A sudden jump in length usually indicates a paste action, rather than single keystrokes
    val isPaste = text.length - previousText.length > 2

    val validation = DataValidator.inspectAndValidate(text)
    val autoSensitive = if (validation.isSensitiveCandidate) true else _uiState.value.captureIsSensitive
    _uiState.value = _uiState.value.copy(
      captureInput = text,
      formatValidation = validation,
      captureIsSensitive = autoSensitive,
      lastCaptureMessage = null,
      duplicateDetectedClip = null
    )

    if (isPaste && text.isNotBlank()) {
      saveCapture(ClipSource.KEYBOARD, forceAllowDuplicate = false)
    }
  }

  fun updateCaptureTitle(title: String) {
    _uiState.value = _uiState.value.copy(captureTitle = title)
  }

  fun toggleCaptureTag(tag: String) {
    val current = _uiState.value.captureTags.toMutableList()
    if (current.contains(tag)) {
      current.remove(tag)
    } else {
      current.add(tag)
    }
    _uiState.value = _uiState.value.copy(captureTags = current)
  }

  fun toggleCaptureSensitivity() {
    _uiState.value = _uiState.value.copy(captureIsSensitive = !_uiState.value.captureIsSensitive)
  }

  fun saveCapture(source: ClipSource = ClipSource.KEYBOARD, forceAllowDuplicate: Boolean = false) {
    if (!_uiState.value.currentRole.canCapture) {
      showSnackbar("Permission Denied: Auditor role is read-only.")
      return
    }

    val input = _uiState.value.captureInput
    val title = _uiState.value.captureTitle
    val tags = _uiState.value.captureTags
    val sensitivity = if (_uiState.value.captureIsSensitive) SensitivityLevel.SENSITIVE else null

    // Enforce strict environment validation if active
    if (_uiState.value.environmentConfig.strictPayloadValidation && !_uiState.value.formatValidation.isValid) {
      _uiState.value = _uiState.value.copy(
        lastCaptureMessage = "Validation Error: ${_uiState.value.formatValidation.validationMessage}"
      )
      DiagnosticLogger.log(
        severity = LogSeverity.WARN,
        component = LogComponent.CAPTURE,
        message = "Strict payload validation blocked invalid input: ${_uiState.value.formatValidation.validationMessage}"
      )
      return
    }

    viewModelScope.launch {
      val result = captureUseCase.execute(
        rawText = input,
        title = title.ifBlank { null },
        source = source,
        userTags = tags,
        allowDuplicate = forceAllowDuplicate,
        forceSensitivity = sensitivity
      )

      when (result) {
        is CaptureResult.Success -> {
          firebaseAutoSaveManager.autoSaveClip(result.clip)
          _uiState.value = _uiState.value.copy(
            captureInput = "",
            captureTitle = "",
            captureTags = emptyList(),
            captureIsSensitive = false,
            formatValidation = DataValidator.inspectAndValidate(""),
            duplicateDetectedClip = null,
            lastCaptureMessage = "Saved locally [${result.clip.shortCode}]",
            snackbarMessage = "Clip saved & Auto-Saved to Cloud [${result.clip.shortCode}]"
          )
        }
        is CaptureResult.Duplicate -> {
          _uiState.value = _uiState.value.copy(
            duplicateDetectedClip = result.existingClip,
            lastCaptureMessage = "Duplicate detected: Already saved as [${result.existingClip.shortCode}]"
          )
        }
        is CaptureResult.Rejected -> {
          _uiState.value = _uiState.value.copy(
            lastCaptureMessage = "Capture rejected: ${result.reason}"
          )
        }
      }
    }
  }

  fun pasteFromClipboard(clipboardText: String) {
    if (clipboardText.isNotBlank()) {
      updateCaptureInput(clipboardText)
    }
  }

  // Vault Actions
  fun setVaultFilter(filter: VaultFilter) {
    _uiState.value = _uiState.value.copy(vaultFilter = filter)
  }

  fun setVaultTagFilter(tag: String?) {
    _uiState.value = _uiState.value.copy(selectedTagFilter = tag)
  }

  fun selectClipDetail(clip: ClipItem?) {
    _uiState.value = _uiState.value.copy(activeClipDetail = clip, aiTransformResult = null)
  }

  fun togglePinClip(clip: ClipItem) {
    if (!_uiState.value.currentRole.canEdit) {
      showSnackbar("Permission Denied: Read-only access.")
      return
    }
    viewModelScope.launch {
      clipRepository.togglePin(clip.id, clip.pinned)
      DiagnosticLogger.log(
        severity = LogSeverity.INFO,
        component = LogComponent.STORAGE,
        message = "Toggled pin state for ${clip.shortCode} -> ${!clip.pinned}"
      )
      showSnackbar(if (!clip.pinned) "Pinned [${clip.shortCode}]" else "Unpinned [${clip.shortCode}]")
    }
  }

  fun toggleArchiveClip(clip: ClipItem) {
    if (!_uiState.value.currentRole.canEdit) {
      showSnackbar("Permission Denied: Read-only access.")
      return
    }
    viewModelScope.launch {
      clipRepository.toggleArchive(clip.id, clip.archived)
      showSnackbar(if (!clip.archived) "Archived [${clip.shortCode}]" else "Restored [${clip.shortCode}]")
    }
  }

  fun deleteClip(clip: ClipItem) {
    if (!_uiState.value.currentRole.canDelete) {
      showSnackbar("Permission Denied: Cannot delete items in read-only mode.")
      return
    }
    viewModelScope.launch {
      clipRepository.deleteClip(clip.id)
      firebaseAutoSaveManager.deleteClipFromCloud(clip.id)
      if (_uiState.value.activeClipDetail?.id == clip.id) {
        _uiState.value = _uiState.value.copy(activeClipDetail = null)
      }
      DiagnosticLogger.log(
        severity = LogSeverity.INFO,
        component = LogComponent.STORAGE,
        message = "Deleted clip [${clip.shortCode}]"
      )
      showSnackbar("Deleted [${clip.shortCode}]")
    }
  }

  fun updateClipText(clipId: String, newTitle: String, newText: String, tags: List<String>) {
    if (!_uiState.value.currentRole.canEdit) {
      showSnackbar("Permission Denied: Read-only access.")
      return
    }
    viewModelScope.launch {
      val existing = _uiState.value.activeClipDetail
      if (existing != null && existing.id == clipId) {
        val updated = existing.copy(
          title = newTitle.ifBlank { null },
          text = newText,
          tags = tags,
          updatedAt = System.currentTimeMillis()
        )
        clipRepository.updateClip(updated)
        firebaseAutoSaveManager.autoSaveClip(updated)
        _uiState.value = _uiState.value.copy(activeClipDetail = updated)
        DiagnosticLogger.log(
          severity = LogSeverity.INFO,
          component = LogComponent.STORAGE,
          message = "Updated clip [${existing.shortCode}]"
        )
        showSnackbar("Updated & Synced [${existing.shortCode}]")
      }
    }
  }

  fun convertClipToNote(clip: ClipItem) {
    if (!_uiState.value.currentRole.canEdit) {
      showSnackbar("Permission Denied: Read-only access.")
      return
    }
    viewModelScope.launch {
      val newNote = VaultNote(
        title = clip.title ?: "Note from ${clip.shortCode}",
        content = clip.text,
        tags = clip.tags,
        sourceClipId = clip.id
      )
      noteRepository.insertNote(newNote)
      firebaseAutoSaveManager.autoSaveNote(newNote)
      DiagnosticLogger.log(
        severity = LogSeverity.INFO,
        component = LogComponent.STORAGE,
        message = "Converted ${clip.shortCode} into VaultNote"
      )
      showSnackbar("Converted ${clip.shortCode} into a Note")
      navigateTo(VaultScreen.NOTES)
    }
  }

  // Notes Actions
  fun selectNoteDetail(note: VaultNote?) {
    _uiState.value = _uiState.value.copy(activeNoteDetail = note)
  }

  fun openCreateNote() {
    if (!_uiState.value.currentRole.canEdit) {
      showSnackbar("Permission Denied: Read-only mode.")
      return
    }
    _uiState.value = _uiState.value.copy(isCreatingNote = true, activeNoteDetail = null)
  }

  fun closeCreateNote() {
    _uiState.value = _uiState.value.copy(isCreatingNote = false)
  }

  fun saveNote(title: String, content: String, tags: List<String>) {
    if (content.isBlank()) return
    if (!_uiState.value.currentRole.canEdit) {
      showSnackbar("Permission Denied: Read-only mode.")
      return
    }
    viewModelScope.launch {
      val note = VaultNote(
        title = if (title.isBlank()) "Untitled Note" else title.trim(),
        content = content.trim(),
        tags = tags
      )
      noteRepository.insertNote(note)
      firebaseAutoSaveManager.autoSaveNote(note)
      _uiState.value = _uiState.value.copy(isCreatingNote = false)
      DiagnosticLogger.log(
        severity = LogSeverity.INFO,
        component = LogComponent.STORAGE,
        message = "Created Note: ${note.title}"
      )
      showSnackbar("Note saved & Auto-Saved to Firebase")
    }
  }

  fun updateNote(note: VaultNote, newTitle: String, newContent: String, tags: List<String>) {
    if (!_uiState.value.currentRole.canEdit) {
      showSnackbar("Permission Denied: Read-only mode.")
      return
    }
    viewModelScope.launch {
      val updated = note.copy(
        title = if (newTitle.isBlank()) "Untitled Note" else newTitle.trim(),
        content = newContent.trim(),
        tags = tags,
        updatedAt = System.currentTimeMillis()
      )
      noteRepository.updateNote(updated)
      firebaseAutoSaveManager.autoSaveNote(updated)
      _uiState.value = _uiState.value.copy(activeNoteDetail = updated)
      showSnackbar("Note updated & Synced")
    }
  }

  fun deleteNote(note: VaultNote) {
    if (!_uiState.value.currentRole.canDelete) {
      showSnackbar("Permission Denied: Read-only mode.")
      return
    }
    viewModelScope.launch {
      noteRepository.deleteNote(note.id)
      firebaseAutoSaveManager.deleteNoteFromCloud(note.id)
      _uiState.value = _uiState.value.copy(activeNoteDetail = null)
      DiagnosticLogger.log(
        severity = LogSeverity.INFO,
        component = LogComponent.STORAGE,
        message = "Deleted Note: ${note.title}"
      )
      showSnackbar("Note deleted")
    }
  }

  // Search
  fun updateSearchQuery(query: String) {
    _uiState.value = _uiState.value.copy(searchQuery = query)
  }

  // AI Transform
  fun runAiTransformation(text: String, taskType: AiTaskType) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isAiLoading = true, aiTransformResult = null)
      val result = aiService.processText(text, taskType)
      _uiState.value = _uiState.value.copy(isAiLoading = false, aiTransformResult = result)
    }
  }

  // Automation Engine
  fun runMaintenanceAutomation() {
    if (!_uiState.value.currentRole.canRunAutomation) {
      showSnackbar("Permission Denied: Role cannot trigger automation tasks.")
      return
    }
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isAutomationRunning = true)
      val summary = AutomationEngine.executeMaintenanceRoutine(clipRepository, _uiState.value.environmentConfig)
      _uiState.value = _uiState.value.copy(
        isAutomationRunning = false,
        automationSummary = summary
      )
      showSnackbar("Automated maintenance complete: ${summary.itemsScrubbed} items scrubbed")
    }
  }

  // Diagnostic Logs Management
  fun openDiagnosticModal() {
    _uiState.value = _uiState.value.copy(isDiagnosticModalOpen = true)
  }

  fun closeDiagnosticModal() {
    _uiState.value = _uiState.value.copy(isDiagnosticModalOpen = false)
  }

  fun setDiagnosticSeverityFilter(severity: LogSeverity?) {
    _uiState.value = _uiState.value.copy(selectedSeverityFilter = severity)
  }

  fun updateDiagnosticSearchQuery(query: String) {
    _uiState.value = _uiState.value.copy(diagnosticSearchQuery = query)
  }

  fun clearDiagnosticLogs() {
    DiagnosticLogger.clearLogs()
    showSnackbar("Diagnostic logs cleared")
  }

  fun exportDiagnosticLogs(): String {
    return DiagnosticLogger.exportLogsAsJson()
  }

  // Plain Text Export for Easy Portability
  suspend fun exportVaultAsPlainText(): String {
    val clips = clipRepository.getAllSnapshot()
    val notes = noteRepository.getAllSnapshot()
    val clipboardItems = database.clipboardDao().getAllClipboardItemsSnapshot()

    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

    val sb = StringBuilder()
    sb.append("====================================================\n")
    sb.append("      CLIPBOARD VAULT - COMPLETE TEXT EXPORT        \n")
    sb.append("Exported: ${dateFormat.format(java.util.Date())}\n")
    sb.append("Summary: ${clips.size} clips, ${clipboardItems.size} clipboard entries, ${notes.size} notes\n")
    sb.append("====================================================\n\n")

    if (clipboardItems.isNotEmpty()) {
      sb.append("=== CAPTURED CLIPBOARD ENTRIES (${clipboardItems.size}) ===\n\n")
      clipboardItems.forEachIndexed { i, item ->
        sb.append("[Item ${i + 1}] Category: ${item.category} | Date: ${item.formattedTimestamp}\n")
        if (item.tags.isNotEmpty()) {
          sb.append("Tags: #${item.tags.joinToString(" #")}\n")
        }
        if (item.isPinned) {
          sb.append("Status: [PINNED]\n")
        }
        sb.append("Content:\n${item.content}\n\n----------------------------------------------------\n\n")
      }
    }

    if (clips.isNotEmpty()) {
      sb.append("=== VAULT CLIPS (${clips.size}) ===\n\n")
      clips.forEachIndexed { i, clip ->
        sb.append("[Clip ${i + 1}] Code: ${clip.shortCode} | Title: ${clip.title ?: "Untitled"}\n")
        if (clip.tags.isNotEmpty()) {
          sb.append("Tags: #${clip.tags.joinToString(" #")}\n")
        }
        sb.append("Created: ${dateFormat.format(java.util.Date(clip.createdAt))} | Source: ${clip.source.name}\n")
        sb.append("Text:\n${clip.text}\n\n----------------------------------------------------\n\n")
      }
    }

    if (notes.isNotEmpty()) {
      sb.append("=== VAULT NOTES (${notes.size}) ===\n\n")
      notes.forEachIndexed { i, note ->
        sb.append("[Note ${i + 1}] Title: ${note.title}\n")
        if (note.tags.isNotEmpty()) {
          sb.append("Tags: #${note.tags.joinToString(" #")}\n")
        }
        sb.append("Content:\n${note.content}\n\n----------------------------------------------------\n\n")
      }
    }

    return sb.toString()
  }

  // WorkManager Encrypted Periodic Database Export
  val encryptedBackupStatus = com.example.service.DatabaseBackupScheduler.statusFlow

  fun refreshEncryptedBackupStatus() {
    com.example.service.DatabaseBackupScheduler.refreshStatus(getApplication())
  }

  fun setPeriodicEncryptedBackup(enabled: Boolean, intervalHours: Long = 6) {
    com.example.service.DatabaseBackupScheduler.setAutoBackupEnabled(getApplication(), enabled, intervalHours)
    showSnackbar(if (enabled) "Scheduled periodic AES-256 encrypted Room DB export (every ${intervalHours}h)" else "Periodic database backup disabled")
  }

  fun triggerImmediateEncryptedBackup() {
    com.example.service.DatabaseBackupScheduler.triggerImmediateEncryptedBackup(getApplication())
    showSnackbar("Encrypted Room DB export task queued in WorkManager")
    viewModelScope.launch {
      kotlinx.coroutines.delay(1000)
      refreshEncryptedBackupStatus()
    }
  }

  fun setCustomBackupDirectory(path: String) {
    com.example.service.DatabaseBackupScheduler.setCustomExportDirectory(getApplication(), path)
    showSnackbar("Custom backup location updated")
  }

  // Backup & Restore
  suspend fun generateBackupJson(): String {
    DiagnosticLogger.log(
      severity = LogSeverity.INFO,
      component = LogComponent.BACKUP,
      message = "Generating JSON export bundle"
    )
    return backupRepository.exportToJsonString()
  }

  fun previewImportJson(jsonString: String) {
    if (!_uiState.value.currentRole.canImport) {
      showSnackbar("Permission Denied: Administrator role required to import backups.")
      return
    }
    viewModelScope.launch {
      val result = backupRepository.validateAndPreview(jsonString)
      when (result) {
        is ImportResult.Preview -> {
          _uiState.value = _uiState.value.copy(importPreview = result, importMessage = null)
        }
        is ImportResult.Error -> {
          DiagnosticLogger.log(
            severity = LogSeverity.ERROR,
            component = LogComponent.BACKUP,
            message = "Import validation failed: ${result.message}"
          )
          _uiState.value = _uiState.value.copy(importPreview = null, importMessage = result.message)
        }
        else -> {}
      }
    }
  }

  fun executeImport(strategy: DuplicateStrategy) {
    if (!_uiState.value.currentRole.canImport) {
      showSnackbar("Permission Denied: Administrator role required to restore backups.")
      return
    }
    val preview = _uiState.value.importPreview ?: return
    viewModelScope.launch {
      val result = backupRepository.commitImport(preview.validClips, preview.validNotes, strategy)
      when (result) {
        is ImportResult.Success -> {
          _uiState.value = _uiState.value.copy(
            importPreview = null,
            importMessage = "Successfully imported ${result.importedClips} clips and ${result.importedNotes} notes."
          )
          DiagnosticLogger.logAudit(
            action = "RESTORE_BACKUP",
            role = _uiState.value.currentRole.name,
            details = "Imported ${result.importedClips} clips and ${result.importedNotes} notes with strategy $strategy"
          )
          showSnackbar("Backup restored successfully")
        }
        is ImportResult.Error -> {
          DiagnosticLogger.log(
            severity = LogSeverity.ERROR,
            component = LogComponent.BACKUP,
            message = "Restore execution failed: ${result.message}"
          )
          _uiState.value = _uiState.value.copy(importMessage = result.message)
        }
        else -> {}
      }
    }
  }

  fun dismissImportPreview() {
    _uiState.value = _uiState.value.copy(importPreview = null, importMessage = null)
  }

  fun clearAllVaultData() {
    if (!_uiState.value.currentRole.canWipe) {
      showSnackbar("Permission Denied: Administrator role required to wipe vault.")
      return
    }
    viewModelScope.launch {
      clipRepository.clearAll()
      noteRepository.clearAll()
      DiagnosticLogger.logAudit(
        action = "WIPE_VAULT",
        role = _uiState.value.currentRole.name,
        details = "All local clips and notes wiped."
      )
      showSnackbar("All local vault data has been wiped.")
    }
  }

  // Firebase Cloud Auto-Save & Sync
  fun toggleFirebaseAutoSave(enabled: Boolean) {
    firebaseAutoSaveManager.toggleAutoSave(enabled)
    showSnackbar(if (enabled) "Firebase Auto-Save Enabled" else "Firebase Auto-Save Paused")
  }

  fun pushAllToFirebase() {
    viewModelScope.launch {
      showSnackbar("Pushing all records to Firebase Cloud...")
      val (clips, notes) = firebaseAutoSaveManager.syncAllToFirebase()
      showSnackbar("Successfully pushed $clips clips and $notes notes to Cloud")
    }
  }

  fun pullAllFromFirebase() {
    viewModelScope.launch {
      showSnackbar("Restoring records from Firebase Cloud...")
      val (clips, notes) = firebaseAutoSaveManager.pullFromFirebase()
      showSnackbar("Restored $clips clips and $notes notes from Cloud")
    }
  }

  // Snackbar & Notifications
  fun showSnackbar(message: String) {
    _uiState.value = _uiState.value.copy(snackbarMessage = message)
  }

  fun clearSnackbar() {
    _uiState.value = _uiState.value.copy(snackbarMessage = null)
  }
}
