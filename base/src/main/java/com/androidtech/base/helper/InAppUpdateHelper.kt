package com.androidtech.base.helper

import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class InAppUpdateHelper @Inject constructor(
    private val appUpdateManager: AppUpdateManager
) {
    /**
     * Lấy thông tin cập nhật. Trả về Result để phân biệt lỗi.
     */
    suspend fun getUpdateResult(): Result<AppUpdateInfo> = suspendCancellableCoroutine { continuation ->
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            continuation.resume(Result.success(info))
        }.addOnFailureListener { exception ->
            Timber.e(exception, "InAppUpdate: Failed to get info")
            continuation.resume(Result.failure(exception))
        }
    }

    /**
     * Flow theo dõi trạng thái cài đặt (đang tải, đã tải xong, lỗi...)
     */
    val installState: Flow<InstallState> = callbackFlow {
        val listener = InstallStateUpdatedListener { state ->
            trySend(state)
        }
        appUpdateManager.registerListener(listener)
        awaitClose {
            appUpdateManager.unregisterListener(listener)
        }
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }
}
