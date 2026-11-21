package com.androidtech.features.ui.screens.home

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.ads.monetization.ui.bannerad.BannerAd
import com.androidtech.base.R
import com.androidtech.base.extension.AppDestination
import com.androidtech.base.extension.LocalNavController
import com.androidtech.base.permission.MediaPermissionHelper
import com.androidtech.base.permission.MediaPermissionStatus
import com.androidtech.base.permission.Permission
import com.androidtech.base.permission.rememberMultiplePermissions
import com.androidtech.base.theme.*
import com.androidtech.base.ui.animation.bounceClick
import com.androidtech.base.utils.formatSize
import com.androidtech.domain.model.media.ScanLayer
import com.androidtech.domain.model.media.SmartInsightType
import com.androidtech.features.ui.components.PermissionRationaleDialog
import com.androidtech.features.ui.components.SettingsRedirectDialog
import com.androidtech.features.ui.screens.scan.badgeColor
import com.androidtech.features.ui.screens.scan.icon
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun NavGraphBuilder.homeScreen() {
    composable(
        route = AppDestination.HomeScreen.route,
        enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { it / 4 }, animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { it / 4 }, animationSpec = tween(300)) }
    ) {
        HomeScreen()
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = LocalNavController.current
    val context = LocalContext.current

    var permissionStatus by remember { mutableStateOf(MediaPermissionHelper.checkPermissionStatus(context)) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsRedirectDialog by remember { mutableStateOf(false) }

    val requiredPermissions = remember { MediaPermissionHelper.getRequiredPermissions() }
    val permissionState = rememberMultiplePermissions(
        permissions = requiredPermissions,
        onAllGranted = {
            permissionStatus = MediaPermissionHelper.checkPermissionStatus(context)
            viewModel.handleEvent(HomeEvent.FastScan)
        },
        onAnyDenied = { _ ->
            permissionStatus = MediaPermissionHelper.checkPermissionStatus(context)
        }
    )

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                permissionStatus = MediaPermissionHelper.checkPermissionStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(permissionStatus) {
        viewModel.handleEvent(HomeEvent.PermissionStatusChanged(permissionStatus == MediaPermissionStatus.FULL_ACCESS || permissionStatus == MediaPermissionStatus.PARTIAL_ACCESS))
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
                is HomeEffect.RequestStoragePermissions -> {
                    when (permissionStatus) {
                        MediaPermissionStatus.FULL_ACCESS -> {}
                        MediaPermissionStatus.PARTIAL_ACCESS, MediaPermissionStatus.DENIED -> {
                            if (!MediaPermissionHelper.hasRequestedBefore(context)) {
                                MediaPermissionHelper.setHasRequestedBefore(context, true)
                                permissionState.requestAll()
                            } else {
                                showRationaleDialog = true
                            }
                        }
                        MediaPermissionStatus.PERMANENTLY_DENIED -> {
                            showSettingsRedirectDialog = true
                        }
                    }
                }

                is HomeEffect.NavigateToSmartScan -> {
                    navController.navigate(AppDestination.ScanScreen.createRoute(ScanLayer.SMART.name))
                }

                is HomeEffect.NavigateToDeepScan -> {
                    navController.navigate(AppDestination.ScanScreen.createRoute(ScanLayer.DEEP.name))
                }

                is HomeEffect.NavigateToSettings -> {
                    navController.navigate(AppDestination.SettingsScreen.route)
                }

                is HomeEffect.NavigateToTrash -> {
                    navController.navigate(AppDestination.TrashScreen.route)
                }

                is HomeEffect.NavigateToDuplicate -> {
                    navController.navigate(AppDestination.DuplicateScreen.route)
                }

                is HomeEffect.NavigateToSimilar -> {
                    navController.navigate(AppDestination.SimilarScreen.route)
                }

                is HomeEffect.NavigateToScreenshot -> {
                    navController.navigate(AppDestination.ScreenshotScreen.route)
                }

                is HomeEffect.NavigateToLargeVideo -> {
                    navController.navigate(AppDestination.LargeVideoScreen.route)
                }

                is HomeEffect.NavigateToBlurry -> {
                    navController.navigate(AppDestination.BlurryScreen.route)
                }

                is HomeEffect.NavigateToOcrDocument -> {
                    navController.navigate(AppDestination.OcrDocumentScreen.route)
                }
            }
        }
    }

    if (showRationaleDialog) {
        PermissionRationaleDialog(
            onDismiss = { showRationaleDialog = false },
            onConfirm = {
                showRationaleDialog = false
                MediaPermissionHelper.setHasRequestedBefore(context, true)
                permissionState.requestAll()
            }
        )
    }

    if (showSettingsRedirectDialog) {
        SettingsRedirectDialog(
            onDismiss = { showSettingsRedirectDialog = false },
            onOpenSettings = {
                showSettingsRedirectDialog = false
                val intent = Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    "package:${context.packageName}".toUri()
                )
                context.startActivity(intent)
            }
        )
    }

    HomeContent(
        uiState = uiState.value,
        onEvent = { viewModel.handleEvent(it) },
        snackbarHostState = snackbarHostState
    )
}

