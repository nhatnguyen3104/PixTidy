package com.androidtech.features.ui.screens.language

import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ads.monetization.ui.nativead.DisplayNativeAdView
import com.ads.monetization.ui.nativead.MediumNativeAdView
import com.androidtech.base.R
import com.androidtech.base.extension.AppDestination
import com.androidtech.base.extension.LocalNavController
import com.androidtech.base.model.LanguageModel
import com.androidtech.base.theme.Body16Regular
import com.androidtech.base.theme.SurfaceBlack20
import com.androidtech.base.theme.Title20Bold
import com.androidtech.base.ui.animation.LoadingAnimation

fun NavGraphBuilder.languageScreen() {
    composable(AppDestination.LanguageScreen.route) {
        LanguageScreen(
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview
@Composable
private fun PreviewLanguageScreen() {
    LanguageScreenContent(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        uiState = LanguageState(),
        onActionEvent = {}
    )
}

@Composable
fun LanguageScreen(
    modifier: Modifier = Modifier,
    viewModel: LanguageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = LocalNavController.current
    val activity = LocalActivity.current

    LaunchedEffect(activity) {
        activity?.let { act ->
            viewModel.handleEvent(LanguageEvent.PreloadAds(act))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                LanguageEffect.NavigateToHome -> {
                    navController.navigate(AppDestination.HomeScreen.route) {
                        popUpTo(AppDestination.LanguageScreen.route) {
                            inclusive = true
                        }
                    }
                }

                LanguageEffect.NavigateToOnboarding -> {
                    navController.navigate(AppDestination.OnboardingScreen.route) {
                        popUpTo(AppDestination.LanguageScreen.route) {
                            inclusive = true
                        }
                    }
                }

                is LanguageEffect.ApplyLanguage -> {
                    val appLocale = LocaleListCompat.forLanguageTags(effect.languageCode)
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            }
        }
    }

    LanguageScreenContent(
        modifier = modifier,
        uiState = uiState,
        onActionEvent = { event ->
            when (event) {
                LanguageEvent.ClickToSaveLanguage -> {
                    viewModel.setEvent(LanguageEvent.ClickToSaveLanguage)
                }
                is LanguageEvent.ClickLanguageItem -> {
                    val languageModel = event.languageModel.copy(isSelected = !event.languageModel.isSelected)
                    viewModel.setEvent(
                        LanguageEvent.ClickLanguageItem(languageModel)
                    )
                }
                is LanguageEvent.PreloadAds -> {
                    viewModel.setEvent(event)
                }
            }
        }
    )

    LoadingAnimation(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBlack20),
        isLoading = uiState.isShowLoading
    )
}

@Composable
private fun LanguageScreenContent(
    modifier: Modifier = Modifier,
    uiState: LanguageState,
    onActionEvent: (LanguageEvent) -> Unit
) {
    androidx.compose.material3.Scaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.text_select_language),
                    style = Title20Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = { onActionEvent(LanguageEvent.ClickToSaveLanguage) },
                    contentPadding = PaddingValues(2.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_save_language),
                        contentDescription = "Save icon",
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
            }
        },
        bottomBar = {
            DisplayNativeAdView(
                adHolder = uiState.nativeHolder,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header (title + save button)


            // 2. Language list — weight(1f)
            LanguageList(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                allLanguages = uiState.allLanguages,
                onActionEvent = onActionEvent
            )
        }
    }
}

@Composable
private fun LanguageList(
    modifier: Modifier = Modifier,
    allLanguages: List<LanguageModel> = emptyList(),
    onActionEvent: (LanguageEvent) -> Unit
) {
    Box(
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                allLanguages,
                key = { it.languageCode }
            ) { languageModel ->
                LanguageModelItem(
                    modifier = Modifier.fillMaxWidth(),
                    languageModel = languageModel,
                    onActionEvent = onActionEvent
                )
            }
        }
    }
}

@Composable
private fun LanguageModelItem(
    modifier: Modifier = Modifier,
    languageModel: LanguageModel,
    onActionEvent: (LanguageEvent) -> Unit
) {
    Row(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
            .background(
                color = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                onActionEvent(
                    LanguageEvent.ClickLanguageItem(languageModel)
                )
            }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(languageModel.flagRes),
            contentDescription = languageModel.languageCode,
            modifier = Modifier.size(48.dp)
        )

        Text(
            text = languageModel.displayName,
            style = Body16Regular,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )

        Image(
            painter = painterResource(
                if (languageModel.isSelected) {
                    R.drawable.ic_selected_language
                } else {
                    R.drawable.ic_unselect_language
                }
            ),
            contentDescription = "select icon",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable {
                    onActionEvent(
                        LanguageEvent.ClickLanguageItem(languageModel)
                    )
                }
                .padding(4.dp)
        )
    }
}