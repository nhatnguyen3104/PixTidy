package com.androidtech.base.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

enum class MediaPermissionStatus {
    FULL_ACCESS,
    PARTIAL_ACCESS,
    DENIED,
    PERMANENTLY_DENIED
}

object MediaPermissionHelper {

    private const val PREF_NAME = "media_permission_prefs"
    private const val KEY_HAS_REQUESTED = "has_requested_permission"

    fun setHasRequestedBefore(context: Context, requested: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAS_REQUESTED, requested).apply()
    }

    fun hasRequestedBefore(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAS_REQUESTED, false)
    }

    fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            }
            else -> {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun Context.findActivity(): Activity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }

    fun checkPermissionStatus(context: Context): MediaPermissionStatus {
        val permissions = getRequiredPermissions()
        val activity = context.findActivity()

        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val hasImages = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasVideos = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            val hasVisualUserSelected = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
            if (hasImages || hasVideos) return MediaPermissionStatus.FULL_ACCESS
            if (hasVisualUserSelected) return MediaPermissionStatus.PARTIAL_ACCESS
            false
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasImages = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasVideos = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            if (hasImages || hasVideos) return MediaPermissionStatus.FULL_ACCESS
            false
        } else {
            val hasStorage = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            if (hasStorage) return MediaPermissionStatus.FULL_ACCESS
            false
        }

        if (isGranted) return MediaPermissionStatus.FULL_ACCESS

        val requestedBefore = hasRequestedBefore(context)
        val showRationale = activity != null && permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }

        return when {
            requestedBefore && !showRationale -> MediaPermissionStatus.PERMANENTLY_DENIED
            else -> MediaPermissionStatus.DENIED
        }
    }
}
