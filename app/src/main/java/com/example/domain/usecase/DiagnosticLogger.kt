package com.example.domain.usecase

import com.example.domain.model.DiagnosticLogEntry
import com.example.domain.model.LogComponent
import com.example.domain.model.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticLogger {

  private val _logs = MutableStateFlow<List<DiagnosticLogEntry>>(emptyList())
  val logs: StateFlow<List<DiagnosticLogEntry>> = _logs.asStateFlow()

  private const val MAX_LOG_ENTRIES = 300

  init {
    log(
      severity = LogSeverity.INFO,
      component = LogComponent.STORAGE,
      message = "Diagnostic Logging System initialized.",
      details = "Ready for environment event capture."
    )
  }

  fun log(
    severity: LogSeverity,
    component: LogComponent,
    message: String,
    details: String? = null
  ) {
    val entry = DiagnosticLogEntry(
      severity = severity,
      component = component,
      message = message,
      details = details
    )
    val current = _logs.value.toMutableList()
    current.add(0, entry)
    if (current.size > MAX_LOG_ENTRIES) {
      _logs.value = current.take(MAX_LOG_ENTRIES)
    } else {
      _logs.value = current
    }
  }

  fun logError(component: LogComponent, message: String, throwable: Throwable?) {
    val stackTraceString = throwable?.stackTraceToString()?.take(500)
    log(
      severity = LogSeverity.ERROR,
      component = component,
      message = message,
      details = stackTraceString ?: throwable?.localizedMessage
    )
  }

  fun logAudit(action: String, role: String, details: String) {
    log(
      severity = LogSeverity.AUDIT,
      component = LogComponent.SECURITY,
      message = "AUDIT: $action by [$role]",
      details = details
    )
  }

  fun clearLogs() {
    _logs.value = emptyList()
    log(
      severity = LogSeverity.INFO,
      component = LogComponent.STORAGE,
      message = "Diagnostic logs cleared by user."
    )
  }

  fun exportLogsAsJson(): String {
    val root = JSONObject()
    root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
    root.put("totalEntries", _logs.value.size)

    val array = JSONArray()
    for (item in _logs.value) {
      val logObj = JSONObject()
      logObj.put("id", item.id)
      logObj.put("timestamp", item.timestamp)
      logObj.put("severity", item.severity.name)
      logObj.put("component", item.component.name)
      logObj.put("message", item.message)
      if (item.details != null) {
        logObj.put("details", item.details)
      }
      array.put(logObj)
    }
    root.put("logs", array)
    return root.toString(2)
  }
}
