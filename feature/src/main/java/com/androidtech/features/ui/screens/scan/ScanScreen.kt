package com.androidtech.features.ui.screens.scan

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.androidtech.base.R
import com.androidtech.base.extension.AppDestination
import com.androidtech.base.extension.LocalNavController
import com.androidtech.base.theme.*
import com.androidtech.base.utils.formatSize
import com.androidtech.domain.model.media.SmartInsightType
import com.androidtech.domain.model.media.ScanLayer
import com.androidtech.domain.model.media.ScanProcess
import com.androidtech.domain.model.media.SmartInsight

fun NavGraphBuilder.scanScreen() {
    composable(
        route = AppDestination.ScanScreen.route,
        arguments = listOf(navArgument("mode") { type = NavType.StringType })
    ) {
        ScanScreen()
    }
}

@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.scanProcess) {
        if (uiState.scanProcess is ScanProcess.Idle) {
            viewModel.handleEvent(ScanEvent.StartScan)
        }
    }

    LaunchedEffect(viewModel.uiEffect, lifecycleOwner) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle).collect { effect ->
            when (effect) {
                is ScanEffect.NavigateBack -> navController.popBackStack()
                is ScanEffect.NavigateToDuplicate -> {
                    navController.navigate(AppDestination.DuplicateScreen.route)
                }
                is ScanEffect.NavigateToSimilar -> {
                    navController.navigate(AppDestination.SimilarScreen.route)
                }
                is ScanEffect.NavigareToScreenshot -> {
                    navController.navigate(AppDestination.ScreenshotScreen.route)
                }
            }
        }
    }

    ScanContent(
        uiState = uiState,
        onEvent = { viewModel.handleEvent(it) },
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanContent(
    uiState: ScanState,
    onEvent: (ScanEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val titleRes = if (uiState.scanLayer == ScanLayer.SMART) {
        R.string.text_smart_scan_storage
    } else {
        R.string.text_intelligent_scan_storage
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(titleRes),
                        style = Title20Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ScanEvent.NavigateBackClick) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = uiState.scanProcess,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "ScanProgressAnimation",
                contentKey = { state -> state::class.simpleName }
            ) { process ->
                when (process) {
                    is ScanProcess.Idle -> {
                        ScanInfoPlaceholder(
                            title = "Ready to Scan",
                            description = "Press the button below to start finding duplicates.",
                            icon = Icons.Default.CheckCircle
                        )
                    }

                    is ScanProcess.Scanning -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ScanProgressGauge(progress = process.progress.progressPercentage / 100f)
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = stringResource(R.string.text_canning_storage),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = process.progress.currentStepMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.text_item_found, process.progress.processedItems),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    is ScanProcess.Success -> {
                        val duplicatesInsight = remember(process.insights) {
                            process.insights.firstOrNull { it.type == SmartInsightType.EXACT_DUPLICATES || it.type == SmartInsightType.DUPLICATE_VIDEOS }
                        }
                        val duplicatesSize = duplicatesInsight?.reclaimableBytes ?: process.summary.duplicatesSizeBytes ?: 0L
                        val duplicatesCount = duplicatesInsight?.affectedItemsCount ?: 0

                        val similarityInsight = remember(process.insights) {
                            process.insights.firstOrNull { it.type == SmartInsightType.SIMILAR_PHOTOS }
                        }
                        val similaritySize = similarityInsight?.reclaimableBytes ?: process.summary.similaritySizeBytes ?: 0L
                        val similarityCount = similarityInsight?.affectedItemsCount ?: 0

                        val screenshotSize = process.summary.screenshotBytes
                        val screenshotCount = process.summary.screenshotCount

                        val videosSize = process.summary.videosSizeBytes

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ReportHeroCard(
                                totalCleanableSize = process.totalReclaimableBytes.formatSize()
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.text_scan_insights),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.text_groups, 4),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 1. Duplicate Photos
                            ScanCategoryCard(
                                title = stringResource(R.string.text_duplicate_photos),
                                sizeText = "$duplicatesCount items • ${duplicatesSize.formatSize()}",
                                icon = Icons.Outlined.ContentCopy,
                                badgeColor = ImageBadge,
                                onClick = {
                                    onEvent(ScanEvent.SmartInsightTypeClick(SmartInsightType.EXACT_DUPLICATES))
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 2. Similar Photos
                            ScanCategoryCard(
                                title = stringResource(R.string.text_similar_photos),
                                sizeText = "$similarityCount items • ${similaritySize.formatSize()}",
                                icon = Icons.Outlined.Collections,
                                badgeColor = AudioBadge,
                                onClick = {
                                    onEvent(ScanEvent.SmartInsightTypeClick(SmartInsightType.SIMILAR_PHOTOS))
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 3. Screenshots
                            ScanCategoryCard(
                                title = stringResource(R.string.text_screenshots_old),
                                sizeText = "$screenshotCount items • ${screenshotSize.formatSize()}",
                                icon = Icons.Outlined.Smartphone,
                                badgeColor = DocumentBadge,
                                onClick = {
                                    onEvent(ScanEvent.SmartInsightTypeClick(SmartInsightType.SCREENSHOTS))
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 4. Large Videos (Disabled / Coming Soon)
                            ScanCategoryCard(
                                title = stringResource(R.string.text_large_videos),
                                sizeText = videosSize.formatSize(),
                                icon = Icons.Outlined.VideoLibrary,
                                badgeColor = VideoBadge,
                                isEnabled = false,
                                comingSoonText = "Soon",
                                onClick = {}
                            )
                        }
                    }

                    is ScanProcess.Error -> {
                        ScanInfoPlaceholder(
                            title = stringResource(R.string.text_scan_failed),
                            description = process.throwable.message ?: stringResource(R.string.text_unknow_error),
                            icon = Icons.Default.ErrorOutline,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (uiState.scanProcess is ScanProcess.Scanning) {
                Button(
                    onClick = { onEvent(ScanEvent.CancelScanClick) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(text = stringResource(R.string.text_cancel_scan), fontWeight = FontWeight.Bold)
                }
            } else if (uiState.scanProcess is ScanProcess.Success || uiState.scanProcess is ScanProcess.Error) {
                Button(
                    onClick = { onEvent(ScanEvent.NavigateBackClick) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(text = stringResource(R.string.text_go_back), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}



@Composable
private fun ScanInfoPlaceholder(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = color
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
