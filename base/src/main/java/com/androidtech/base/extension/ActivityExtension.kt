package com.androidtech.base.extension

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

inline fun <T : Any> ComponentActivity.collectFlowOn(
    flow: Flow<T>,
    lifecycleState: Lifecycle.State = Lifecycle.State.CREATED,
    crossinline onResult: (t: T) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(lifecycleState) {
            flow.collect {
                onResult.invoke(it)
            }
        }
    }
}

inline fun <T : Any> ComponentActivity.collectFlowOn(
    stateFlow: StateFlow<T>,
    lifecycleState: Lifecycle.State = Lifecycle.State.CREATED,
    crossinline onResult: (t: T) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(lifecycleState) {
            stateFlow.collect {
                onResult.invoke(it)
            }
        }
    }
}

inline fun <T : Any> ComponentActivity.collectFlowOn(
    sharedFlow: SharedFlow<T>,
    lifecycleState: Lifecycle.State = Lifecycle.State.CREATED,
    crossinline onResult: (t: T) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(lifecycleState) {
            sharedFlow.collect {
                onResult.invoke(it)
            }
        }
    }
}

fun <T : Fragment> T.withArgs(
    argsBuilder: Bundle.() -> Unit
): T = this.apply {
    arguments = Bundle().apply(argsBuilder)
}

inline fun <T: Any?> ComponentActivity.collectFlowOnNullable(
    stateFlow: StateFlow<T>,
    lifecycleState: Lifecycle.State = Lifecycle.State.CREATED,
    crossinline onResult: (t: T) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(lifecycleState) {
            stateFlow.collect {
                onResult.invoke(it)
            }
        }
    }
}

inline fun <T: Any?> ComponentActivity.collectFlowOnNullable(
    sharedFlow: SharedFlow<T>,
    lifecycleState: Lifecycle.State = Lifecycle.State.CREATED,
    crossinline onResult: (t: T) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(lifecycleState) {
            sharedFlow.collect {
                onResult.invoke(it)
            }
        }
    }
}

fun Context.getScreenHeightByContext(): Int {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = wm.currentWindowMetrics
        val insets = windowMetrics.windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        windowMetrics.bounds.height() - insets.top - insets.bottom
    } else {
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(displayMetrics)
        displayMetrics.heightPixels
    }
}

fun Context.getScreenWidthByContext(): Int {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = wm.currentWindowMetrics
        val insets = windowMetrics.windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        windowMetrics.bounds.width() - insets.left - insets.right
    } else {
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(displayMetrics)
        displayMetrics.widthPixels
    }
}

fun Context.getScreenWidthDpByContext(): Int {
    val adWidthPixels = getScreenWidthByContext()

    val density = resources.displayMetrics.density
    return (adWidthPixels / density).toInt()
}

fun Context.getScreenHeightDpByContext(): Int {
    val adWidthPixels = getScreenHeightByContext()

    val density = resources.displayMetrics.density
    return (adWidthPixels / density).toInt()
}

fun Context.getBannerAdWidthByContext(): Int {
    val adWidthPixels = resources.displayMetrics.widthPixels
    val density = resources.displayMetrics.density
    return (adWidthPixels / density).toInt()
}

fun Activity.getScreenWidthDp(): Int {
    val adWidthPixels = getScreenWidth()

    val density = resources.displayMetrics.density
    return (adWidthPixels / density).toInt()
}

fun Activity.getScreenHeightDp(): Int {
    val adWidthPixels = getScreenHeight()

    val density = resources.displayMetrics.density
    return (adWidthPixels / density).toInt()
}

fun Activity.getBannerAdWidth(): Int {
    val adWidthPixels = getScreenWidth()

    val density = resources.displayMetrics.density
    return (adWidthPixels / density).toInt()
}

fun Activity.getScreenWidth(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = windowManager.currentWindowMetrics

        val insets = windowMetrics.windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())

        windowMetrics.bounds.width() - insets.left - insets.right
    } else {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val view = window.decorView
        val rootWindowInsets = view.rootWindowInsets
        if (rootWindowInsets != null) {
            val insets = WindowInsetsCompat.toWindowInsetsCompat(rootWindowInsets, view).getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            displayMetrics.widthPixels - insets.left - insets.right
        } else {
            displayMetrics.widthPixels
        }
    }
}

fun Activity.getScreenHeight(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = windowManager.currentWindowMetrics

        val insets = windowMetrics.windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())

        windowMetrics.bounds.height() - insets.top - insets.bottom
    } else {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val view = window.decorView
        val rootWindowInsets = view.rootWindowInsets
        if (rootWindowInsets != null) {
            val insets = WindowInsetsCompat.toWindowInsetsCompat(rootWindowInsets, view).getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            displayMetrics.heightPixels - insets.top - insets.bottom
        } else {
            displayMetrics.heightPixels
        }
    }
}