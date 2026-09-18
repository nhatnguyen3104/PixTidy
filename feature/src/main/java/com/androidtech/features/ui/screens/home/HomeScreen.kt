package com.androidtech.features.ui.screens.home

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.androidtech.base.R
import com.androidtech.base.extension.AppDestination
import com.androidtech.base.extension.LocalNavController
import com.androidtech.base.permission.Permission
import com.androidtech.base.permission.rememberMultiplePermissions
import com.androidtech.base.theme.*
import com.androidtech.base.ui.animation.bounceClick
import com.androidtech.base.utils.formatSize
import com.androidtech.domain.model.media.ScanLayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun NavGraphBuilder.homeScreen() {
    composable(
        route = AppDestination.HomeScreen.route,
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

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Permission.ReadMediaImages, Permission.ReadMediaVideos)
    } else {
        listOf(Permission.ReadExternalStorage)
    }

    val permissionState = rememberMultiplePermissions(
        permissions = permissions.toTypedArray(),
        onAllGranted = { viewModel.handleEvent(HomeEvent.FastScan) }
    )

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
                    if (!permissionState.allGranted) {
                        permissionState.requestAll()
                    }
                }
                is HomeEffect.NavigateToSmartScan -> {
                    navController.navigate(AppDestination.ScanScreen.createRoute(ScanLayer.SMART.name))
                }
                is HomeEffect.NavigateToDeepScan -> {
                    navController.navigate(AppDestination.ScanScreen.createRoute(ScanLayer.DEEP.name))
                }
                is HomeEffect.NavigateToSettings -> {

                }
                is HomeEffect.NavigateToDuplicate -> {
                    navController.navigate(AppDestination.DuplicateScreen.route)
                }
                is HomeEffect.NavigateToSimilar -> {
                    navController.navigate(AppDestination.SimilarScreen.route)
                }
            }
        }
    }
    HomeContent(
        uiState = uiState.value,
        onEvent = { viewModel.handleEvent(it) },
        snackbarHostState = snackbarHostState
    )
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
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_viet_nam),
                            contentDescription = "logo",
                            tint = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = Title20Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(HomeEvent.SettingsClicked) }, modifier = Modifier.size(40.dp)) {
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
                windowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0 )
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
            Spacer(modifier = Modifier.height(12.dp))

            StorageRingGauge(
                usedGb = uiState.scanSummary.deviceUsedGb,
                totalGb = uiState.scanSummary.deviceTotalGb,
                availableGb = uiState.scanSummary.deviceFreeGb
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onEvent(HomeEvent.DeepScanClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .bounceClick(onClick = { onEvent(HomeEvent.DeepScanClicked) }),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.text_intelligent_scan_storage),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onEvent(HomeEvent.SmartScanClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .bounceClick(onClick = { onEvent(HomeEvent.SmartScanClicked) }),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.text_smart_scan_storage),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Smart cleanup",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )

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
                    icon = Icons.Outlined.ContentCopy,
                    badgeColor = ImageBadge,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(HomeEvent.CategoryClick(Category.DUPLICATE)) }
                )
                CategoryCard(
                    title = stringResource(R.string.text_similar_photos),
                    sizeText = similaritySize.formatSize(),
                    icon = Icons.Outlined.Collections,
                    badgeColor = AudioBadge,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(HomeEvent.CategoryClick(Category.SIMILAR)) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryCard(
                    title = stringResource(R.string.text_large_videos),
                    sizeText = uiState.scanSummary.videosSizeBytes.formatSize(),
                    icon = Icons.Outlined.VideoLibrary,
                    badgeColor = VideoBadge,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(HomeEvent.CategoryClick(Category.LARGE_VIDEOS)) }
                )
                CategoryCard(
                    title = stringResource(R.string.text_screenshots),
                    sizeText = uiState.scanSummary.screenshotBytes.formatSize(),
                    icon = Icons.Outlined.Smartphone,
                    badgeColor = DocumentBadge,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(HomeEvent.CategoryClick(Category.SCREENSHOTS)) }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "YOUR FILES STAY ON YOUR DEVICE",
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
