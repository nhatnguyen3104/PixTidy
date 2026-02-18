package com.androidtech.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf")
data class PdfModel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0
)