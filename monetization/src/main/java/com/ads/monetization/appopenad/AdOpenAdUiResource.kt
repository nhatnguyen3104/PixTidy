package com.ads.monetization.appopenad

sealed class AdOpenAdUiResource {
    class AdLoaded(val adPlaceName: String) : AdOpenAdUiResource()

    class AdImpression(val adPlaceName: String) : AdOpenAdUiResource()

    class AdShowing(val adPlaceName: String) : AdOpenAdUiResource()

    class AdDismissed(val adPlaceName: String) : AdOpenAdUiResource()

    class AdNotValidOrLoadFailed(val adPlaceName: String) : AdOpenAdUiResource()

    class AdFailedToShow(val adPlaceName: String, val canRetryToShow: Boolean) : AdOpenAdUiResource()
}