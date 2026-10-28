package com.androidtech.features.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FolderDelete
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.androidtech.base.extension.AppDestination
import com.androidtech.base.R
import com.androidtech.base.extension.LocalNavController
import com.androidtech.base.theme.Title20Bold
import com.androidtech.domain.model.settings.AppLanguage
import com.androidtech.domain.model.settings.AppTheme
import com.androidtech.features.ui.screens.home.HomeViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun NavGraphBuilder.settingsScreen() {
    composable(
        route = AppDestination.SettingsScreen.route,
    ) {
        SettingsScreen()
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = LocalNavController.current
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    fun showSnackbar(message: String) {
        snackbarJob?.cancel()
        snackbarJob = scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(viewModel.uiEffect, lifecycleOwner) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle).collect { effect ->
            when (effect) {
                is SettingsEffect.NavigateBack -> {
                    navController.popBackStack()
                }
                is SettingsEffect.ShowSnackbar -> {

                }
                is SettingsEffect.OpenWebUrl -> {
                    val intent = Intent(Intent.ACTION_VIEW, effect.url.toUri())
                    context.startActivity(intent)
                }
            }
        }
    }
    SettingsContent(
        uiState = uiState.value,
        onEvent = { viewModel.handleEvent(it) },
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    uiState: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    var showLanguageBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.text_settings_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(SettingsEvent.NavigateBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 1. Privacy hero banner
                PrivacyGuaranteeHeroCard(
                    onClick = { onEvent(SettingsEvent.PrivacyPolicyClicked) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                /*// 2. Scanning preferences
                SettingsSectionHeader(title = stringResource(R.string.text_scan_and_cleanup))
                SettingsGroup {
                    SettingsClickableRow(
                        icon = Icons.Outlined.Security,
                        title = stringResource(R.string.title_manage_permissions),
                        subtitle = stringResource(R.string.desc_manage_permissions),
                        onClick = { onEvent(SettingsEvent.ManagePermissionsClick) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))*/

                // 3. Appearance & theme
                SettingsSectionHeader(title = stringResource(R.string.text_appearance))
                SettingsGroup {
                    SettingsSwitchRow(
                        icon = Icons.Outlined.DarkMode,
                        title = stringResource(R.string.title_dark_theme),
                        subtitle = stringResource(R.string.desc_dark_theme),
                        checked = uiState.theme == AppTheme.DARK,
                        onCheckedChange = { onEvent(SettingsEvent.ToggleDarkMode(it)) }
                    )
                    /*SettingsDivider()
                    SettingsClickableRow(
                        icon = Icons.Outlined.Language,
                        title = stringResource(R.string.title_language),
                        subtitle = "${uiState.language.flagEmoji}  ${uiState.language.nativeName}",
                        onClick = { showLanguageBottomSheet = true }
                    )*/
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Support & legal
                SettingsSectionHeader(title = stringResource(R.string.text_about_and_support))
                SettingsGroup {
                    /*SettingsClickableRow(
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        title = stringResource(R.string.title_feedback),
                        subtitle = stringResource(R.string.desc_feedback),
                        onClick = { onEvent(SettingsEvent.FeedbackClicked) }
                    )
                    SettingsDivider()*/
                    SettingsClickableRow(
                        icon = Icons.Outlined.PrivacyTip,
                        title = stringResource(R.string.title_privacy_policy),
                        subtitle = stringResource(R.string.desc_privacy_policy),
                        onClick = { onEvent(SettingsEvent.PrivacyPolicyClicked) }
                    )
                    SettingsDivider()
                    SettingsClickableRow(
                        icon = Icons.Outlined.PrivacyTip,
                        title = stringResource(R.string.title_terms_of_service),
                        subtitle = stringResource(R.string.desc_terms_of_service),
                        onClick = { onEvent(SettingsEvent.TermsOfServiceClicked) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. App version footer
                SettingsFooter(
                    version = uiState.appVersion,
                    tagline = stringResource(R.string.app_slogan)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (showLanguageBottomSheet) {
                LanguageSelectionBottomSheet(
                    currentLanguage = uiState.language,
                    onLanguageSelected = { selectedLang ->
                        onEvent(SettingsEvent.LanguageSelected(selectedLang))
                    },
                    onDismissRequest = { showLanguageBottomSheet = false }
                )
            }
        }
    }
}

