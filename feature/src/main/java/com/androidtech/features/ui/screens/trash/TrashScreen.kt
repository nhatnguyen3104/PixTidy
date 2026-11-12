package com.androidtech.features.ui.screens.trash

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import com.androidtech.base.theme.Title20Bold
import com.androidtech.base.utils.UiText
import com.androidtech.base.utils.formatSize
import com.androidtech.features.ui.components.MediaPreviewScreen
import com.androidtech.features.ui.components.RestoreDeleteBottomBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun NavGraphBuilder.trashScreen() {
    composable(
        route = AppDestination.TrashScreen.route,
    ) {
        TrashScreen()
    }
}

@Composable
fun TrashScreen(
    viewModel: TrashViewModel = hiltViewModel()
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

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == android.app.Activity.RESULT_OK) {
            showSnackbar(UiText.StringResource(R.string.text_success_clean).asString(context))
        }
    }

    LaunchedEffect(viewModel.uiEffect, lifecycleOwner) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle).collect { effect ->
            when (effect) {
                is TrashEffect.NavigateBack -> {
                    navController.popBackStack()
                }
                is TrashEffect.ShowSnackbar -> {
                    val message = effect.message.asString(context)
                    showSnackbar(message)
                }
                is TrashEffect.LaunchIntentSender -> {
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
            onBackClick = { viewModel.handleEvent(TrashEvent.ClosePreview) }
        )
    } else {
        TrashContent(
            uiState = uiState.value,
            onEvent = { viewModel.handleEvent(it) },
            isLoading = uiState.value.isLoading,
            snackbarHostState = snackbarHostState
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashContent(
    uiState: TrashState,
    onEvent: (TrashEvent) -> Unit,
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val mediaItems = uiState.trashMedia
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.text_trash),
                            style = Title20Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(TrashEvent.NavigateBackClick) },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                                    onEvent(TrashEvent.SelectAllClicked)
                                }
                            )
                            if (uiState.isSelectionActive) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.text_clear_selection)) },
                                    onClick = {
                                        showMoreVert = false
                                        onEvent(TrashEvent.ClearSelectionClicked)
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
            val selectedMedia = mediaItems.filter { it.isSelected }
            val selectedCount =  mediaItems.count { it.isSelected }
            val selectedSize = selectedMedia.sumOf { it.size }

            if (selectedCount > 0) {
                RestoreDeleteBottomBar(
                    selectedCount = selectedCount,
                    sizeText = selectedSize.formatSize(),
                    onRestoreClick = {
                        onEvent(TrashEvent.RestoreClick(selectedMedia.map { it.id }))
                    },
                    onDeleteClick = {
                        onEvent(TrashEvent.DeleteClick(selectedMedia.map { it.id }))
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                mediaItems.isNotEmpty() -> TrashGrid(
                    items = mediaItems,
                    onMediaClick = { onEvent(TrashEvent.OnItemClick(it))},
                    onMediaToggle = { onEvent(TrashEvent.ToggleItem(it)) }
                )
                isLoading -> CircularProgressIndicator()
                else -> Text(stringResource(R.string.text_no_media_trash), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

