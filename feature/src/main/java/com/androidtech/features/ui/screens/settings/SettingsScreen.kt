package com.androidtech.features.ui.screens.settings

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                        style = Title20Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(SettingsEvent.NavigateBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
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
                Spacer(modifier = Modifier.height(6.dp))

                // 1. Privacy Banner Section
                PrivacyGuaranteeHeroCard(
                    onPrivacyPolicyClick = { onEvent(SettingsEvent.PrivacyPolicyClicked) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Section: Scanning Preferences
                SettingsSectionHeader(title = stringResource(R.string.text_scan_and_cleanup))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        /*SettingsSwitchRow(
                        icon = Icons.Outlined.FolderDelete,
                        title = "Include Downloads folder",
                        subtitle = "Scan files in the system Downloads directory",
                        checked = includeDownloadsState,
                        onCheckedChange = { includeDownloadsState = it }
                    )*/
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                        SettingsClickableRow(
                            icon = Icons.Outlined.Security,
                            title = "Manage App Permissions",
                            subtitle = "Check or revoke storage access in Android settings",
                            onClick = { onEvent(SettingsEvent.ManagePermissionsClick) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 3. Section: Appearance & Theme
                SettingsSectionHeader(title = stringResource(R.string.text_appearance))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsSwitchRow(
                            icon = Icons.Outlined.DarkMode,
                            title = "Dark Theme",
                            subtitle = "Reduce eye strain and battery usage",
                            checked = uiState.theme == AppTheme.DARK,
                            onCheckedChange = { onEvent(SettingsEvent.ToggleDarkMode(it)) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingsClickableRow(
                            icon = Icons.Outlined.Language,
                            title = "Language",
                            subtitle = "${uiState.language.flagEmoji}  ${uiState.language.nativeName}",
                            onClick = { showLanguageBottomSheet = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 4. Section: Support & Legal
                SettingsSectionHeader(title = stringResource(R.string.text_about_and_support))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsClickableRow(
                            icon = Icons.AutoMirrored.Outlined.HelpOutline,
                            title = "Send Feedback & Support",
                            subtitle = "Report a bug or suggest improvements",
                            onClick = { onEvent(SettingsEvent.FeedbackClicked) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                        SettingsClickableRow(
                            icon = Icons.Outlined.PrivacyTip,
                            title = "Privacy Policy",
                            subtitle = "Read our 100% local processing commitment",
                            onClick = { onEvent(SettingsEvent.PrivacyPolicyClicked) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                // 5. App Version Footer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SnapSweep v${uiState.appVersion}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Clean your gallery. Free your storage.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                }
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