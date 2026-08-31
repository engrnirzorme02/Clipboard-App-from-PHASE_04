package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.CloudSyncState
import com.example.data.firebase.FirebaseSyncStatus
import com.example.domain.model.EnvironmentConfig
import com.example.domain.model.EnvironmentType
import com.example.domain.model.UserRole
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultRose
import com.example.ui.viewmodel.VaultScreen

@Composable
fun TopVaultBar(
  currentScreen: VaultScreen,
  clipCount: Int,
  environmentConfig: EnvironmentConfig = EnvironmentConfig.DEV_PROFILE,
  userRole: UserRole = UserRole.ADMIN,
  firebaseSyncStatus: FirebaseSyncStatus? = null,
  modifier: Modifier = Modifier
) {
  val envColor = when (environmentConfig.type) {
    EnvironmentType.DEVELOPMENT -> VaultAmber
    EnvironmentType.STAGING -> Color(0xFF6366F1)
    EnvironmentType.PRODUCTION -> VaultEmerald
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding(),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = "Vault Icon",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "Clipboard Vault",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.3.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = currentScreen.title,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = " • ${userRole.name}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Firebase Cloud Auto-Save Badge
        if (firebaseSyncStatus != null) {
          val (cloudIcon, cloudColor, cloudText) = when (firebaseSyncStatus.state) {
            CloudSyncState.SYNCED -> Triple(Icons.Default.CloudDone, VaultEmerald, "Cloud")
            CloudSyncState.SYNCING -> Triple(Icons.Default.CloudSync, VaultCyan, "Syncing")
            CloudSyncState.DISCONNECTED_LOCAL_FIRST -> Triple(Icons.Default.CloudQueue, VaultAmber, "Local")
            CloudSyncState.DISABLED -> Triple(Icons.Default.CloudOff, Color.Gray, "Off")
            CloudSyncState.ERROR -> Triple(Icons.Default.CloudOff, VaultRose, "Retry")
          }

          Surface(
            shape = RoundedCornerShape(20.dp),
            color = cloudColor.copy(alpha = 0.15f),
            modifier = Modifier.clip(RoundedCornerShape(20.dp))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = cloudIcon,
                contentDescription = "Cloud Status",
                tint = cloudColor,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = cloudText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                color = cloudColor
              )
            }
          }
        }

        // Environment Profile Badge
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = envColor.copy(alpha = 0.15f),
          modifier = Modifier.clip(RoundedCornerShape(20.dp))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(envColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = environmentConfig.type.displayName.uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
              color = envColor
            )
          }
        }
      }
    }
  }
}

@Composable
fun BottomVaultNavigation(
  currentScreen: VaultScreen,
  onNavigate: (VaultScreen) -> Unit,
  modifier: Modifier = Modifier
) {
  NavigationBar(
    modifier = modifier.testTag("bottom_nav_bar"),
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp
  ) {
    val items = listOf(
      Triple(VaultScreen.CLIPBOARD, Icons.Filled.ContentPaste, Icons.Outlined.ContentPaste),
      Triple(VaultScreen.CAPTURE, Icons.Filled.Search, Icons.Outlined.Search),
      Triple(VaultScreen.VAULT, Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
      Triple(VaultScreen.NOTES, Icons.Filled.EditNote, Icons.Outlined.EditNote),
      Triple(VaultScreen.SETTINGS, Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    items.forEach { (screen, selectedIcon, unselectedIcon) ->
      val isSelected = currentScreen == screen
      NavigationBarItem(
        selected = isSelected,
        onClick = { onNavigate(screen) },
        icon = {
          Icon(
            imageVector = if (isSelected) selectedIcon else unselectedIcon,
            contentDescription = screen.title
          )
        },
        label = {
          Text(
            text = screen.title,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = MaterialTheme.colorScheme.primary,
          selectedTextColor = MaterialTheme.colorScheme.primary,
          indicatorColor = MaterialTheme.colorScheme.primaryContainer,
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
      )
    }
  }
}
