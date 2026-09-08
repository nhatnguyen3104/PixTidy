package com.androidtech.data.local.database

import androidx.room.TypeConverter
import com.androidtech.domain.model.media.SmartInsightType

class ScanResultConverters {
    @TypeConverter
    fun fromInsightType(type: SmartInsightType): String {
        return type.name
    }

    @TypeConverter
    fun toInsightType(name: String): SmartInsightType {
        return try {
            SmartInsightType.valueOf(name)
        } catch (e: Exception) {
            SmartInsightType.EXACT_DUPLICATES
        }
    }
}
