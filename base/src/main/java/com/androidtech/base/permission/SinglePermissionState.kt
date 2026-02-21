package com.androidtech.base.permission

class SinglePermissionState(
    val permission: String,
    val isGranted: Boolean,
    val request: () -> Unit
)