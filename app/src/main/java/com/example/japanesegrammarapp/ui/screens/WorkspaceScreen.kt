package com.example.japanesegrammarapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.AnalysisStatus
import com.example.japanesegrammarapp.ui.WorkspaceViewModel
import com.example.japanesegrammarapp.ui.UiEvent
import com.example.japanesegrammarapp.ui.screens.components.FloatingActionBall
import com.example.japanesegrammarapp.ui.screens.components.WorkspaceInputForm
import com.example.japanesegrammarapp.ui.screens.components.WorkspaceResultContent
import com.example.japanesegrammarapp.ui.screens.components.ZenLoadingView
import com.example.japanesegrammarapp.ui.theme.ZenColors.SumiInk
import com.example.japanesegrammarapp.ui.theme.ZenColors.WashiBg
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun WorkspaceScreen(
    navController: NavController, 
    viewModel: WorkspaceViewModel,
    fromBookmarks: Boolean = false,
    onOpenDrawer: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {
        if (navController.currentDestination?.route == "workspace") {
            navController.navigate("settings")
        }
    }
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val WashiBg = MaterialTheme.colorScheme.background
    val PrimaryColor = MaterialTheme.colorScheme.primary
    val OnPrimaryColor = MaterialTheme.colorScheme.onPrimary

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val latestUiState by rememberUpdatedState(uiState)
    val snackbarHostState = remember { SnackbarHostState() }
    
    val isPlayingTts by viewModel.isPlayingTts.collectAsState(initial = false)

    // Hoisted States for input form
    var textInputState by androidx.compose.runtime.saveable.rememberSaveable(uiState.currentOriginalText) {
        mutableStateOf(uiState.currentOriginalText)
    }
    var selectedImageUriState by remember { mutableStateOf<Uri?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }


    // Clear image uri when returning to homepage (no active record)
    LaunchedEffect(uiState.selectedRecord) {
        if (uiState.selectedRecord == null) {
            selectedImageUriState = null
        }
    }

    // Intercept back button to return to input page (unless we came from Bookmarks)
    androidx.activity.compose.BackHandler(enabled = uiState.selectedRecord != null && !uiState.isExternalQuery && !fromBookmarks) {
        viewModel.clearSelectedRecord()
    }

    val navigateToSettings = onNavigateToSettings

    // Refresh settings when returning to workspace
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSettings()
                viewModel.refreshCurrentRecord()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.stopTts()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // UI Event Collection & Localization Handling
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.ShowLocalizedError -> {
                    val formattedMessage = try {
                        if (event.args.isNotEmpty()) {
                            val resolvedArgs = event.args.map { arg ->
                                if (arg is Int) {
                                    try {
                                        context.getString(arg)
                                    } catch (e: android.content.res.Resources.NotFoundException) {
                                        arg
                                    }
                                } else arg
                            }
                            context.getString(event.resId, *resolvedArgs.toTypedArray())
                        } else {
                            context.getString(event.resId)
                        }
                    } catch (e: android.content.res.Resources.NotFoundException) {
                        "Notification triggered"
                    }
                    snackbarHostState.showSnackbar(formattedMessage)
                }
                is UiEvent.TaskCompleted -> {
                    val messageStr = if (event.isShortened) {
                        context.getString(R.string.analysis_completed_short, event.analyzedText.take(10))
                    } else {
                        context.getString(R.string.analysis_completed_full, event.analyzedText)
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = messageStr,
                        actionLabel = context.getString(R.string.show_action),
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.selectRecordById(event.recordId)
                    }
                }
                is UiEvent.ShareFileEvent -> {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        event.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooserIntent = Intent.createChooser(sendIntent, context.getString(event.chooserTitleResId))
                    val resInfoList = context.packageManager.queryIntentActivities(chooserIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                    for (resolveInfo in resInfoList) {
                        val packageName = resolveInfo.activityInfo.packageName
                        context.grantUriPermission(packageName, event.uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(chooserIntent)
                }
                is UiEvent.ShareTextEvent -> {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, event.text)
                        event.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                    }
                    val chooserIntent = Intent.createChooser(sendIntent, context.getString(event.chooserTitleResId))
                    context.startActivity(chooserIntent)
                }
                is UiEvent.NavigateToCameraWithImage -> {
                    navController.navigate("camera?imageUri=${Uri.encode(event.uri.toString())}")
                }
                else -> {}
            }
        }
    }

    // Camera Result Observer & OCR Executor
    val cameraNavBackStackEntry = navController.currentBackStackEntry
    LaunchedEffect(cameraNavBackStackEntry) {
        cameraNavBackStackEntry?.savedStateHandle?.getStateFlow<String?>("captured_image_uri", null)
            ?.collect { uriString ->
                if (!uriString.isNullOrBlank()) {
                    val uri = Uri.parse(uriString)
                    val currentUiState = latestUiState
                    if (currentUiState.useOcr) {
                        if (currentUiState.autoNavigateResult) {
                            selectedImageUriState = uri
                            viewModel.startNewAnalysisWithText("")
                        }
                        viewModel.startAnalysis("", uri)
                    } else {
                        if (currentUiState.autoNavigateResult) {
                            selectedImageUriState = uri
                            viewModel.startNewAnalysisWithText("")
                        }
                        viewModel.startAnalysis("", uri)
                    }
                    cameraNavBackStackEntry?.savedStateHandle?.set("captured_image_uri", null)
                }
            }
    }

    // Observe navigation-to-record requests from BookmarksScreen
    LaunchedEffect(cameraNavBackStackEntry) {
        cameraNavBackStackEntry?.savedStateHandle?.getStateFlow<Int?>("navigate_to_record_id", null)
            ?.collect { recordId ->
                if (recordId != null) {
                    viewModel.selectRecordById(recordId)
                    cameraNavBackStackEntry?.savedStateHandle?.set("navigate_to_record_id", null)
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("workspace-screen")
    ) {
        Scaffold(
            containerColor = if (uiState.wallpaperUri.isNotBlank()) Color.Transparent else WashiBg,
        snackbarHost = { 
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SumiInk.copy(alpha = 0.85f),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 60.dp, start = 16.dp, end = 16.dp)
                )
            } 
        },
        topBar = {
            AnimatedVisibility(
                visible = uiState.selectedRecord != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.analysis_results),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        if (fromBookmarks) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        } else {
                            IconButton(onClick = onOpenDrawer) {
                                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.history_menu_desc), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    },
                    actions = {
                        if (uiState.selectedRecord != null) {
                                IconButton(onClick = {
                                    uiState.selectedRecord?.let { viewModel.retryAnalysis(it.id) }
                                }) {
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.retry), tint = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = { viewModel.clearSelectedRecord() }) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_analysis), tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            IconButton(onClick = {
                                if (fromBookmarks) {
                                    navController.popBackStack("bookmarks", false)
                                } else {
                                    navController.navigate("bookmarks")
                                }
                            }) {
                                Icon(Icons.Default.Star, contentDescription = stringResource(R.string.view_bookmarks_desc), tint = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = navigateToSettings) {
                                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if (uiState.wallpaperUri.isNotBlank()) Color.Transparent else WashiBg,
                            titleContentColor = PrimaryColor
                        )
                    )
            }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val hasResult = uiState.selectedRecord != null

                AnimatedContent(
                    targetState = hasResult,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "WorkspaceStateTransition",
                    modifier = Modifier.fillMaxSize()
                ) { targetHasResult ->
                    if (!targetHasResult) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                        ) {
                            // Custom Top Bar for Elegant UI
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 48.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onOpenDrawer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.history_menu_desc), tint = SumiInk)
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            if (fromBookmarks) {
                                                navController.popBackStack("bookmarks", false)
                                            } else {
                                                navController.navigate("bookmarks")
                                            }
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .testTag("workspace-bookmarks-button")
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = stringResource(R.string.view_bookmarks_desc), tint = SumiInk)
                                    }
                                    IconButton(
                                        onClick = navigateToSettings,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .testTag("workspace-settings-button")
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings), tint = SumiInk)
                                    }
                                }
                            }

                            // Hero Typography
                            Text(
                                text = stringResource(R.string.hero_title),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = SumiInk,
                                letterSpacing = (-1).sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = stringResource(R.string.hero_subtitle),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Light,
                                color = SumiInk.copy(alpha = 0.6f),
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 72.dp)
                            )

                            // Main Floating Panel
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 40.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 3.dp,
                                tonalElevation = 0.dp
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    WorkspaceInputForm(
                                        uiState = uiState,
                                        textInput = textInputState,
                                        onTextInputChanged = { text ->
                                            textInputState = text
                                            viewModel.setCurrentOriginalText(text)
                                        },
                                        selectedImageUri = selectedImageUriState,
                                        onSelectedImageUriChanged = { uri -> selectedImageUriState = uri },
                                        onModelSelected = { model -> viewModel.setActiveModel(model) },
                                        onStartAnalysis = { text, uri ->
                                            viewModel.startAnalysis(text, uri, forceNavigate = true)
                                        },
                                        onCancelAnalysis = {
                                            uiState.selectedRecord?.id?.let { viewModel.cancelAnalysis(it) }
                                        },
                                        onNavigateToCamera = { navController.navigate("camera") },
                                        onPickImage = { sourceUri ->
                                            viewModel.prepareImageForCamera(sourceUri)
                                        },
                                        onNavigateToSettings = navigateToSettings
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                             Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(24.dp),
                                shadowElevation = 3.dp,
                                tonalElevation = 0.dp
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val currentText = uiState.currentOriginalText
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.target_sentence),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = SumiInk.copy(alpha = 0.6f)
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val isPending = uiState.selectedRecord?.status == AnalysisStatus.PENDING

                                            if (!isPending && uiState.selectedRecord != null) {
                                                val isSentenceBookmarked = uiState.isSentenceBookmarked
                                                val goldColor = Color(0xFFD4A017)
                                                val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

                                                // Star pulse animation when bookmark state changes
                                                var wasSentenceBookmarked by remember { mutableStateOf(isSentenceBookmarked) }
                                                var starScale by remember { mutableFloatStateOf(1f) }
                                                LaunchedEffect(isSentenceBookmarked) {
                                                    if (isSentenceBookmarked && !wasSentenceBookmarked) {
                                                        val pulse = androidx.compose.animation.core.Animatable(1f)
                                                        pulse.animateTo(1.3f, animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                                                        pulse.animateTo(1.0f, animationSpec = androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                                                        starScale = 1f
                                                    }
                                                    wasSentenceBookmarked = isSentenceBookmarked
                                                }

                                                // Brief glow animation on bookmark
                                                var showGlowAnimation by remember { mutableStateOf(false) }
                                                val glow = remember { androidx.compose.animation.core.Animatable(0f) }
                                                LaunchedEffect(isSentenceBookmarked) {
                                                    if (isSentenceBookmarked && showGlowAnimation) {
                                                        glow.snapTo(0.4f)
                                                        glow.animateTo(0f, animationSpec = androidx.compose.animation.core.tween(600, easing = androidx.compose.animation.core.EaseInOutCubic))
                                                        showGlowAnimation = false
                                                    }
                                                }

                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                            if (!isSentenceBookmarked) {
                                                                showGlowAnimation = true
                                                            }
                                                            viewModel.toggleSentenceBookmark()
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isSentenceBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                                                            contentDescription = stringResource(if (isSentenceBookmarked) R.string.bookmark_sentence_removed else R.string.bookmark_sentence_added),
                                                            tint = if (isSentenceBookmarked) goldColor else SumiInk.copy(alpha = 0.45f),
                                                            modifier = Modifier.size(20.dp).graphicsLayer {
                                                                scaleX = starScale
                                                                scaleY = starScale
                                                            }
                                                        )
                                                    }
                                                    if (isSentenceBookmarked && glow.value > 0.01f) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(RoundedCornerShape(16.dp))
                                                                .background(goldColor.copy(alpha = glow.value))
                                                        )
                                                    }
                                                }
                                            }

                                            TextButton(
                                                onClick = {
                                                    if (isPending) {
                                                        uiState.selectedRecord?.let { record ->
                                                            viewModel.cancelAnalysis(record.id)
                                                            viewModel.deleteRecord(record)
                                                        }
                                                    } else {
                                                        showDeleteConfirmDialog = true
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                colors = ButtonDefaults.textButtonColors(contentColor = SumiInk)
                                            ) {
                                                Icon(
                                                    imageVector = if (isPending) Icons.Default.Close else Icons.Default.Delete,
                                                    contentDescription = stringResource(if (isPending) R.string.cancel else R.string.delete),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = stringResource(if (isPending) R.string.cancel else R.string.delete),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            TextButton(
                                                onClick = {
                                                    viewModel.startNewAnalysisWithText(currentText)
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                colors = ButtonDefaults.textButtonColors(contentColor = SumiInk)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = stringResource(R.string.edit_and_reanalyze_desc),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = stringResource(R.string.edit_and_reanalyze),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    androidx.compose.foundation.text.selection.SelectionContainer {
                                        Text(
                                            text = currentText.ifBlank { stringResource(R.string.image_analysis) },
                                            fontSize = 14.sp,
                                            color = SumiInk,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }

                            val record = uiState.selectedRecord
                            if (record != null) {
                                val resultState = when {
                                    record.status == AnalysisStatus.FAILED -> "FAILED"
                                    else -> "CONTENT"
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    AnimatedContent(
                                        targetState = resultState,
                                        transitionSpec = {
                                            fadeIn(animationSpec = tween(400, easing = EaseInOutCubic))
                                                .togetherWith(fadeOut(animationSpec = tween(400, easing = EaseInOutCubic)))
                                        },
                                        label = "ResultStateTransition",
                                        modifier = Modifier.fillMaxSize()
                                    ) { state ->
                                        when (state) {
                                            "CONTENT" -> {
                                                WorkspaceResultContent(
                                                    uiState = uiState,
                                                    isPlayingTts = isPlayingTts,
                                                    onPlayTts = { viewModel.playTtsForCurrentRecord() },
                                                    onStopTts = { viewModel.stopTts() },
                                                    onToggleBookmark = { segment ->
                                                        viewModel.toggleBookmark(segment)
                                                    },
                                                    onToggleGrammarBookmark = { pattern, explanation, sourceText ->
                                                        viewModel.toggleGrammarPointBookmark(pattern, explanation, sourceText)
                                                    },
                                                    onLoadNewer = { viewModel.loadNewerRecord() },
                                                    onLoadOlder = { viewModel.loadOlderRecord() },
                                                    uiPreferencesRepository = viewModel.uiPreferencesRepository
                                                )
                                            }
                                            "FAILED" -> {
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp).padding(top = 16.dp),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    border = BorderStroke(1.dp, Color(0xFFF3D8D8)),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(16.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Warning,
                                                            contentDescription = "Error",
                                                            tint = Color(0xFFD32F2F),
                                                            modifier = Modifier.size(36.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        Text(
                                                            text = stringResource(R.string.error_occurred),
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFD32F2F),
                                                            fontSize = 14.sp
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                         androidx.compose.foundation.text.selection.SelectionContainer {
                                                             Text(
                                                                 text = record.errorMessage ?: stringResource(R.string.error_occurred),
                                                                 color = SumiInk.copy(alpha = 0.7f),
                                                                 fontSize = 13.sp,
                                                                 textAlign = TextAlign.Center
                                                             )
                                                         }
                                                         Spacer(modifier = Modifier.height(12.dp))
                                                         val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                                         val errorStr = record.errorMessage ?: stringResource(R.string.error_occurred)
                                                         TextButton(
                                                             onClick = {
                                                                 clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(errorStr))
                                                                 android.widget.Toast.makeText(context, context.getString(R.string.copy_success_toast), android.widget.Toast.LENGTH_SHORT).show()
                                                             },
                                                             contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                             colors = ButtonDefaults.textButtonColors(contentColor = SumiInk.copy(alpha = 0.8f))
                                                         ) {
                                                             Icon(
                                                                 imageVector = Icons.Default.ContentCopy,
                                                                 contentDescription = "Copy Error Details",
                                                                 modifier = Modifier.size(14.dp)
                                                             )
                                                             Spacer(modifier = Modifier.width(4.dp))
                                                             Text(stringResource(R.string.copy_full_error_log), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                         }
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                            OutlinedButton(
                                                                onClick = { viewModel.deleteRecord(record); viewModel.clearSelectedRecord() },
                                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                                                                border = BorderStroke(1.dp, Color(0xFFF3D8D8))
                                                            ) {
                                                                Text(stringResource(R.string.delete), fontSize = 13.sp)
                                                            }
                                                            Button(
                                                                onClick = { viewModel.retryAnalysis(record.id) },
                                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = OnPrimaryColor)
                                                            ) {
                                                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.retry), modifier = Modifier.size(16.dp))
                                                                 Spacer(modifier = Modifier.width(4.dp))
                                                                Text(stringResource(R.string.retry), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    if (showDeleteConfirmDialog) {
        val record = uiState.selectedRecord
        if (record != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text(stringResource(R.string.delete_analysis_confirm_title), fontWeight = FontWeight.Bold, color = SumiInk) },
                text = { Text(stringResource(R.string.delete_analysis_confirm_message), color = SumiInk) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteRecord(record)
                            viewModel.clearSelectedRecord()
                            showDeleteConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text(stringResource(R.string.cancel), color = SumiInk)
                    }
                },
                containerColor = Color.White
            )
        }
    }

    // Input Dialog for Floating Action Ball
    val showInputDialogFromVm by viewModel.showInputDialog.collectAsState()
    var showInputDialogLocal by remember { mutableStateOf(false) }
    val showInputDialog = showInputDialogFromVm || showInputDialogLocal
    var inputText by remember { mutableStateOf("") }
    
    if (showInputDialog) {
        AlertDialog(
            onDismissRequest = { 
                showInputDialogLocal = false
                viewModel.hideGlobalInputDialog() 
            },
            title = { Text(stringResource(R.string.input_dialog_title), fontWeight = FontWeight.Bold, color = SumiInk) },
            text = {
                val focusManager = LocalFocusManager.current
                val keyboardController = LocalSoftwareKeyboardController.current
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    placeholder = { Text(stringResource(R.string.input_dialog_placeholder)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = SumiInk.copy(alpha = 0.2f)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            if (uiState.autoNavigateResult) {
                                viewModel.startNewAnalysisWithText(inputText)
                                viewModel.startAnalysis(inputText, null, forceNavigate = true)
                            } else {
                                viewModel.startAnalysis(inputText, null)
                            }
                        }
                        showInputDialogLocal = false
                        viewModel.hideGlobalInputDialog()
                        inputText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = OnPrimaryColor)
                ) {
                    Text(stringResource(R.string.start_analysis_btn), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showInputDialogLocal = false
                    viewModel.hideGlobalInputDialog()
                }) {
                    Text(stringResource(R.string.cancel), color = SumiInk)
                }
            },
            containerColor = Color.White
        )
    }

    // Floating Action Ball
    FloatingActionBall(
        onTextClick = { showInputDialogLocal = true },
        onCameraClick = { navController.navigate("camera") },
        uiPreferencesRepository = viewModel.uiPreferencesRepository
    )
    }
}