@Preview(showBackground = true)
@Composable
fun HomeContentPreview(
) {
    AppTheme(
        theme = com.androidtech.domain.model.settings.AppTheme.LIGHT,
        dynamicColor = false
    ) {
        HomeContent(
            uiState = HomeState(),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeState,
    onEvent: (HomeEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
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
                actions = {
                    IconButton(
                        onClick = { onEvent(HomeEvent.TrashClicked) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_trash),
                            contentDescription = "trash",
                            tint = MaterialTheme.colorScheme.error.withAlpha7(),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onEvent(HomeEvent.SettingsClicked) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_gears),
                            contentDescription = "settings",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0)
            )
        },
        bottomBar = {
            BannerAd(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
                adHolder = uiState.bannerAdHolder
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            StorageRingGauge(
                usedGb = uiState.scanSummary.deviceUsedGb,
                totalGb = uiState.scanSummary.deviceTotalGb,
                availableGb = uiState.scanSummary.deviceFreeGb
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.isPermissionGranted) {
                ScanRecommendationCard(
                    onSmartScan = { onEvent(HomeEvent.SmartScanClicked) },
                    onDeepScan = { onEvent(HomeEvent.DeepScanClicked) },
                    isRecommended = uiState.isScanRecommended
                )

            } else {
                PermissionRequestCard(
                    onRequestPermissionClick = { onEvent(HomeEvent.RequestPermissionClicked) },
                    onPrivacyPolicyClick = {}
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isPermissionGranted) stringResource(R.string.text_smart_insights) else stringResource(
                        R.string.text_available_after_access
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = if (uiState.isPermissionGranted) painterResource(R.drawable.ic_info) else painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.withAlpha7(),
                    modifier = Modifier.size(22.dp)
                )
            }


            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val duplicatesSize = uiState.scanSummary.duplicatesSizeBytes ?: 0L
                val similaritySize = uiState.scanSummary.similaritySizeBytes ?: 0L
                CategoryCard(
                    title = stringResource(R.string.text_duplicate_photos),
                    sizeText = duplicatesSize.formatSize(),
                    isPermissionGranted = uiState.isPermissionGranted,
                    icon = SmartInsightType.EXACT_DUPLICATES.icon,
                    badgeColor = SmartInsightType.EXACT_DUPLICATES.badgeColor,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(HomeEvent.SmartInsightTypeClick(SmartInsightType.EXACT_DUPLICATES)) }
                )
                CategoryCard(
                    title = stringResource(R.string.text_similar_photos),
                    sizeText = similaritySize.formatSize(),
                    isPermissionGranted = uiState.isPermissionGranted,
                    icon = SmartInsightType.SIMILAR_PHOTOS.icon,
                    badgeColor = SmartInsightType.SIMILAR_PHOTOS.badgeColor,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(HomeEvent.SmartInsightTypeClick(SmartInsightType.SIMILAR_PHOTOS)) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryCard(
                    title = stringResource(R.string.text_screenshots_old),
                    sizeText = uiState.scanSummary.screenshotBytes.formatSize(),
                    isPermissionGranted = uiState.isPermissionGranted,
                    icon = SmartInsightType.SCREENSHOTS.icon,
                    badgeColor = SmartInsightType.SCREENSHOTS.badgeColor,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(HomeEvent.SmartInsightTypeClick(SmartInsightType.SCREENSHOTS)) }
                )
                CategoryCard(
                    title = stringResource(R.string.text_large_videos),
                    sizeText = uiState.scanSummary.largeVideoBytes.formatSize(),
                    isPermissionGranted = uiState.isPermissionGranted,
                    icon = SmartInsightType.LARGE_VIDEOS.icon,
                    badgeColor = SmartInsightType.LARGE_VIDEOS.badgeColor,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(HomeEvent.SmartInsightTypeClick(SmartInsightType.LARGE_VIDEOS)) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryCard(
                    title = stringResource(R.string.text_blurry_photos),
                    sizeText = uiState.scanSummary.blurrySizeBytes.formatSize(),
                    isPermissionGranted = uiState.isPermissionGranted,
                    icon = SmartInsightType.BLURRY_PHOTOS.icon,
                    badgeColor = SmartInsightType.BLURRY_PHOTOS.badgeColor,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(HomeEvent.SmartInsightTypeClick(SmartInsightType.BLURRY_PHOTOS)) }
                )
                CategoryCard(
                    title = stringResource(R.string.text_ocr_document),
                    sizeText = uiState.scanSummary.documentBytes.formatSize(),
                    isPermissionGranted = uiState.isPermissionGranted,
                    icon = SmartInsightType.DEEP_OCR_DOCUMENTS.icon,
                    badgeColor = SmartInsightType.DEEP_OCR_DOCUMENTS.badgeColor,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(HomeEvent.SmartInsightTypeClick(SmartInsightType.DEEP_OCR_DOCUMENTS)) }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.app_slogan),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
