package com.example.domain.model

enum class EnvironmentType(val displayName: String, val badgeColorHex: String) {
  DEVELOPMENT("Development", "#F59E0B"),
  STAGING("Staging", "#6366F1"),
  PRODUCTION("Production", "#10B981")
}

data class EnvironmentConfig(
  val type: EnvironmentType = EnvironmentType.DEVELOPMENT,
  val apiBaseUrl: String = "https://dev-api.clipboardvault.internal/v1",
  val syncIntervalSeconds: Int = 10,
  val maxLocalClips: Int = 500,
  val debugLoggingEnabled: Boolean = true,
  val strictPayloadValidation: Boolean = false,
  val securityClearanceRequired: Boolean = false,
  val autoScrubbingRetentionDays: Int = 7
) {
  companion object {
    val DEV_PROFILE = EnvironmentConfig(
      type = EnvironmentType.DEVELOPMENT,
      apiBaseUrl = "https://dev-api.clipboardvault.internal/v1",
      syncIntervalSeconds = 5,
      maxLocalClips = 500,
      debugLoggingEnabled = true,
      strictPayloadValidation = false,
      securityClearanceRequired = false,
      autoScrubbingRetentionDays = 7
    )

    val STAGING_PROFILE = EnvironmentConfig(
      type = EnvironmentType.STAGING,
      apiBaseUrl = "https://staging-api.clipboardvault.internal/v1",
      syncIntervalSeconds = 30,
      maxLocalClips = 2000,
      debugLoggingEnabled = true,
      strictPayloadValidation = true,
      securityClearanceRequired = false,
      autoScrubbingRetentionDays = 30
    )

    val PROD_PROFILE = EnvironmentConfig(
      type = EnvironmentType.PRODUCTION,
      apiBaseUrl = "https://vault-api.clipboardvault.prod/v1",
      syncIntervalSeconds = 120,
      maxLocalClips = 10000,
      debugLoggingEnabled = false,
      strictPayloadValidation = true,
      securityClearanceRequired = true,
      autoScrubbingRetentionDays = 90
    )

    fun forType(type: EnvironmentType): EnvironmentConfig {
      return when (type) {
        EnvironmentType.DEVELOPMENT -> DEV_PROFILE
        EnvironmentType.STAGING -> STAGING_PROFILE
        EnvironmentType.PRODUCTION -> PROD_PROFILE
      }
    }
  }
}
