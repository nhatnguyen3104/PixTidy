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

    LaunchedEffect(Unit) {
        viewModel.handleEvent(ScanEvent.StartScanClick)
    }

    LaunchedEffect(viewModel.uiEffect, lifecycleOwner) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle).collect { effect ->
            when (effect) {
                is ScanEffect.NavigateBack -> navController.popBackStack()
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
    val titleRes = if (uiState.scanLayer == ScanLayer.SMART_SCAN) {
        R.string.text_smart_scan_storage
    } else {
        R.string.text_deep_scan_storage
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
                                text = "Scanning Storage...",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = process.progress.currentStepMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Items found: ${process.progress.totalItems}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    is ScanProcess.Success -> {
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
                                    text = "Scan Insights",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${process.insights.size} Groups",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            process.insights.forEach { insight ->
                                ResultCategoryRow(
                                    category = mapInsightToCategory(insight),
                                    onClick = { /* Navigate to detail if needed */ }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    is ScanProcess.Error -> {
                        ScanInfoPlaceholder(
                            title = "Scan Failed",
                            description = process.throwable.message ?: "Unknown error",
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
                    Text(text = "Cancel Scan", fontWeight = FontWeight.Bold)
                }
            } else if (uiState.scanProcess is ScanProcess.Success || uiState.scanProcess is ScanProcess.Error) {
                Button(
                    onClick = { onEvent(ScanEvent.NavigateBackClick) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(text = "Go Back", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun mapInsightToCategory(insight: SmartInsight): ScanResultCategory {
    val icon = when (insight.type) {
        SmartInsightType.EXACT_DUPLICATES -> Icons.Default.CopyAll
        SmartInsightType.SIMILAR_PHOTOS -> Icons.Default.PhotoLibrary
        SmartInsightType.SCREENSHOTS -> Icons.Default.Screenshot
        SmartInsightType.LARGE_VIDEOS -> Icons.Default.VideoFile
        SmartInsightType.BLURRY_PHOTOS -> Icons.Default.BlurOn
        SmartInsightType.DEEP_OCR_DOCUMENTS -> Icons.Default.Description
    }

    val badgeColor = when (insight.type) {
        SmartInsightType.EXACT_DUPLICATES -> DocumentBadge
        SmartInsightType.SIMILAR_PHOTOS -> ImageBadge
        SmartInsightType.SCREENSHOTS -> StorageBadge
        SmartInsightType.LARGE_VIDEOS -> VideoBadge
        SmartInsightType.BLURRY_PHOTOS -> ArchiveBadge
        SmartInsightType.DEEP_OCR_DOCUMENTS -> DocumentBadge
    }.copy(alpha = 0.2f)

    val iconTint = when (insight.type) {
        SmartInsightType.EXACT_DUPLICATES -> DocumentBadge
        SmartInsightType.SIMILAR_PHOTOS -> ImageBadge
        SmartInsightType.SCREENSHOTS -> StorageBadge
        SmartInsightType.LARGE_VIDEOS -> VideoBadge
        SmartInsightType.BLURRY_PHOTOS -> ArchiveBadge
        SmartInsightType.DEEP_OCR_DOCUMENTS -> DocumentBadge
    }

    return ScanResultCategory(
        title = insight.title,
        sizeText = "${insight.affectedItemsCount} items • ${insight.reclaimableBytes.formatSize()}",
        icon = icon,
        iconTint = iconTint,
        badgeBgColor = badgeColor
    )
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
