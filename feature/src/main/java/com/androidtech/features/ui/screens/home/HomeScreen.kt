package com.androidtech.features.ui.screens.home

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
import com.androidtech.base.theme.*
import com.androidtech.base.ui.animation.bounceClick
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun NavGraphBuilder.homeScreen() {
    composable(
        route = AppDestination.HomeScreen.route,

        ) {
        HomeScreen(
        )
    }
}

/*@Composable
@Preview
private fun PreviewHomeScreen() {
    HomeScreen(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    )
}*/

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
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
                is HomeEffect.NavigateToScan -> {
                    //navController.navigate(AppDestination.ScanScreen.route)
                }
                is HomeEffect.NavigateToSettings -> {

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

                        /*if(uiState.isPremiumUser) {
                            Icon(
                                painter = painterResource(R.drawable.img_premium),
                                contentDescription = null,
                                tint = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }*/
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

            // 1. Storage Progress Gauge
            StorageRingGauge(
                usedGb = uiState.storageStats.usedGb,
                totalGb = uiState.storageStats.totalGb,
                availableGb = uiState.storageStats.freeGb
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Potential Cleanup Card
            PotentialCleanupCard(potentialGb = uiState.storageStats.duplicatesSizeBytes.formatSize())

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Scan Button CTA
            Button(
                onClick = { onEvent(HomeEvent.ScanClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .bounceClick(onClick = { onEvent(HomeEvent.ScanClicked) }),
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
                    text = stringResource(R.string.text_scan_storage),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4. Smart Cleanup Grid
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
                CategoryCard(
                    title = "Duplicate photos",
                    sizeText = uiState.storageStats.duplicatesSizeBytes.formatSize(),
                    icon = Icons.Outlined.ContentCopy,
                    badgeColor = ImageBadge,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(HomeEvent.CategoryClick("duplicates")) }
                )
                CategoryCard(
                    title = "Similar photos",
                    sizeText = uiState.storageStats.photosSizeBytes.formatSize(),
                    icon = Icons.Outlined.Collections,
                    badgeColor = AudioBadge,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(HomeEvent.CategoryClick("duplicates")) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryCard(
                    title = "Large videos",
                    sizeText = uiState.storageStats.videosSizeBytes.formatSize(),
                    icon = Icons.Outlined.VideoLibrary,
                    badgeColor = VideoBadge,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(HomeEvent.CategoryClick("duplicates")) }
                )
                CategoryCard(
                    title = "Screenshots",
                    sizeText = uiState.storageStats.screenshotsSizeBytes.formatSize(),
                    icon = Icons.Outlined.Smartphone,
                    badgeColor = DocumentBadge,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(HomeEvent.CategoryClick("duplicates")) }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 5. Privacy Reassurance Banner
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