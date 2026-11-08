package com.androidtech.domain.model.configad

sealed class NativeTemplateType {
    abstract val key: String

    companion object {
        fun getTypeBy(key: String): NativeTemplateType {
            return when (key) {
                "small" -> Small
                "medium" -> Medium
                else -> Full
            }
        }
    }

    data object Small: NativeTemplateType() {
        override val key: String = "small"
    }

    data object Medium: NativeTemplateType() {
        override val key: String = "medium"
    }

    data object Full: NativeTemplateType() {
        override val key: String = "full"
    }

    fun isMedium() = key == "medium"
    fun isSmall() = key == "small"
    fun isFull() = key == "full"
}