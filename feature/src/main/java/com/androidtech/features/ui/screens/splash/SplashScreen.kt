package com.androidtech.features.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.androidtech.base.R
import com.androidtech.base.extension.AppDestination
import com.androidtech.base.extension.LocalNavController
import com.androidtech.base.theme.AppTheme
import com.androidtech.base.theme.Body12Medium
import com.androidtech.base.theme.Body12SemiBold
import com.androidtech.base.theme.Title24Bold

fun NavGraphBuilder.splashScreen() {
    composable(
        route = AppDestination.SplashScreen.route,
    ) {
        SplashScreen()
    }
}

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = LocalNavController.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.handleEvent(SplashEvent.StartInitialization)
    }

    LaunchedEffect(viewModel.uiEffect, lifecycleOwner) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle).collect { effect ->
            when (effect) {
                is SplashEffect.NavigateToHome -> {
                    navController.navigate(AppDestination.HomeScreen.route) {
                        popUpTo(AppDestination.SplashScreen.route) {
                            inclusive = true
                        }
                    }
                }
                is SplashEffect.NavigateToLanguage -> {
                    navController.navigate(AppDestination.LanguageScreen.route) {
                        popUpTo(AppDestination.SplashScreen.route) {
                            inclusive = true
                        }
                    }
                }
                is SplashEffect.ShowFullScreenAd -> {
                    /*activity?.let {
                        viewModel.handleEvent(SplashEvent.ShowInterAd(activity))
                    }*/
                }
            }
        }
    }
    SplashContent(
        uiState = uiState.value,
    )

}

@Composable
fun SplashContent(
    uiState: SplashState,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "splash_progress_anim"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Logo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_app),
                    contentDescription = "app logo",
                    tint = null,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title
            Text(
                text = stringResource(R.string.app_name),
                style = Title24Bold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Slogan/Sub-title
            Text(
                text = stringResource(R.string.app_slogan),
                style = Body12Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.7f),
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )
        }

        // Bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Linear Progress Indicator
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(3.5.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Banner or Native Ads
            /*AdMobBannerView(
                adManager = viewModel.adManager,
                adUnitId = viewModel.adManager.getAdConfig().bannerAdId,
                preloadedAdView = viewModel.adManager.getPreloadedBannerAd()
            )*/
        }
    }
}
