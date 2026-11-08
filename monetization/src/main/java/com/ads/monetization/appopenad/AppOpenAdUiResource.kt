package com.ads.monetization.appopenad

sealed class AppOpenAdUiResource {
    class AdLoaded(val adPlaceName: String) : AppOpenAdUiResource()

    class AdImpression(val adPlaceName: String) : AppOpenAdUiResource()

    class AdShowing(val adPlaceName: String) : AppOpenAdUiResource()

    class AdDismissed(val adPlaceName: String) : AppOpenAdUiResource()

    class AdNotValidOrLoadFailed(val adPlaceName: String) : AppOpenAdUiResource()

    class AdFailedToShow(val adPlaceName: String, val canRetryToShow: Boolean) : AppOpenAdUiResource()
}