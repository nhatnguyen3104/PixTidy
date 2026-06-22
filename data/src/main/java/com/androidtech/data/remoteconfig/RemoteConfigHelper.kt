package com.androidtech.data.remoteconfig

import com.androidtech.data.model.remoteconfig.RemoteConfigParam
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

internal inline fun <reified T : Any, P : RemoteConfigParam<T>> FirebaseRemoteConfig.read(
    moshi: Moshi,
    param: P,
): T? {
    val value: Any? = when (T::class) {
        String::class -> getString(param.key)
        Int::class -> getLong(param.key).toInt()
        Boolean::class -> getBoolean(param.key)
        Float::class -> getDouble(param.key).toFloat()
        Double::class -> getDouble(param.key)
        Long::class -> getLong(param.key)
        else -> {
            try {
                val json = getString(param.key)
                json.takeIf {
                    it.isNotBlank()
                }?.let {
                    val jsonAdapter = moshi.adapter(T::class.java)
                    jsonAdapter.fromJson(json)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    return value as? T
}


internal inline fun <reified T : Any, P : RemoteConfigParam<T>> FirebaseRemoteConfig.readList(
    moshi: Moshi,
    param: P,
): List<T> {
    try {
        val json = getString(param.key)
        json.takeIf {
            it.isNotBlank()
        }?.let {
            val listType = Types.newParameterizedType(List::class.java, T::class.java)
            val jsonAdapter: JsonAdapter<List<T>> = moshi.adapter(listType)
            return jsonAdapter.fromJson(json) ?: listOf()
        }
    } catch (_: Exception) {
        return listOf()
    }
    return listOf()
}