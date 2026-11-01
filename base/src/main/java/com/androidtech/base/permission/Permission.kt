package com.androidtech.base.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

sealed class Permission(val permission: String) {

    data object Camera : Permission(Manifest.permission.CAMERA)

    data object ReadExternalStorage : Permission(Manifest.permission.READ_EXTERNAL_STORAGE)
    data object WriteExternalStorage : Permission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    data object RecordAudio : Permission(Manifest.permission.RECORD_AUDIO)

    // For android 12 -
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    data object ReadMediaImages : Permission(Manifest.permission.READ_MEDIA_IMAGES)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    data object ReadMediaVideos : Permission(Manifest.permission.READ_MEDIA_VIDEO)

    // For android 14+ (API 34+) partial access
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    data object ReadMediaVisualUserSelected : Permission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

}

class SinglePermissionState(
    val permission: String,
    val isGranted: Boolean,
    val request: () -> Unit
)

class MultiplePermissionsState(
    val permissions: List<String>,
    val grantedMap: Map<String, Boolean>,
    val requestAll: () -> Unit
) {
    val allGranted: Boolean get() = grantedMap.values.all { it }
    val deniedPermissions: List<String> get() = grantedMap.filterValues { !it }.keys.toList()
}

@Composable
fun rememberPermissionGranted(permission: String): MutableState<Boolean> {
    val content = LocalContext.current
    return remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(content, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
}
// Overload
@Composable
fun rememberPermissionGranted(permission: Permission): MutableState<Boolean> {
    return rememberPermissionGranted(permission.permission)
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
// Overload
@Composable
fun rememberSinglePermission(
    permission: Permission,
    onGranted: () -> Unit = {},
    onDenied: () -> Unit = {}
): SinglePermissionState {
    return rememberSinglePermission(permission.permission, onGranted, onDenied)
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
// Overload
@Composable
fun rememberMultiplePermissions(
    vararg permissions: Permission,
    onAllGranted: () -> Unit = {},
    onAnyDenied: (List<String>) -> Unit = {}
): MultiplePermissionsState {
    val permissionStrings = permissions.map { it.permission }.toTypedArray()
    return rememberMultiplePermissions(
        permissions = permissionStrings,
        onAllGranted = onAllGranted,
        onAnyDenied = onAnyDenied
    )
}