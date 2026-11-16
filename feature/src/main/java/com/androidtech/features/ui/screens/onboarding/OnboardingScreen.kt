package com.androidtech.features.ui.screens.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.androidtech.base.R
import com.androidtech.base.extension.AppDestination
import com.androidtech.base.extension.LocalNavController
import com.androidtech.base.theme.Title20Bold
import com.androidtech.features.ui.screens.home.HomeEvent
import androidx.core.net.toUri
import com.androidtech.base.theme.Body18SemiBold
import com.androidtech.base.theme.PrimaryBlue
import com.androidtech.base.theme.SurfaceVariantDark
import com.androidtech.base.theme.SurfaceWhite
import com.androidtech.domain.model.onboarding.OnboardingPage

fun NavGraphBuilder.onboardingScreen() {
    composable(
        route = AppDestination.OnboardingScreen.route,
        exitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(300)) }
    ) {
        OnboardingScreen()
    }
}

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = LocalNavController.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.handleEvent(OnboardingEvent.Initialize)
    }

    LaunchedEffect(viewModel.uiEffect, lifecycleOwner) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle).collect { effect ->
            when (effect) {
                is OnboardingEffect.ScrollToPage -> {

                }
                is OnboardingEffect.NavigateToHome -> {
                    navController.navigate(AppDestination.HomeScreen.route) {
                        popUpTo(AppDestination.OnboardingScreen.route) {
                            inclusive = true
                        }
                    }
                }
            }
        }
    }
    OnboardingScreenContent (
        uiState = uiState,
        onActionEvent = { event ->
            when (event) {
                is OnboardingEvent.ClickNext -> {
                    viewModel.handleEvent(OnboardingEvent.ClickNext(event.page))
                }
                else -> {
                    viewModel.handleEvent(event)
                }
            }
        }
    )
}


@Composable
private fun OnboardingScreenContent(
    modifier: Modifier = Modifier,
    uiState: OnboardingState,
    onActionEvent: ((OnboardingEvent) -> Unit)?
) {
    val pagerState = rememberPagerState(
        pageCount = { uiState.totalPage }
    )

    LaunchedEffect(uiState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage) {
            pagerState.animateScrollToPage(
                page = uiState.currentPage
            )
        }
    }

    // Đồng bộ từ Pager -> ViewModel (khi vuốt tay)
    LaunchedEffect(pagerState.settledPage, uiState.currentPage) {
        if (pagerState.settledPage > uiState.currentPage) {
            onActionEvent?.invoke(OnboardingEvent.ClickNext(uiState.currentPage))
        }
    }

    Column(
        modifier = modifier
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                OnboardingPage.FirstOnboarding.page -> FirstOnboarding(
                    modifier = Modifier.fillMaxSize()
                )
                OnboardingPage.SecondOnboarding.page -> SecondOnboarding(
                    modifier = Modifier.fillMaxSize()
                )
                OnboardingPage.ThirdOnboarding.page -> ThirdOnboarding(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        PageIndicator(
            modifier = Modifier.fillMaxWidth(),
            totalPageCount = pagerState.pageCount,
            currentPage = pagerState.currentPage,
            onActionEvent = onActionEvent
        )
    }
}

@Composable
private fun PageIndicator(
    modifier: Modifier = Modifier,
    totalPageCount: Int,
    currentPage: Int,
    onActionEvent: ((OnboardingEvent) -> Unit)? = null
) {
    val buttonNameRes = remember(currentPage) {
        if (currentPage == (totalPageCount - 1)) {
            R.string.text_started
        } else {
            R.string.text_next
        }
    }

    Row(
        modifier = modifier
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.wrapContentHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalPageCount) { index ->
                val indicatorColor = if (currentPage == index) {
                    PrimaryBlue
                } else {
                    SurfaceVariantDark
                }
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color = indicatorColor)
                        .size(16.dp)
                )
            }
        }

        Button(
            onClick = {
                onActionEvent?.invoke(OnboardingEvent.ClickNext(currentPage))
            },
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.Transparent,
                containerColor = PrimaryBlue
            ),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 24.dp),
            shape = RoundedCornerShape(48.dp)
        ) {
            Text(
                text = stringResource(buttonNameRes),
                style = Body18SemiBold,
                color = SurfaceWhite
            )
        }
    }
}