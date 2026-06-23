package com.androidtech.base.extension

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
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