package com.example.domain.usecase

import com.example.data.repository.ClipRepository
import com.example.domain.model.AutomationRule
import com.example.domain.model.ClipItem
import com.example.domain.model.DetectedFormatType
import com.example.domain.model.EnvironmentConfig
import com.example.domain.model.LogComponent
import com.example.domain.model.LogSeverity
import com.example.domain.model.SensitivityLevel

object AutomationEngine {

  val defaultRules = listOf(
    AutomationRule(
      id = "rule_auto_tag",
      name = "Format-Based Auto-Tagging",
      description = "Automatically applies relevant tags (#link, #json, #code, #email) based on content structure.",
      isEnabled = true
    ),
    AutomationRule(
      id = "rule_auto_sensitive",
      name = "Sensitive Credential Classifier",
      description = "Flags tokens, passwords, and private keys as SENSITIVE to enforce clearance gates.",
      isEnabled = true
    ),
    AutomationRule(
      id = "rule_retention_scrub",
      name = "Scheduled Data Retention Scrub",
      description = "Purges unpinned clips older than the current environment's retention threshold.",
      isEnabled = true
    )
  )

  data class AutomationExecutionSummary(
    val rulesEvaluated: Int,
    val rulesTriggered: Int,
    val itemsScrubbed: Int = 0,
    val summaryDetails: List<String> = emptyList()
  )

  fun identifyStaleClips(
    clips: List<ClipItem>,
    retentionDays: Int,
    currentTimestamp: Long = System.currentTimeMillis()
  ): List<ClipItem> {
    val retentionMillis = retentionDays * 24L * 60 * 60 * 1000L
    val cutoffTimestamp = currentTimestamp - retentionMillis
    return clips.filter { !it.pinned && it.createdAt < cutoffTimestamp }
  }

  suspend fun executeMaintenanceRoutine(
    clipRepository: ClipRepository,
    environmentConfig: EnvironmentConfig
  ): AutomationExecutionSummary {
    val details = mutableListOf<String>()
    val allClips = clipRepository.getAllSnapshot()
    val staleClips = identifyStaleClips(allClips, environmentConfig.autoScrubbingRetentionDays)
    var scrubbedCount = 0

    for (clip in staleClips) {
      clipRepository.deleteClip(clip.id)
      scrubbedCount++
    }

    if (scrubbedCount > 0) {
      val msg = "Retention Policy Scrub: Removed $scrubbedCount unpinned items older than ${environmentConfig.autoScrubbingRetentionDays} days."
      details.add(msg)
      DiagnosticLogger.log(
        severity = LogSeverity.INFO,
        component = LogComponent.AUTOMATION,
        message = msg
      )
    } else {
      val msg = "Retention check passed: No stale unpinned items found."
      details.add(msg)
      DiagnosticLogger.log(
        severity = LogSeverity.INFO,
        component = LogComponent.AUTOMATION,
        message = msg
      )
    }

    return AutomationExecutionSummary(
      rulesEvaluated = defaultRules.size,
      rulesTriggered = if (scrubbedCount > 0) 1 else 0,
      itemsScrubbed = scrubbedCount,
      summaryDetails = details
    )
  }
}
