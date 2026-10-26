package com.ads.monetization.config

sealed class ConsentFromUiResource {
    data object Loading : ConsentFromUiResource()
    data object Showing : ConsentFromUiResource()
    data object Complete : ConsentFromUiResource()
}