package com.example

import com.example.domain.model.ClipItem
import com.example.domain.model.ClipSource
import com.example.domain.model.DetectedFormatType
import com.example.domain.model.EnvironmentConfig
import com.example.domain.model.EnvironmentType
import com.example.domain.model.LogComponent
import com.example.domain.model.LogSeverity
import com.example.domain.model.UserRole
import com.example.domain.usecase.AutomationEngine
import com.example.domain.usecase.DataValidator
import com.example.domain.usecase.DiagnosticLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EnvironmentCustomizationTest {

  @Before
  fun setup() {
    DiagnosticLogger.clearLogs()
  }

  @Test
  fun testDataValidatorJsonDetection() {
    val validJson = """{"status": "ok", "env": "production", "code": 200}"""
    val result = DataValidator.inspectAndValidate(validJson)
    assertEquals(DetectedFormatType.JSON_OBJECT, result.formatType)
    assertTrue(result.isValid)
  }

  @Test
  fun testDataValidatorUrlDetection() {
    val validUrl = "https://ais-dev-ec59cc5a-ca64-4ed6-bdfa-4213382f7328.antigravity.run/api/v1/health"
    val result = DataValidator.inspectAndValidate(validUrl)
    assertEquals(DetectedFormatType.URL_LINK, result.formatType)
    assertTrue(result.isValid)
  }

  @Test
  fun testDataValidatorSecretDetection() {
    val secretText = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.doNotLeakThisSignature"
    val result = DataValidator.inspectAndValidate(secretText)
    assertEquals(DetectedFormatType.SECRET_CREDENTIAL, result.formatType)
  }

  @Test
  fun testUserRolePermissions() {
    // Admin has full permissions
    assertTrue(UserRole.ADMIN.canCapture)
    assertTrue(UserRole.ADMIN.canEdit)
    assertTrue(UserRole.ADMIN.canDelete)
    assertTrue(UserRole.ADMIN.canImport)
    assertTrue(UserRole.ADMIN.canChangeEnv)
    assertTrue(UserRole.ADMIN.canRunAutomation)

    // Auditor is read-only
    assertFalse(UserRole.AUDITOR.canCapture)
    assertFalse(UserRole.AUDITOR.canEdit)
    assertFalse(UserRole.AUDITOR.canDelete)
    assertFalse(UserRole.AUDITOR.canImport)
    assertFalse(UserRole.AUDITOR.canWipe)

    // Editor can capture and edit, but not wipe or change environment
    assertTrue(UserRole.EDITOR.canCapture)
    assertTrue(UserRole.EDITOR.canEdit)
    assertFalse(UserRole.EDITOR.canWipe)
    assertFalse(UserRole.EDITOR.canChangeEnv)
  }

  @Test
  fun testEnvironmentProfiles() {
    val dev = EnvironmentConfig.DEV_PROFILE
    assertEquals(EnvironmentType.DEVELOPMENT, dev.type)
    assertEquals(5, dev.syncIntervalSeconds)
    assertEquals(7, dev.autoScrubbingRetentionDays)

    val staging = EnvironmentConfig.STAGING_PROFILE
    assertEquals(EnvironmentType.STAGING, staging.type)
    assertEquals(30, staging.syncIntervalSeconds)

    val prod = EnvironmentConfig.PROD_PROFILE
    assertEquals(EnvironmentType.PRODUCTION, prod.type)
    assertTrue(prod.strictPayloadValidation)
    assertEquals(90, prod.autoScrubbingRetentionDays)
  }

  @Test
  fun testDiagnosticLoggerRecording() {
    DiagnosticLogger.log(
      severity = LogSeverity.INFO,
      component = LogComponent.SECURITY,
      message = "Validation pipeline initialized"
    )

    DiagnosticLogger.logAudit(
      action = "SWITCH_ROLE",
      role = UserRole.ADMIN.name,
      details = "Switched to ADMIN for operational control"
    )

    val logs = DiagnosticLogger.logs.value
    assertTrue(logs.size >= 2)
    assertEquals(LogSeverity.AUDIT, logs[0].severity)

    val exportedJson = DiagnosticLogger.exportLogsAsJson()
    assertTrue(exportedJson.contains("SWITCH_ROLE"))
    assertTrue(exportedJson.contains("Validation pipeline initialized"))
  }

  @Test
  fun testAutomationEngineScrubbingRule() {
    val now = System.currentTimeMillis()
    val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000L)
    val fortyDaysAgo = now - (40 * 24 * 60 * 60 * 1000L)

    val recentClip = ClipItem(
      text = "Recent clip",
      normalizedHash = "hash1",
      pinned = false,
      createdAt = twoDaysAgo,
      source = ClipSource.KEYBOARD
    )

    val stalePinnedClip = ClipItem(
      text = "Important pinned clip",
      normalizedHash = "hash2",
      pinned = true,
      createdAt = fortyDaysAgo,
      source = ClipSource.KEYBOARD
    )

    val staleUnpinnedClip = ClipItem(
      text = "Old unpinned clip",
      normalizedHash = "hash3",
      pinned = false,
      createdAt = fortyDaysAgo,
      source = ClipSource.KEYBOARD
    )

    val clips = listOf(recentClip, stalePinnedClip, staleUnpinnedClip)
    val retentionDays = 30

    val staleItems = AutomationEngine.identifyStaleClips(clips, retentionDays, now)
    assertEquals(1, staleItems.size)
    assertEquals(staleUnpinnedClip.id, staleItems[0].id)
  }
}
