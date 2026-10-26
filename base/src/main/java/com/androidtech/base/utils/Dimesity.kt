package com.androidtech.base.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import java.util.Locale

/*
@Composable
fun Dp.toPx() = with(LocalDensity.current) { this@toPx.toPx() }*/

fun Long.formatSize(): String {
    val kb = 1000L
    val mb = kb * 1000L
    val gb = mb * 1000L

    return when {
        this >= gb -> String.format(Locale.US, "%.1f GB", this.toFloat() / gb)
        this >= mb -> String.format(Locale.US, "%.1f MB", this.toFloat() / mb)
        this >= kb -> String.format(Locale.US, "%.1f KB", this.toFloat() / kb)
        else -> "$this B"
    }
}

