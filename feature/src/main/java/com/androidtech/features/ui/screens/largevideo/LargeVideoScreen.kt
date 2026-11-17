package com.androidtech.features.ui.screens.largevideo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.androidtech.base.permission.Permission
import com.androidtech.base.permission.rememberSinglePermission
import com.androidtech.base.theme.Body16Medium
import com.androidtech.base.theme.Title20Bold
import com.androidtech.base.utils.UiText
import com.androidtech.base.utils.formatSize
import com.androidtech.features.ui.components.DeleteBottomBar
import com.androidtech.features.ui.components.MediaPreviewScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun NavGraphBuilder.largeVideoScreen() {
    composable(
        route = AppDestination.LargeVideoScreen.route,
    ) {
        LargeVideoScreen()
    }
}

@Composable
fun LargeVideoScreen(
    viewModel: LargeVideoViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = LocalNavController.current
    val context = LocalContext.current

    val writePermissionState = rememberSinglePermission(
        permission = Permission.WriteExternalStorage,
        onGranted = {
            val selectedVideos =
                uiState.value.largeVideoGroups.flatMap { it.selectedVideoItems }.map { it.id }
            viewModel.handleEvent(LargeVideoEvent.DeleteClick(selectedVideos))
        }
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

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == android.app.Activity.RESULT_OK) {
            val selectedVideos =
                uiState.value.largeVideoGroups.flatMap { it.selectedVideoItems }.map { it.id }
            viewModel.handleEvent(LargeVideoEvent.ConfirmCacheCleanup(selectedVideos))
            showSnackbar(UiText.StringResource(R.string.text_success_clean).asString(context))
        }
    }

    LaunchedEffect(viewModel.uiEffect, lifecycleOwner) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle).collect { effect ->
            when (effect) {
                is LargeVideoEffect.NavigateBack -> {
                    navController.popBackStack()
                }

                is LargeVideoEffect.ShowSnackbar -> {
                    val message = effect.message.asString(context)
                    showSnackbar(message)
                }

                is LargeVideoEffect.RequestWritePermission -> {
                    if (writePermissionState.isGranted) {
                        viewModel.handleEvent(LargeVideoEvent.DeleteClick(effect.selectedVideoIds))
                    } else {
                        writePermissionState.request()
                    }
                }

                is LargeVideoEffect.LaunchIntentSender -> {
                    intentSenderLauncher.launch(
                        IntentSenderRequest.Builder(effect.intentSender).build()
                    )
                }
            }
        }
    }

    if (uiState.value.previewMediaList != null) {
        MediaPreviewScreen(
            mediaList = uiState.value.previewMediaList!!,
            initialIndex = uiState.value.previewInitialIndex,
            onBackClick = { viewModel.handleEvent(LargeVideoEvent.ClosePreview) }
        )
    } else {
        LargeVideoContent(
            uiState = uiState.value,
            onEvent = { viewModel.handleEvent(it) },
            snackbarHostState = snackbarHostState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeVideoContent(
    uiState: LargeVideoState,
    onEvent: (LargeVideoEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val groups = uiState.largeVideoGroups
    val totalSize: Long = groups.sumOf { it.potentialSavingsBytes }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.text_large_videos),
                            style = Title20Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = stringResource(
                                R.string.text_large_videos_description,
                                uiState.largeVideoGroups.size, totalSize.formatSize() ),
                            style = Body16Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(LargeVideoEvent.NavigateBackClick) },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    var showMoreVert by remember { mutableStateOf(false) }
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = { showMoreVert = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = "more",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreVert,
                            onDismissRequest = { showMoreVert = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.text_select_all)) },
                                onClick = {
                                    showMoreVert = false
                                    onEvent(LargeVideoEvent.SelectAllClicked)
                                }
                            )
                            if (uiState.isSelectionActive) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.text_clear_selection)) },
                                    onClick = {
                                        showMoreVert = false
                                        onEvent(LargeVideoEvent.ClearSelectionClicked)
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0)
            )
        },
        bottomBar = {
            val selectedCount = groups.sumOf { it.selectedVideoItemsCount }
            val totalSelectedVideos = groups.flatMap { it.selectedVideoItems }
            val totalSelectedSize = groups.sumOf { it.totalSelectedSize }

            Column {
                if (selectedCount > 0) {
                    DeleteBottomBar(
                        selectedCount = selectedCount,
                        sizeText = totalSelectedSize.formatSize(),
                        onDeleteClick = {
                            onEvent(LargeVideoEvent.DeleteClick(totalSelectedVideos.map { it.id }))
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            BannerAd(
                adHolder = uiState.bannerAdHolder,
                modifier = modifier.fillMaxWidth().background(Color.White)
            )

        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.showBackConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { onEvent(LargeVideoEvent.DismissBackConfirmation) },
                    title = { Text(text = stringResource(R.string.text_dialog_back_title)) },
                    text = { Text(text = stringResource(R.string.text_dialog_back_desc)) },
                    confirmButton = {
                        Button(onClick = { onEvent(LargeVideoEvent.ConfirmBackClicked) }) {
                            Text(stringResource(R.string.text_dialog_exit))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { onEvent(LargeVideoEvent.DismissBackConfirmation) }) {
                            Text(stringResource(R.string.text_dialog_cancel))
                        }
                    }
                )
            }
            if (groups.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_broom),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.text_no_large_video_found),
                        style = Title20Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.text_no_large_video_description),
                        style = Body16Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(groups, key = { it.groupId }) { group ->
                        LargeVideoGroupSection(
                            group = group,
                            onVideoClick = { videoId ->
                                onEvent(LargeVideoEvent.VideoClicked(videoId))
                            },
                            onVideoToggle = { videoId ->
                                onEvent(LargeVideoEvent.VideoToggleSelection(videoId))
                            },
                            onGroupToggle = { isSelected ->
                                onEvent(
                                    LargeVideoEvent.GroupToggleClicked(
                                        group.groupId,
                                        isSelected
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
