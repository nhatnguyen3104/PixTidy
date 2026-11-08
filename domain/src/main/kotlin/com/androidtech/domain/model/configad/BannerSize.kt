package com.androidtech.domain.model.configad

sealed class BannerSize {

    abstract val key: String

    fun isAnchored() = this is Anchored || this is LargeAnchored

    companion object {

        fun getSizeBy(key: String): BannerSize {
            return when(key) {
                Anchored.key -> Anchored
                LargeAnchored.key -> LargeAnchored
                Inline.key -> Inline
                StandardMedium.key -> StandardMedium
                StandardLarge.key -> StandardLarge
                else -> Anchored
            }
        }
    }

    data object Anchored: BannerSize() {
        override val key = "anchored"
    }

    data object LargeAnchored: BannerSize() {
        override val key = "large_anchored"
    }

    data object Inline: BannerSize() {
        override val key = "inline"
    }

    data object StandardMedium: BannerSize() {
        override val key = "standard_medium"
    }

    data object StandardLarge: BannerSize() {
        override val key = "standard_large"
    }

}
