package com.androidtech.data.model.aduiresource

import com.androidtech.domain.model.configad.AdPlace

sealed class AdFullScreenUiResource {

    class AdRequestInfo(val adPlace: AdPlace) : AdFullScreenUiResource()

    class AdLoaded(val adPlace: AdPlace) : AdFullScreenUiResource()

    class AdImpression(val adPlace: AdPlace) : AdFullScreenUiResource()

    class AdNotValidOrLoadFailed(val adPlace: AdPlace) : AdFullScreenUiResource()

    class AdFailedToShow(val adPlace: AdPlace, val canRetryToShow: Boolean) : AdFullScreenUiResource()

    class AdSucceedToShow(val adPlace: AdPlace) : AdFullScreenUiResource()

    class AdCompleted(
        val adPlace: AdPlace,
        val isEarnedReward: Boolean,
        val amount: Int,
        val adErrorType: AdErrorType
    ) : AdFullScreenUiResource()
}

fun AdFullScreenUiResource.getAdPlaceName(): String = when (this) {
    is AdFullScreenUiResource.AdRequestInfo -> adPlace.placeName
    is AdFullScreenUiResource.AdLoaded -> adPlace.placeName
    is AdFullScreenUiResource.AdImpression -> adPlace.placeName
    is AdFullScreenUiResource.AdNotValidOrLoadFailed -> adPlace.placeName
    is AdFullScreenUiResource.AdFailedToShow -> adPlace.placeName
    is AdFullScreenUiResource.AdSucceedToShow -> adPlace.placeName
    is AdFullScreenUiResource.AdCompleted -> adPlace.placeName
}