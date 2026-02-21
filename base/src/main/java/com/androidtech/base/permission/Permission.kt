package com.androidtech.base.permission

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun rememberPermissionState(permission: String): MutableState<Boolean> {
    val content = LocalContext.current
    return remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(content, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
}

@Composable
fun rememberSinglePermission(
    permission: String,
    onGranted: () -> Unit = {},
    onDenied: () -> Unit = {}
): SinglePermissionState {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isGranted = granted
        if (granted) onGranted() else onDenied()
    }

    return remember(isGranted) {
        SinglePermissionState(
            permission = permission,
            isGranted = isGranted,
            request = { launcher.launch(permission) }
        )
    }
}

@Composable
fun rememberMultiplePermissions(
    vararg permissions: String,
    onAllGranted: () -> Unit = {},
    onAnyDenied: (List<String>) -> Unit = {}
): MultiplePermissionsState {
    val context = LocalContext.current

    var grantedMap by remember {
        mutableStateOf(
            permissions.associateWith { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        grantedMap = result
        val denied = result.filterValues { !it }.keys.toList()
        if (denied.isEmpty()) onAllGranted() else onAnyDenied(denied)
    }

    return remember(grantedMap) {
        MultiplePermissionsState(
            permissions = permissions.toList(),
            grantedMap = grantedMap,
            requestAll = { launcher.launch(permissions.toList().toTypedArray()) }
        )
    }
}