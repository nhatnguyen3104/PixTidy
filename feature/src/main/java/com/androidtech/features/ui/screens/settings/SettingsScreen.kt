package com.androidtech.features.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ads.monetization.ui.bannerad.BannerAd
import com.androidtech.base.R
import com.androidtech.base.extension.AppDestination
import com.androidtech.base.extension.LocalNavController
import com.androidtech.base.model.LanguageModel
import com.androidtech.base.permission.MediaPermissionHelper
import com.androidtech.base.permission.MediaPermissionStatus
import com.androidtech.base.permission.rememberMultiplePermissions
import com.androidtech.domain.model.settings.AppTheme
import com.androidtech.features.ui.components.PermissionRationaleDialog
import com.androidtech.features.ui.components.SettingsRedirectDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun NavGraphBuilder.settingsScreen(
    onCheckForUpdates: (onUpToDate: () -> Unit) -> Unit = {}
) {
    composable(
        route = AppDestination.SettingsScreen.route,
    ) {
        SettingsScreen(onCheckForUpdates = onCheckForUpdates)
    }
}

@Composable
fun SettingsScreen(
    onCheckForUpdates: (onUpToDate: () -> Unit) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = LocalNavController.current
    val context = LocalContext.current

    val requiredPermissions = remember { MediaPermissionHelper.getRequiredPermissions() }
    val permissionState = rememberMultiplePermissions(
        permissions = requiredPermissions,
        onAllGranted = {
            viewModel.handleEvent(SettingsEvent.UpdatePermissionStatus(MediaPermissionStatus.FULL_ACCESS))
        },
        onAnyDenied = { _ ->
            val status = MediaPermissionHelper.checkPermissionStatus(context)
            viewModel.handleEvent(SettingsEvent.UpdatePermissionStatus(status))
        }
    )

    LaunchedEffect(Unit) {
        val status = MediaPermissionHelper.checkPermissionStatus(context)
        viewModel.handleEvent(SettingsEvent.UpdatePermissionStatus(status))
    }

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
                    showSnackbar(effect.message.asString(context))
                }
                is SettingsEffect.OpenWebUrl -> {
                    val intent = Intent(Intent.ACTION_VIEW, effect.url.toUri())
                    context.startActivity(intent)
                }
                is SettingsEffect.OpenAppPermissions -> {
                    val intent = Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        "package:${context.packageName}".toUri()
                    )
                    context.startActivity(intent)
                }
                is SettingsEffect.RequestPermissions -> {
                    MediaPermissionHelper.setHasRequestedBefore(context, true)
                    permissionState.requestAll()
                }
                is SettingsEffect.CheckForUpdates -> {
                    onCheckForUpdates {
                        viewModel.handleEvent(SettingsEvent.ShowUpToDateSnackbar)
                    }
                }
            }
        }
    }

    if (uiState.value.showRationaleDialog) {
        PermissionRationaleDialog(
            onDismiss = { viewModel.handleEvent(SettingsEvent.DismissRationaleDialog) },
            onConfirm = { viewModel.handleEvent(SettingsEvent.ConfirmRationaleDialog) }
        )
    }

    if (uiState.value.showSettingsRedirectDialog) {
        SettingsRedirectDialog(
            onDismiss = { viewModel.handleEvent(SettingsEvent.DismissSettingsRedirectDialog) },
            onOpenSettings = { viewModel.handleEvent(SettingsEvent.ConfirmSettingsRedirectDialog) }
        )
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
    val currentLangModel = LanguageModel.getSupportedLanguageByCode(uiState.languageCode) ?: LanguageModel.ENGLISH

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
                windowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BannerAd(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White),
                adHolder = uiState.bannerAdHolder
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

                // 2. Scanning preferences
                SettingsSectionHeader(title = stringResource(R.string.text_scan_and_cleanup))
                SettingsGroup {
                    SettingsClickableRow(
                        icon = Icons.Outlined.Security,
                        title = stringResource(R.string.title_manage_permissions),
                        subtitle = when (uiState.permissionStatus) {
                            MediaPermissionStatus.FULL_ACCESS -> stringResource(R.string.text_full_access)
                            MediaPermissionStatus.PARTIAL_ACCESS -> stringResource(R.string.text_partial_access)
                            MediaPermissionStatus.DENIED -> stringResource(R.string.text_denied)
                            MediaPermissionStatus.PERMANENTLY_DENIED -> stringResource(R.string.text_permanently_denied)
                        },
                        onClick = { onEvent(SettingsEvent.ManagePermissionsClick) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                    SettingsDivider()
                    SettingsClickableRow(
                        icon = Icons.Outlined.Language,
                        title = stringResource(R.string.text_select_language),
                        subtitle = currentLangModel.displayName,
                        onClick = { showLanguageBottomSheet = true }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Support & legal
                SettingsSectionHeader(title = stringResource(R.string.text_about_and_support))
                SettingsGroup {
                    SettingsClickableRow(
                        icon = Icons.Outlined.SystemUpdate,
                        title = stringResource(R.string.title_check_for_updates),
                        subtitle = stringResource(R.string.desc_check_for_updates),
                        onClick = { onEvent(SettingsEvent.CheckForUpdatesClicked) }
                    )
                    SettingsDivider()
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
                    version = stringResource(R.string.app_version),
                    tagline = stringResource(R.string.app_slogan)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (showLanguageBottomSheet) {
                LanguageSelectionBottomSheet(
                    currentLanguageCode = uiState.languageCode,
                    onLanguageSelected = { selectedLang ->
                        onEvent(SettingsEvent.LanguageSelected(selectedLang))
                    },
                    onDismissRequest = { showLanguageBottomSheet = false }
                )
            }
        }
    }
}
