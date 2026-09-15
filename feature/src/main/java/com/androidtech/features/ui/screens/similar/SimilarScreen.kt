package com.androidtech.features.ui.screens.similar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.androidtech.base.permission.Permission
import com.androidtech.base.permission.rememberSinglePermission
import com.androidtech.base.theme.Body16Medium
import com.androidtech.base.theme.Title20Bold
import com.androidtech.base.utils.formatSize
import com.androidtech.features.ui.components.DeleteBottomBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun NavGraphBuilder.similarScreen() {
    composable(
        route = AppDestination.SimilarScreen.route,
    ) {
        SimilarScreen()
    }
}

@Composable
fun SimilarScreen(
    viewModel: SimilarViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = LocalNavController.current
    val context = LocalContext.current

    val writePermissionState = rememberSinglePermission(
        permission = Permission.WriteExternalStorage,
        onGranted = {
            val selectedPhotos = uiState.value.similarGroups.flatMap { it.items }.filter { it.isSelected }.map { it.id }
            viewModel.handleEvent(SimilarEvent.DeleteClick(selectedPhotos))
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
            val selectedPhotos = uiState.value.similarGroups.flatMap { it.items }.filter { it.isSelected }.map { it.id }
            viewModel.handleEvent(SimilarEvent.ConfirmCacheCleanup(selectedPhotos))
            showSnackbar("Xóa thành công các tệp đã chọn")
        }
    }

    LaunchedEffect(viewModel.uiEffect, lifecycleOwner) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle).collect { effect ->
            when (effect) {
                is SimilarEffect.NavigateBack -> {
                    navController.popBackStack()
                }
                is SimilarEffect.ShowSnackbar -> {
                    showSnackbar(effect.message)
                }
                is SimilarEffect.RequestWritePermission -> {
                    if (writePermissionState.isGranted) {
                        viewModel.handleEvent(SimilarEvent.DeleteClick(effect.selectedPhotoIds))
                    } else {
                        writePermissionState.request()
                    }
                }
                is SimilarEffect.LaunchIntentSender -> {
                    intentSenderLauncher.launch(
                        IntentSenderRequest.Builder(effect.intentSender).build()
                    )
                }
            }
        }
    }

    SimilarContent(
        uiState = uiState.value,
        onEvent = { viewModel.handleEvent(it) },
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimilarContent(
    uiState: SimilarState,
    onEvent: (SimilarEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val similarGroups = uiState.similarGroups
    val totalSavings: Long = similarGroups.sumOf { it.potentialSavingsBytes }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.text_similar_photos),
                            style = Title20Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = stringResource(R.string.text_similar_photos_description, uiState.similarGroups.size, totalSavings.formatSize() ?: ""),
                            style = Body16Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(SimilarEvent.NavigateBackClick) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(SimilarEvent.MoreClicked) }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert),
                            contentDescription = "more",
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
            val selectedPhotos = similarGroups.flatMap { it.items }.filter { it.isSelected }
            val selectedCount = selectedPhotos.size
            val selectedSize = selectedPhotos.sumOf { it.size }
            
            if (selectedCount > 0) {
                DeleteBottomBar(
                    selectedCount = selectedCount,
                    sizeText = selectedSize.formatSize(),
                    onDeleteClick = {
                        onEvent(SimilarEvent.DeleteClick(selectedPhotos.map { it.id }))
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(similarGroups, key = { it.groupId }) { group ->
                SimilarGroupSection(
                    group = group,
                    onPhotoToggle = { photoId -> onEvent(SimilarEvent.TogglePhotoSelection(photoId)) }
                )
            }
        }
    }
}
