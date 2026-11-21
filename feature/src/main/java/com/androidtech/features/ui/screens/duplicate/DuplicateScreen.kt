package com.androidtech.features.ui.screens.duplicate

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

fun NavGraphBuilder.duplicateScreen() {
    composable(
        route = AppDestination.DuplicateScreen.route,
    ) {
        DuplicateScreen()
    }
}

@Composable
fun DuplicateScreen(
    viewModel: DuplicateViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = LocalNavController.current
    val context = LocalContext.current

    val writePermissionState = rememberSinglePermission(
        permission = Permission.WriteExternalStorage,
        onGranted = {
            val selectedPhotos =
                uiState.value.duplicateGroups.flatMap { it.selectedDuplicateItems }.map { it.id }
            viewModel.handleEvent(DuplicateEvent.DeleteClick(selectedPhotos))
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
            val selectedPhotos =
                uiState.value.duplicateGroups.flatMap { it.selectedDuplicateItems }.map { it.id }
            viewModel.handleEvent(DuplicateEvent.ConfirmCacheCleanup(selectedPhotos))
            showSnackbar(UiText.StringResource(R.string.text_success_clean).asString(context))
        }
    }

    LaunchedEffect(viewModel.uiEffect, lifecycleOwner) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle).collect { effect ->
            when (effect) {
                is DuplicateEffect.NavigateBack -> {
                    navController.popBackStack()
                }

                is DuplicateEffect.ShowSnackbar -> {
                    val message = effect.message.asString(context)
                    showSnackbar(message)
                }

                is DuplicateEffect.RequestWritePermission -> {
                    if (writePermissionState.isGranted) {
                        viewModel.handleEvent(DuplicateEvent.DeleteClick(effect.selectedPhotoIds))
                    } else {
                        writePermissionState.request()
                    }
                }

                is DuplicateEffect.LaunchIntentSender -> {
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
            onBackClick = { viewModel.handleEvent(DuplicateEvent.ClosePreview) }
        )
    } else {
        DuplicateContent(
            uiState = uiState.value,
            onEvent = { viewModel.handleEvent(it) },
            snackbarHostState = snackbarHostState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateContent(
    uiState: DuplicateState,
    onEvent: (DuplicateEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val duplicateGroups = uiState.duplicateGroups
    val totalSize: Long = duplicateGroups.sumOf { it.totalSizeDifferenceBytes }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.text_duplicate_photos),
                            style = Title20Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = stringResource(
                                R.string.text_duplicate_photos_description,
                                uiState.duplicateGroups.size,
                                totalSize.formatSize() ?: ""
                            ),
                            style = Body16Medium
                        )
                    }

                },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(DuplicateEvent.NavigateBackClick) },
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

                        // 3. DropdownMenu
                        DropdownMenu(
                            expanded = showMoreVert,
                            onDismissRequest = { showMoreVert = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.text_select_all)) },
                                onClick = {
                                    showMoreVert = false
                                    // Action
                                    onEvent(DuplicateEvent.SelectAllClicked)
                                }
                            )
                            if (uiState.isSelectionActive) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.text_clear_selection)) },
                                    onClick = {
                                        showMoreVert = false
                                        onEvent(DuplicateEvent.ClearSelectionClicked)
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
            val selectedCount = duplicateGroups.sumOf { it.selectedDuplicateItemsCount }
            val totalSelectedPhotos = duplicateGroups.flatMap { it.selectedDuplicateItems }
            val totalSelectedSize = duplicateGroups.sumOf { it.totalSelectedSize }
            Column {
                if (selectedCount > 0) {
                    DeleteBottomBar(
                        selectedCount = selectedCount,
                        sizeText = totalSelectedSize.formatSize(),
                        onDeleteClick = {
                            onEvent(DuplicateEvent.DeleteClick(totalSelectedPhotos.map { it.id }))
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                BannerAd(
                    adHolder = uiState.bannerAdHolder,
                    modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                )
            }

        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.showBackConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { onEvent(DuplicateEvent.DismissBackConfirmation) },
                    title = { Text(text = stringResource(R.string.text_dialog_back_title)) },
                    text = { Text(text = stringResource(R.string.text_dialog_back_desc)) },
                    confirmButton = {
                        Button(onClick = { onEvent(DuplicateEvent.ConfirmBackClicked) }) {
                            Text(stringResource(R.string.text_dialog_exit))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { onEvent(DuplicateEvent.DismissBackConfirmation) }) {
                            Text(stringResource(R.string.text_dialog_cancel))
                        }
                    }
                )
            }
            if (duplicateGroups.isEmpty()) {
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
                        text = stringResource(R.string.text_no_duplicates_found),
                        style = Title20Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.text_no_duplicates_description),
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
                    items(duplicateGroups, key = { it.groupId }) { group ->
                        DuplicateGroupSection(
                            group = group,
                            onItemClick = { itemId ->
                                onEvent(DuplicateEvent.PhotoClicked(itemId))
                            },
                            onPhotoToggle = { photoId ->
                                onEvent(
                                    DuplicateEvent.PhotoToggleSelection(
                                        photoId
                                    )
                                )
                            },
                            onGroupToggle = { isSelected ->
                                onEvent(
                                    DuplicateEvent.GroupToggleClicked(
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

