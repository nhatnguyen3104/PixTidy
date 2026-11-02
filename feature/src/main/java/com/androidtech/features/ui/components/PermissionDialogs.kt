package com.androidtech.features.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PermissionRationaleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Storage Permission Required") },
        text = { Text(text = "SnapSweep needs access to your photos and videos to scan and clean junk media, duplicate files, and screenshots efficiently.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
fun SettingsRedirectDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Permission Required") },
        text = { Text(text = "Storage permission is required for cleaning up files. Please enable it in App Settings to use this feature.") },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(text = "Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}
