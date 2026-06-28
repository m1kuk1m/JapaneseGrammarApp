package com.example.japanesegrammarapp.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.ui.screens.*
import com.example.japanesegrammarapp.ui.screens.components.HistorySidebar
import com.example.japanesegrammarapp.ui.screens.components.ExportSelectionDialog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
fun AppNavigation(externalTextFlow: Flow<String> = emptyFlow(), intentFlow: Flow<Intent> = emptyFlow()) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home_pager",
        modifier = Modifier.fillMaxSize(),
        // Shortened from 400ms → 300ms: reduces the window during which competing
        // internal Compose animations (expandVertically, animateContentSize) can
        // cause frame drops. EaseInOutQuart feels equally smooth but completes faster.
        enterTransition = {
            fadeIn(animationSpec = tween(300, easing = EaseInOutQuart))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300, easing = EaseInOutQuart))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300, easing = EaseInOutQuart))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300, easing = EaseInOutQuart))
        }
    ) {
        composable(
            route = "home_pager?selectRecordId={selectRecordId}&selectBookmarkId={selectBookmarkId}&fromBookmarks={fromBookmarks}",
            arguments = listOf(
                androidx.navigation.navArgument("selectRecordId") {
                    type = androidx.navigation.NavType.IntType
                    defaultValue = -1
                },
                androidx.navigation.navArgument("selectBookmarkId") {
                    type = androidx.navigation.NavType.IntType
                    defaultValue = -1
                },
                androidx.navigation.navArgument("fromBookmarks") {
                    type = androidx.navigation.NavType.BoolType
                    defaultValue = false
                }
            ),
            enterTransition = {
                if (targetState.arguments?.getBoolean("fromBookmarks") == true) {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300, easing = EaseInOutQuart)
                    )
                } else {
                    fadeIn(animationSpec = tween(300, easing = EaseInOutQuart))
                }
            },
            exitTransition = {
                if (initialState.arguments?.getBoolean("fromBookmarks") == true) {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300, easing = EaseInOutQuart)
                    )
                } else {
                    fadeOut(animationSpec = tween(300, easing = EaseInOutQuart))
                }
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300, easing = EaseInOutQuart))
            },
            popExitTransition = {
                if (initialState.arguments?.getBoolean("fromBookmarks") == true) {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300, easing = EaseInOutQuart)
                    )
                } else {
                    fadeOut(animationSpec = tween(300, easing = EaseInOutQuart))
                }
            }
        ) { backStackEntry ->
            val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
            val coroutineScope = rememberCoroutineScope()

            val selectRecordId = backStackEntry.arguments?.getInt("selectRecordId") ?: -1
            val selectBookmarkId = backStackEntry.arguments?.getInt("selectBookmarkId") ?: -1
            val fromBookmarks = backStackEntry.arguments?.getBoolean("fromBookmarks") ?: false

            val workspaceViewModel: WorkspaceViewModel = hiltViewModel()
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            
            val drawerWidth = 310.dp
            val drawerAnimation = remember { Animatable(0f) }
            var drawerOffsetPx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
            var drawerAnimationRunning by remember { mutableStateOf(false) }
            var drawerAnimationJob by remember { mutableStateOf<Job?>(null) }
            
            val history = workspaceViewModel.history.collectAsLazyPagingItems()
            val historySearchQuery by workspaceViewModel.historySearchQuery.collectAsState()
            val allHistoryForExport by workspaceViewModel.allHistoryForExport.collectAsState()
            val uiState by workspaceViewModel.uiState.collectAsState()
            val bookmarkedSentenceIds by workspaceViewModel.bookmarkedSentenceRecordIds.collectAsState()
            
            var recordToDelete by remember { mutableStateOf<AnalysisDomainRecord?>(null) }
            var showExportDialog by remember { mutableStateOf(false) }

            val WashiBg = androidx.compose.material3.MaterialTheme.colorScheme.background
            val SumiInk = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
            val textToolbar = LocalTextToolbar.current
            
            LaunchedEffect(Unit) {
                externalTextFlow.collect { text ->
                    workspaceViewModel.startNewAnalysisWithText(text, isExternal = true)
                    workspaceViewModel.startAnalysis(text, null, forceNavigate = true)
                    pagerState.animateScrollToPage(0)
                }
            }

            // Handle navigation from bookmarks screen: select a record by ID or bookmark ID
            // We navigate (not pop) so the bookmark screen stays in the back stack
            LaunchedEffect(selectRecordId, selectBookmarkId) {
                if (selectRecordId > 0) {
                    pagerState.animateScrollToPage(0)
                    workspaceViewModel.selectRecordById(selectRecordId)
                } else if (selectBookmarkId > 0) {
                    pagerState.animateScrollToPage(0)
                    workspaceViewModel.selectRecordByBookmarkId(selectBookmarkId)
                }
            }

            LaunchedEffect(Unit) {
                intentFlow.collect { intent ->
                    val dest = intent.getStringExtra("destination")
                    val showInput = intent.getBooleanExtra("show_input", false)
                    val ocrUriStr = intent.getStringExtra("ocr_uri")
                    
                    if (dest == "camera") {
                        pagerState.animateScrollToPage(0)
                        navController.navigate("camera")
                    } else if (showInput) {
                        pagerState.animateScrollToPage(0)
                        workspaceViewModel.showGlobalInputDialog()
                    } else if (!ocrUriStr.isNullOrBlank()) {
                        pagerState.animateScrollToPage(0)
                        navController.currentBackStackEntry?.savedStateHandle?.set("captured_image_uri", ocrUriStr)
                    }
                }
            }

            LaunchedEffect(pagerState.currentPage) {
                if (pagerState.currentPage != 0) {
                    workspaceViewModel.stopTts()
                }
            }
            
            val density = androidx.compose.ui.platform.LocalDensity.current
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            
            val ballSizePx = with(density) { 56.dp.toPx() }
            val drawerWidthPx = with(density) { drawerWidth.toPx() }
            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
            val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
            
            val defaultFabX = screenWidthPx - ballSizePx - with(density) { 16.dp.toPx() }
            val defaultFabY = screenHeightPx - ballSizePx - with(density) { 120.dp.toPx() }

            LaunchedEffect(drawerWidthPx) {
                if (drawerOffsetPx > drawerWidthPx) {
                    drawerOffsetPx = drawerWidthPx
                }
            }

            fun animateDrawerTo(targetOffset: Float, durationMillis: Int) {
                drawerAnimationJob?.cancel()
                drawerAnimationJob = coroutineScope.launch {
                    drawerAnimationRunning = true
                    try {
                        drawerAnimation.snapTo(drawerOffsetPx)
                        drawerAnimation.animateTo(
                            targetValue = targetOffset.coerceIn(0f, drawerWidthPx),
                            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
                        ) {
                            drawerOffsetPx = value
                        }
                        drawerOffsetPx = targetOffset.coerceIn(0f, drawerWidthPx)
                    } finally {
                        drawerAnimationRunning = false
                    }
                }
            }

            fun settleDrawer(velocityX: Float) {
                val shouldOpen = when {
                    velocityX > 700f -> true
                    velocityX < -700f -> false
                    else -> drawerOffsetPx >= drawerWidthPx * 0.35f
                }
                animateDrawerTo(if (shouldOpen) drawerWidthPx else 0f, 220)
            }

            fun openDrawer() {
                animateDrawerTo(drawerWidthPx, 220)
            }

            fun closeDrawer() {
                animateDrawerTo(0f, 200)
            }

            val drawerProgress = if (drawerWidthPx > 0f) {
                (drawerOffsetPx / drawerWidthPx).coerceIn(0f, 1f)
            } else {
                0f
            }
            val drawerVisible = drawerProgress > 0.001f || drawerAnimationRunning

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pagerState.currentPage, fromBookmarks, drawerWidthPx) {
                            if (pagerState.currentPage == 0) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    
                                    val fabX = workspaceViewModel.uiPreferencesRepository.getFloatingActionBallX(defaultFabX)
                                    val fabY = workspaceViewModel.uiPreferencesRepository.getFloatingActionBallY(defaultFabY)
                                    val isTouchOnFab = down.position.x >= fabX &&
                                            down.position.x <= fabX + ballSizePx &&
                                            down.position.y >= fabY &&
                                            down.position.y <= fabY + ballSizePx

                                    if (isTouchOnFab) {
                                        do {
                                            val event = awaitPointerEvent()
                                        } while (event.changes.any { it.pressed })
                                        return@awaitEachGesture
                                    }

                                    if (textToolbar.status == TextToolbarStatus.Shown) {
                                        // If selection is already active, skip this gesture to allow dragging selection handles
                                        do {
                                            val event = awaitPointerEvent()
                                        } while (event.changes.any { it.pressed })
                                        return@awaitEachGesture
                                    }

                                    var totalDx = 0f
                                    var totalDy = 0f
                                    var isDecided = false
                                    var isRightSwipe = false
                                    var startTime = down.uptimeMillis
                                    var lastTime = down.uptimeMillis

                                    do {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull()
                                        
                                        if (change != null) {
                                            val timeElapsed = change.uptimeMillis - down.uptimeMillis
                                            val positionChange = change.positionChange()
                                            lastTime = change.uptimeMillis
                                            if (!isDecided) {
                                                if (timeElapsed > 300L || textToolbar.status == TextToolbarStatus.Shown) {
                                                    // Cancel swipe detection if held for >300ms (long-press text selection)
                                                    // or if selection becomes active
                                                    isDecided = true
                                                    isRightSwipe = false
                                                } else {
                                                    totalDx += positionChange.x
                                                    totalDy += positionChange.y
                                                    
                                                    if (kotlin.math.abs(totalDx) > 20f || kotlin.math.abs(totalDy) > 20f) {
                                                        isDecided = true
                                                        if (totalDx > 0 && kotlin.math.abs(totalDx) > kotlin.math.abs(totalDy)) {
                                                            isRightSwipe = true
                                                            startTime = change.uptimeMillis
                                                            drawerAnimationJob?.cancel()
                                                            drawerAnimationRunning = false
                                                        }
                                                    }
                                                }
                                            } else if (isRightSwipe) {
                                                totalDx += positionChange.x
                                                totalDy += positionChange.y
                                            }

                                            if (isDecided && isRightSwipe) {
                                                if (fromBookmarks && uiState.selectedRecord != null) {
                                                    change.consume()
                                                } else {
                                                    drawerOffsetPx = totalDx.coerceIn(0f, drawerWidthPx)
                                                    change.consume()
                                                }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })

                                    if (isDecided && isRightSwipe) {
                                        if (fromBookmarks && uiState.selectedRecord != null) {
                                            navController.popBackStack()
                                        } else {
                                            val elapsedMs = (lastTime - startTime).coerceAtLeast(1L)
                                            val velocityX = totalDx / elapsedMs * 1000f
                                            settleDrawer(velocityX)
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !drawerVisible
                    ) { page ->
                        when (page) {
                            0 -> {
                                WorkspaceScreen(
                                    navController = navController,
                                    viewModel = workspaceViewModel,
                                    fromBookmarks = fromBookmarks,
                                    onOpenDrawer = {
                                        openDrawer()
                                    },
                                    onNavigateToSettings = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(1)
                                        }
                                    }
                                )
                            }
                            1 -> {
                                SettingsScreen(
                                    navController = navController,
                                    viewModel = settingsViewModel,
                                    isVisible = pagerState.currentPage == 1,
                                    onBack = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(0)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    // Replaced Edge Swipe Interceptor with global interceptor
                }

                if (drawerVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.32f * drawerProgress))
                            .pointerInput(drawerWidthPx) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    drawerAnimationJob?.cancel()
                                    drawerAnimationRunning = false

                                    var totalDx = 0f
                                    var totalDy = 0f
                                    var isDecided = false
                                    var isHorizontalDrag = false
                                    val startOffset = drawerOffsetPx
                                    val startTime = down.uptimeMillis
                                    var lastTime = down.uptimeMillis

                                    do {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull()

                                        if (change != null) {
                                            val positionChange = change.positionChange()
                                            totalDx += positionChange.x
                                            totalDy += positionChange.y
                                            lastTime = change.uptimeMillis

                                            if (!isDecided && (kotlin.math.abs(totalDx) > 20f || kotlin.math.abs(totalDy) > 20f)) {
                                                isDecided = true
                                                isHorizontalDrag = kotlin.math.abs(totalDx) > kotlin.math.abs(totalDy)
                                            }

                                            if (isDecided && isHorizontalDrag) {
                                                drawerOffsetPx = (startOffset + totalDx).coerceIn(0f, drawerWidthPx)
                                                change.consume()
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })

                                    if (isDecided && isHorizontalDrag) {
                                        val elapsedMs = (lastTime - startTime).coerceAtLeast(1L)
                                        val velocityX = totalDx / elapsedMs * 1000f
                                        settleDrawer(velocityX)
                                    }
                                }
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { closeDrawer() }
                            )
                    )
                }

                if (drawerVisible) {
                    Box(
                        modifier = Modifier
                            .width(drawerWidth)
                            .fillMaxHeight()
                            .graphicsLayer {
                                translationX = drawerOffsetPx - drawerWidthPx
                            }
                            .background(if (uiState.wallpaperUri.isNotBlank()) Color.Transparent else WashiBg)
                            .pointerInput(drawerWidthPx) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    drawerAnimationJob?.cancel()
                                    drawerAnimationRunning = false

                                    var totalDx = 0f
                                    var totalDy = 0f
                                    var isDecided = false
                                    var isHorizontalDrag = false
                                    val startOffset = drawerOffsetPx
                                    val startTime = down.uptimeMillis
                                    var lastTime = down.uptimeMillis

                                    do {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull()

                                        if (change != null) {
                                            val positionChange = change.positionChange()
                                            totalDx += positionChange.x
                                            totalDy += positionChange.y
                                            lastTime = change.uptimeMillis

                                            if (!isDecided && (kotlin.math.abs(totalDx) > 20f || kotlin.math.abs(totalDy) > 20f)) {
                                                isDecided = true
                                                isHorizontalDrag = kotlin.math.abs(totalDx) > kotlin.math.abs(totalDy)
                                            }

                                            if (isDecided && isHorizontalDrag) {
                                                drawerOffsetPx = (startOffset + totalDx).coerceIn(0f, drawerWidthPx)
                                                change.consume()
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })

                                    if (isDecided && isHorizontalDrag) {
                                        val elapsedMs = (lastTime - startTime).coerceAtLeast(1L)
                                        val velocityX = totalDx / elapsedMs * 1000f
                                        settleDrawer(velocityX)
                                    }
                                }
                            }
                    ) {
                        HistorySidebar(
                            historyList = history,
                            searchQuery = historySearchQuery,
                            selectedRecord = uiState.selectedRecord,
                            bookmarkedSentenceIds = bookmarkedSentenceIds,
                            onSearchQueryChange = workspaceViewModel::setHistorySearchQuery,
                            onSelectRecord = { record -> workspaceViewModel.selectRecord(record) },
                            onClearSelection = { workspaceViewModel.clearSelectedRecord() },
                            onDeleteRecord = { record -> recordToDelete = record },
                            onExportAll = {
                                closeDrawer()
                                workspaceViewModel.loadAllHistoryForExport()
                                showExportDialog = true
                            },
                            onExportRecord = { record -> workspaceViewModel.exportRecord(record) },
                            onCloseDrawer = { closeDrawer() },
                            onImportHistory = { uri -> workspaceViewModel.importHistoryFromUri(uri) },
                            onToggleBookmarkSentence = { record -> workspaceViewModel.toggleSentenceBookmark(record) },
                            onNavigateToStatistics = {
                                closeDrawer()
                                navController.navigate("statistics")
                            }
                        )
                    }
                }
            }

            BackHandler(enabled = drawerVisible) {
                closeDrawer()
            }
            
            // Deletion Confirmation Dialog
            if (recordToDelete != null) {
                val record = recordToDelete!!
                AlertDialog(
                    onDismissRequest = { recordToDelete = null },
                    title = { Text(stringResource(R.string.delete_history_title), fontWeight = FontWeight.Bold, color = SumiInk) },
                    text = { Text(stringResource(R.string.delete_history_confirm, record.originalText.take(15)), color = SumiInk) },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (uiState.selectedRecord?.id == record.id) {
                                    workspaceViewModel.clearSelectedRecord()
                                }
                                workspaceViewModel.deleteRecord(record)
                                recordToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)
                        ) {
                            Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { recordToDelete = null }) {
                            Text(stringResource(R.string.cancel), color = SumiInk)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }

            // Export Selection Dialog
            if (showExportDialog) {
                ExportSelectionDialog(
                    historyList = allHistoryForExport,
                    onDismiss = { showExportDialog = false },
                    onExportSelected = { selectedRecordIds ->
                        workspaceViewModel.exportHistoryByIds(selectedRecordIds)
                        showExportDialog = false
                    }
                )
            }
        }
        composable(
            route = "camera?imageUri={imageUri}",
            arguments = listOf(
                androidx.navigation.navArgument("imageUri") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val galleryImageUriString = backStackEntry.arguments?.getString("imageUri")
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val cameraViewModel: CameraViewModel = hiltViewModel()
            val settingsUiState by settingsViewModel.uiState.collectAsState()
            CameraScreen(
                navController = navController,
                galleryImageUriString = galleryImageUriString,
                ocrBoxDetectionSettings = settingsUiState.ocrBoxDetectionSettings,
                autoDeskewAfterCapture = settingsUiState.autoDeskewAfterCapture,
                uiPreferencesRepository = cameraViewModel.uiPreferencesRepository
            )
        }

        composable(
            route = "bookmarks",
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            }
        ) {
            val bookmarkViewModel: com.example.japanesegrammarapp.ui.BookmarkViewModel = hiltViewModel()
            BookmarksScreen(
                navController = navController,
                viewModel = bookmarkViewModel,
                onNavigateToRecord = { recordId, bookmarkId ->
                    if (recordId > 0 || bookmarkId > 0) {
                        navController.navigate("bookmark_workspace?recordId=$recordId&bookmarkId=$bookmarkId")
                    }
                }
            )
        }

        composable(
            route = "bookmark_workspace?recordId={recordId}&bookmarkId={bookmarkId}",
            arguments = listOf(
                androidx.navigation.navArgument("recordId") {
                    type = androidx.navigation.NavType.IntType
                    defaultValue = -1
                },
                androidx.navigation.navArgument("bookmarkId") {
                    type = androidx.navigation.NavType.IntType
                    defaultValue = -1
                }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            }
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getInt("recordId") ?: -1
            val bookmarkId = backStackEntry.arguments?.getInt("bookmarkId") ?: -1
            val workspaceViewModel: WorkspaceViewModel = hiltViewModel()
            val uiState by workspaceViewModel.uiState.collectAsState()
            val textToolbar = LocalTextToolbar.current
            var hasShownTarget by remember(recordId, bookmarkId) { mutableStateOf(false) }

            LaunchedEffect(recordId, bookmarkId) {
                when {
                    recordId > 0 -> workspaceViewModel.selectRecordById(recordId)
                    bookmarkId > 0 -> workspaceViewModel.selectRecordByBookmarkId(bookmarkId)
                    else -> navController.popBackStack()
                }
            }

            val targetReady = when {
                recordId > 0 -> uiState.selectedRecord?.id == recordId
                bookmarkId > 0 -> uiState.selectedRecord != null
                else -> false
            }

            LaunchedEffect(targetReady) {
                if (targetReady) {
                    hasShownTarget = true
                }
            }

            if (targetReady || hasShownTarget) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(recordId, bookmarkId) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                if (textToolbar.status == TextToolbarStatus.Shown) {
                                    do {
                                        val event = awaitPointerEvent()
                                    } while (event.changes.any { it.pressed })
                                    return@awaitEachGesture
                                }

                                var totalDx = 0f
                                var totalDy = 0f
                                var shouldReturn = false

                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull()
                                    if (change != null) {
                                        totalDx += change.positionChange().x
                                        totalDy += change.positionChange().y

                                        if (totalDx > 60f && kotlin.math.abs(totalDx) > kotlin.math.abs(totalDy) * 1.3f) {
                                            shouldReturn = true
                                            change.consume()
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                if (shouldReturn) {
                                    navController.popBackStack()
                                }
                            }
                        }
                ) {
                    WorkspaceScreen(
                        navController = navController,
                        viewModel = workspaceViewModel,
                        fromBookmarks = true,
                        onOpenDrawer = {},
                        onNavigateToSettings = {
                            navController.navigate("settings_standalone")
                        }
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        composable(
            route = "settings_standalone",
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            }
        ) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                navController = navController,
                viewModel = settingsViewModel
            )
        }

        composable(
            route = "flashcard?mode={mode}&limit={limit}&pos={pos}&scope={scope}",
            arguments = listOf(
                androidx.navigation.navArgument("mode") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = "ja_to_zh"
                },
                androidx.navigation.navArgument("limit") {
                    type = androidx.navigation.NavType.IntType
                    defaultValue = -1
                },
                androidx.navigation.navArgument("pos") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = "ALL"
                },
                androidx.navigation.navArgument("scope") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = "unarchived"
                }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            }
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "ja_to_zh"
            val limit = backStackEntry.arguments?.getInt("limit") ?: -1
            val pos = backStackEntry.arguments?.getString("pos") ?: "ALL"
            val scope = backStackEntry.arguments?.getString("scope") ?: "unarchived"

            val bookmarkViewModel: com.example.japanesegrammarapp.ui.BookmarkViewModel = hiltViewModel()
            FlashcardScreen(
                navController = navController,
                viewModel = bookmarkViewModel,
                studyMode = mode,
                cardLimit = limit,
                posFilter = pos,
                archiveScope = scope
            )
        }

        composable(
            route = "statistics",
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = EaseInOutQuart)
                )
            }
        ) {
            com.example.japanesegrammarapp.ui.statistics.StatisticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
