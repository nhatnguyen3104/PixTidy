package com.androidtech.data.local.prefs

import android.content.Context
import androidx.core.content.edit
import com.androidtech.data.R
import kotlin.reflect.KProperty

abstract class Preference(
    private val context: Context
) {
    private val preference = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)

    enum class StorageType {
        String, Int, Float, Long, Boolean
    }

    interface PreferenceDelegate<T> {
        operator fun setValue(thisReference: Any, property: KProperty<*>, value: T)
        operator fun getValue(thisReference: Any, property: KProperty<*>): T
    }

    @Suppress("UNCHECKED_CAST")
    inner class GenericDelegate<T>(
        private val preferenceKey: String,
        private val storageType: StorageType,
        private val defaultValue: T
    ): PreferenceDelegate<T> {
        override fun setValue(thisReference: Any, property: KProperty<*>, value: T) {
            when (storageType) {
                StorageType.String -> preference.edit(true) { putString(preferenceKey, value as String) }
                StorageType.Int -> preference.edit(true) { putInt(preferenceKey, value as Int) }
                StorageType.Float -> preference.edit(true) { putFloat(preferenceKey, value as Float) }
                StorageType.Long -> preference.edit(true) { putLong(preferenceKey, value as Long) }
                StorageType.Boolean -> preference.edit(true) { putBoolean(preferenceKey, value as Boolean) }
            }
        }

        override fun getValue(thisReference: Any, property: KProperty<*>): T {
            return when (storageType) {
                StorageType.String -> preference.getString(preferenceKey, defaultValue as String) as T
                StorageType.Int -> preference.getInt(preferenceKey, defaultValue as Int) as T
                StorageType.Float -> preference.getFloat(preferenceKey, defaultValue as Float) as T
                StorageType.Long -> preference.getLong(preferenceKey, defaultValue as Long) as T
                StorageType.Boolean -> preference.getBoolean(preferenceKey, defaultValue as Boolean) as T
            }
        }
    }

    fun stringRef(
        key: String,
        defaultValue: String
    ) = GenericDelegate(key, StorageType.String, defaultValue)

    fun intRef(
        key: String,
        defaultValue: Int
    ) = GenericDelegate(key, StorageType.Int, defaultValue)

    fun floatRef(
        key: String,
        defaultValue: Float
    ) = GenericDelegate(key, StorageType.Float, defaultValue)

    fun longRef(
        key: String,
        defaultValue: Long
    ) = GenericDelegate(key, StorageType.Long, defaultValue)

    fun booleanRef(
        key: String,
        defaultValue: Boolean
    ) = GenericDelegate(key, StorageType.Boolean, defaultValue)
}
