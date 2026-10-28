package com.androidtech.base.helper

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.androidtech.base.R
import com.androidtech.base.logger.Logger
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.core.content.edit

/**
 * Checks Google Play for a newer version and, if one exists, shows a custom dialog
 * that sends the user to the Play Store listing.
 *
 * Includes a cooldown period (e.g., 3 days) when the user clicks "Skip" to avoid
 * spamming the user on every app launch.
 */
class AppUpdateChecker(
    private val activity: ComponentActivity,
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
) : DefaultLifecycleObserver {

    private var dialog: AlertDialog? = null
    private val sharedPreferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        activity.lifecycle.addObserver(this)
    }

    /** Queries Play for update availability and shows the dialog if needed. */
    fun check(isManualCheck: Boolean = false) {
        activity.lifecycleScope.launch {
            try {
                val info = appUpdateManager.requestAppUpdateInfo()
                val updateAvailable =
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE

                if (activity.isFinishing || activity.isDestroyed) return@launch

                // Check update available
                if (updateAvailable) {
                    // Nếu là manual check hoặc vượt qua được điều kiện 3 ngày -> Hiện Dialog cập nhật
                    if (isManualCheck || shouldShowUpdateDialog(info)) {
                        showUpdateDialog(info)
                    }
                } else if (isManualCheck) {
                    // Nếu người dùng chủ động bấm trong Settings mà app chưa có bản mới -> Báo cáo nhẹ nhàng
                    showUpToDateDialog()
                }
            } catch (e: CancellationException) {
                throw e // never swallow coroutine cancellation
            } catch (e: Exception) {
                // Not installed from Play, Play Store missing/outdated, no network, etc.
                // An update check must never crash or block the app.
                Logger.d("Update check failed", e)
            }
        }
    }

    /**
     * Cho phép hiển thị Dialog nếu:
     * 1. Đây là phiên bản mới hơn hẳn phiên bản từng bấm Skip trước đó.
     * 2. Hoặc phiên bản giống nhau nhưng đã trôi qua ít nhất 3 ngày kể từ lúc nhấn Skip.
     */
    private fun shouldShowUpdateDialog(info: AppUpdateInfo): Boolean {
        val lastSkippedVersion = sharedPreferences.getInt(KEY_SKIPPED_VERSION, -1)
        val lastSkippedTime = sharedPreferences.getLong(KEY_SKIPPED_TIMESTAMP, 0L)
        val currentAvailableVersion = info.availableVersionCode()

        // Nếu là một version mới hơn hẳn version từng bấm Skip -> Hiện lại ngay không cần đợi
        if (currentAvailableVersion > lastSkippedVersion) {
            return true
        }

        // Nếu cùng version -> Kiểm tra khoảng cách thời gian từ lần Skip cuối
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - lastSkippedTime
        val daysPassed = TimeUnit.MILLISECONDS.toDays(timeDifference)

        return daysPassed >= COOLDOWN_DAYS
    }

    private fun showUpdateDialog(info: AppUpdateInfo) {
        if (dialog?.isShowing == true) return

        dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.update_dialog_title)
            .setMessage(R.string.update_dialog_message)
            .setCancelable(true)
            .setPositiveButton(R.string.update_dialog_positive) { d, _ ->
                openPlayStore()
                d.dismiss()
            }
            .setNegativeButton(R.string.update_dialog_negative) { d, _ ->
                saveSkipState(info.availableVersionCode())
                d.dismiss() // user continues using the app normally
            }
            .show()
    }

    private fun showUpToDateDialog() {
        if (dialog?.isShowing == true) return

        dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.update_uptodate_title)
            .setMessage(R.string.update_uptodate_message)
            .setPositiveButton("Ok") { d, _ ->
                d.dismiss()
            }
            .show()
    }

    /** Lưu versionCode và mốc thời gian (timestamp) hiện tại khi người dùng bấm Skip */
    private fun saveSkipState(availableVersionCode: Int) {
        sharedPreferences.edit {
            putInt(KEY_SKIPPED_VERSION, availableVersionCode)
                .putLong(KEY_SKIPPED_TIMESTAMP, System.currentTimeMillis())
        }
    }

    private fun openPlayStore() {
        val packageName = activity.packageName
        try {
            // Preferred: open directly in the Play Store app
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
            )
        } catch (e: ActivityNotFoundException) {
            // Fallback: Play Store app not installed, open in a browser
            try {
                activity.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://play.google.com/store/apps/details?id=$packageName".toUri()
                    )
                )
            } catch (e2: ActivityNotFoundException) {
                Logger.e(e2, "No app available to open the Play Store listing")
            }
        }
    }

    /** Dismiss the dialog with the Activity to avoid leaking the window. */
    override fun onDestroy(owner: LifecycleOwner) {
        dialog?.dismiss()
        dialog = null
        activity.lifecycle.removeObserver(this)
    }

    private companion object {
        const val TAG = "AppUpdateChecker"
        const val PREFS_NAME = "app_update_checker_prefs"
        const val KEY_SKIPPED_VERSION = "key_skipped_version"
        const val KEY_SKIPPED_TIMESTAMP = "key_skipped_timestamp"
        const val COOLDOWN_DAYS = 3L // Thời gian giãn cách giữa các lần nhắc (3 ngày)
    }
}