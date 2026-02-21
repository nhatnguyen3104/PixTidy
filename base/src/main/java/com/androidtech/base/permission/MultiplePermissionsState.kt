package com.androidtech.base.permission

class MultiplePermissionsState(
    val permissions: List<String>,
    val grantedMap: Map<String, Boolean>,
    val requestAll: () -> Unit
) {
    val allGranted: Boolean get() = grantedMap.values.all { it }
    val deniedPermissions: List<String> get() = grantedMap.filterValues { !it }.keys.toList()
}