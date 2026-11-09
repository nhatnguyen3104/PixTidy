package com.androidtech.domain.model.configad

sealed class NativeTemplateType {
    abstract val key: String

    companion object {
        fun getTypeBy(key: String): NativeTemplateType {
            val lowercaseKey = key.lowercase()
            return when {
                lowercaseKey.contains("small") -> Small
                lowercaseKey.contains("medium") -> Medium
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

    fun isMedium() = this is Medium || key == "medium"
    fun isSmall() = this is Small || key == "small"
    fun isFull() = this is Full || key == "full"
}