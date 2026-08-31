package com.example.domain.model

enum class UserRole(
  val title: String,
  val description: String,
  val canCapture: Boolean,
  val canEdit: Boolean,
  val canDelete: Boolean,
  val canExport: Boolean,
  val canImport: Boolean,
  val canWipe: Boolean,
  val canChangeEnv: Boolean,
  val canRunAutomation: Boolean
) {
  ADMIN(
    title = "System Administrator",
    description = "Full control: Configuration, Automation, Export/Import, and Vault Wipe.",
    canCapture = true,
    canEdit = true,
    canDelete = true,
    canExport = true,
    canImport = true,
    canWipe = true,
    canChangeEnv = true,
    canRunAutomation = true
  ),
  EDITOR(
    title = "Standard Editor",
    description = "Create, edit, tag, and organize clips and notes. Cannot wipe or switch environments.",
    canCapture = true,
    canEdit = true,
    canDelete = true,
    canExport = true,
    canImport = false,
    canWipe = false,
    canChangeEnv = false,
    canRunAutomation = true
  ),
  AUDITOR(
    title = "Security Auditor (Read-Only)",
    description = "Inspect vault items, audit trails, and diagnostic logs. Cannot create, edit, or delete items.",
    canCapture = false,
    canEdit = false,
    canDelete = false,
    canExport = true,
    canImport = false,
    canWipe = false,
    canChangeEnv = false,
    canRunAutomation = false
  )
}
