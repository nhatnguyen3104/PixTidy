package com.androidtech.features.ui.screens.screenshot

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import com.androidtech.features.ui.screens.duplicate.DuplicateEffect
import com.androidtech.features.ui.screens.duplicate.DuplicateEvent
import com.androidtech.features.ui.screens.duplicate.DuplicateScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun NavGraphBuilder.screenshotScreen() {
    composable(
        route = AppDestination.ScreenshotScreen.route,
    ) {
        ScreenshotScreen()
    }
}
@Composable
fun ScreenshotScreen(
    viewModel: ScreenshotViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = LocalNavController.current
    val context = LocalContext.current

    val writePermissionState = rememberSinglePermission(
        permission = Permission.WriteExternalStorage,
        onGranted = {
            val selectedPhotos =
                uiState.value.screenshotGroups.flatMap { it.selectedScreenshotItems }.map { it.id }
            viewModel.handleEvent(ScreenshotEvent.DeleteClick(selectedPhotos))
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
                uiState.value.screenshotGroups.flatMap { it.selectedScreenshotItems }.map { it.id }
            viewModel.handleEvent(ScreenshotEvent.ConfirmCacheCleanup(selectedPhotos))
            showSnackbar(UiText.StringResource(R.string.text_success_clean).asString(context))
        }
    }

    LaunchedEffect(viewModel.uiEffect, lifecycleOwner) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle).collect { effect ->
            when (effect) {
                is ScreenshotEffect.NavigateBack -> {
                    navController.popBackStack()
                }

                is ScreenshotEffect.ShowSnackbar -> {
                    val message = effect.message.asString(context)
                    showSnackbar(message)
                }

                is ScreenshotEffect.RequestWritePermission -> {
                    if (writePermissionState.isGranted) {
                        viewModel.handleEvent(ScreenshotEvent.DeleteClick(effect.selectedPhotoIds))
                    } else {
                        writePermissionState.request()
                    }
                }

                is ScreenshotEffect.LaunchIntentSender -> {
                    intentSenderLauncher.launch(
                        IntentSenderRequest.Builder(effect.intentSender).build()
                    )
                }
            }
        }
    }

    ScreenshotContent(
        uiState = uiState.value,
        onEvent = { viewModel.handleEvent(it) },
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotContent(
    uiState: ScreenshotState,
    onEvent: (ScreenshotEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val screenshotGroups = uiState.screenshotGroups
    val totalSize: Long = screenshotGroups.sumOf { it.potentialSavingsBytes }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.text_screenshots_old),
                            style = Title20Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = stringResource(
                                R.string.text_screenshots_description,
                                uiState.screenshotGroups.size,
                                totalSize.formatSize() ?: ""
                            ),
                            style = Body16Medium
                        )
                    }

                },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(ScreenshotEvent.NavigateBackClick) },
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
                                    onEvent(ScreenshotEvent.SelectAllClicked)
                                }
                            )
                            if (uiState.isSelectionActive) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.text_clear_selection)) },
                                    onClick = {
                                        showMoreVert = false
                                        onEvent(ScreenshotEvent.ClearSelectionClicked)
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
            val selectedCount = screenshotGroups.sumOf { it.selectedScreenshotItemsCount }
            val totalSelectedPhotos = screenshotGroups.flatMap { it.selectedScreenshotItems }
            val totalSelectedSize = screenshotGroups.sumOf { it.totalSelectedSize }
            if (selectedCount > 0) {
                DeleteBottomBar(
                    selectedCount = selectedCount,
                    sizeText = totalSelectedSize.formatSize(),
                    onDeleteClick = {
                        onEvent(ScreenshotEvent.DeleteClick(totalSelectedPhotos.map { it.id }))
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.showBackConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { onEvent(ScreenshotEvent.DismissBackConfirmation) },
                    title = { Text(text = stringResource(R.string.text_dialog_back_title)) },
                    text = { Text(text = stringResource(R.string.text_dialog_back_desc)) },
                    confirmButton = {
                        Button(onClick = { onEvent(ScreenshotEvent.ConfirmBackClicked) }) {
                            Text(stringResource(R.string.text_dialog_exit))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { onEvent(ScreenshotEvent.DismissBackConfirmation) }) {
                            Text(stringResource(R.string.text_dialog_cancel))
                        }
                    }
                )
            }
            if (screenshotGroups.isEmpty()) {
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
                        text = stringResource(R.string.text_no_screenshots_found),
                        style = Title20Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.text_no_screenshots_description),
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
                    items(screenshotGroups, key = { it.groupId }) { group ->
                        ScreenshotTimeGroupSection(
                            group = group,
                            onPhotoToggle = { photoId ->
                                onEvent(
                                    ScreenshotEvent.PhotoToggleSelection(
                                        photoId
                                    )
                                )
                            },
                            onGroupToggle = { isSelected ->
                                onEvent(
                                    ScreenshotEvent.GroupToggleClicked(
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