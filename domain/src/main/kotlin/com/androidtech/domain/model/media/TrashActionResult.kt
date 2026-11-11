package com.androidtech.domain.model.media

/**
 * Outcome of a trash-bin operation (move to trash, restore, delete permanently).
 *
 * On modern Android these operations usually can't complete silently: the
 * platform asks the user to confirm. The domain layer can't name the
 * platform's confirmation type, so it is carried as an opaque [Any].
 */
sealed interface TrashActionResult {

    /** The operation completed (or there was nothing to do, e.g. an empty id list). */
    data object Success : TrashActionResult

    /**
     * The platform needs the user's consent before it performs the action.
     *
     * @property actionSender Opaque, platform-specific handle that the
     *   presentation layer knows how to launch (on Android: a `PendingIntent`
     *   whose `intentSender` goes into an `IntentSenderRequest`). The domain
     *   and use-case layers must never inspect or cast it. The system performs
     *   the action itself once the user approves, so the caller only needs to
     *   refresh its data afterwards.
     */
    data class RequiresUserConsent(val actionSender: Any) : TrashActionResult

    data class Error(val throwable: Throwable) : TrashActionResult
}