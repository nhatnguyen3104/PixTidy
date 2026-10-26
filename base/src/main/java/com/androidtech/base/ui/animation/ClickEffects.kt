package com.androidtech.base.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/**
 * Hiệu ứng Bounce/Scale khi người dùng click vào Button hoặc Card.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier = composed {

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animation hold
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "pressScale"
    )

    // Animation "pop"
    val clickScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    this
        .graphicsLayer {
            val s = pressScale * clickScale.value
            scaleX = s
            scaleY = s
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    onClick()
                    scope.launch {
                        clickScale.snapTo(0.9f)
                        clickScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                }
            } else {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    /* No-op */
                }
            }
        )
        /*.clickable(
            interactionSource = interactionSource,
            indication = null
        ) {
            onClick()
            scope.launch {
                clickScale.snapTo(0.9f)
                clickScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }*/
}