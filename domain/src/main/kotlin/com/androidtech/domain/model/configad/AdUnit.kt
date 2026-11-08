package com.androidtech.domain.model.configad

sealed class AdUnit {
    abstract val adUnitId: String

    companion object {
        val AllPriceEmpty = AllPrice("")
        val MediumPriceEmpty = MediumPrice("")
        val HighPriceEmpty = HighPrice("")
    }

    fun isBlank() = adUnitId.isBlank()
    fun isNotBlank() = adUnitId.isNotBlank()

    class AllPrice(
        override val adUnitId: String
    ): AdUnit()

    class MediumPrice(
        override val adUnitId: String
    ): AdUnit()

    class HighPrice(
        override val adUnitId: String
    ): AdUnit()
}