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
import com.androidtech.base.R
import com.androidtech.base.extension.AppDestination
import com.androidtech.base.extension.LocalNavController
import com.androidtech.base.theme.Title20Bold
import com.androidtech.features.ui.screens.home.HomeEvent
import androidx.core.net.toUri

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
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = LocalNavController.current
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })

    LaunchedEffect(pagerState.currentPage) {
        if (uiState.value.currentPage != pagerState.currentPage) {
            viewModel.handleEvent(OnboardingEvent.PageChanged(pagerState.currentPage))
        }
    }

    LaunchedEffect(viewModel.uiEffect, lifecycleOwner) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle).collect { effect ->
            when (effect) {
                is OnboardingEffect.ScrollToPage -> {
                    pagerState.animateScrollToPage(effect.page)
                }
                is OnboardingEffect.NavigateToHome -> {
                    navController.navigate(AppDestination.HomeScreen.route) {
                        popUpTo(AppDestination.OnboardingScreen.route) {
                            inclusive = true
                        }
                    }
                }
                is OnboardingEffect.OpenWebUrl -> {
                    val intent = Intent(Intent.ACTION_VIEW, effect.url.toUri())
                    context.startActivity(intent)
                }
            }
        }
    }
    OnboardingContent (
        uiState = uiState.value,
        onEvent = viewModel::handleEvent,
        pagerState = pagerState,
    )
}

private val onboardingPages = listOf(
    OnboardingPageData(
        title = "Clean Similar & Duplicate Photos",
        description = "PixTidy automatically detects duplicate photos, best shots, and similar burst images to free up space.",
        icon = Icons.Outlined.Collections,
        highlightBadgeText = "Smart AI Detection"
    ),
    OnboardingPageData(
        title = "Manage Large Videos & Screenshots",
        description = "Easily filter out heavy videos and old screenshots occupying valuable storage in just one tap.",
        icon = Icons.Outlined.VideoLibrary,
        highlightBadgeText = "Storage Saver"
    ),
    OnboardingPageData(
        title = "Private & Local Processing",
        description = "Your photos and videos are scanned completely on your device. Zero data ever leaves your phone.",
        icon = Icons.Outlined.Shield,
        highlightBadgeText = "Privacy Guarantee"
    )
)

@OptIn( ExperimentalMaterial3Api::class)
@Composable
fun OnboardingContent(
    uiState: OnboardingState,
    onEvent: (OnboardingEvent) -> Unit,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = Title20Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_app),
                            contentDescription = "logo",
                            tint = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0)
            )
        },
        bottomBar = {
            OnboardingBottomNavigation(
                isLastPage = uiState.currentPage == onboardingPages.size - 1,
                currentPage = uiState.currentPage,
                pageCount = onboardingPages.size,
                isPrivacyAccepted = uiState.isPrivacyAccepted,
                onTogglePrivacy = { accepted -> onEvent(OnboardingEvent.TogglePrivacyAcceptance(accepted)) },
                onTermsClick = { onEvent(OnboardingEvent.TermsOfServiceClicked) },
                onPrivacyClick = { onEvent(OnboardingEvent.PrivacyPolicyClicked) },
                onNextClick = {
                    onEvent(OnboardingEvent.NextClicked)
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { pageIndex ->
                OnboardingPageContent(pageData = onboardingPages[pageIndex])
            }
        }
    }
}