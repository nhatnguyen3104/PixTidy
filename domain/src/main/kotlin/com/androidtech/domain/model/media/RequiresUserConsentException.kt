package com.androidtech.domain.model.media

/**
 * Exception thrown when a trash operation (or safe cleanup) requires explicit
 * system user consent via an interactive platform dialog.
 *
 * @property actionSender Platform-specific handle (such as `PendingIntent` on Android)
 *   used by the presentation layer to launch the confirmation dialog.
 */
class RequiresUserConsentException(
    val actionSender: Any
) : Exception("Operation requires explicit user consent via platform system dialog")
