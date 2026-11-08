package com.androidtech.data.remoteconfig

sealed class CountryTier {
    data object Default: CountryTier()
    data object Tier1: CountryTier()
    data object Tier2: CountryTier()
    data object Tier3: CountryTier()
}