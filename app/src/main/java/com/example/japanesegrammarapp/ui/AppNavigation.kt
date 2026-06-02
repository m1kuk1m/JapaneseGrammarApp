package com.example.japanesegrammarapp.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.ui.screens.*
import com.example.japanesegrammarapp.ui.screens.components.HistorySidebar
import com.example.japanesegrammarapp.ui.screens.components.ExportSelectionDialog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
        composable("home_pager") {
            val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
            val coroutineScope = rememberCoroutineScope()
            
            val workspaceViewModel: WorkspaceViewModel = hiltViewModel()
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            
            val history = workspaceViewModel.history.collectAsLazyPagingItems()
            val allHistoryForExport by workspaceViewModel.allHistoryForExport.collectAsState()
            val uiState by workspaceViewModel.uiState.collectAsState()
            
            var recordToDelete by remember { mutableStateOf<AnalysisDomainRecord?>(null) }
            var showExportDialog by remember { mutableStateOf(false) }

            val WashiBg = androidx.compose.material3.MaterialTheme.colorScheme.background
            val SumiInk = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
            
            LaunchedEffect(Unit) {
                externalTextFlow.collect { text ->
                    workspaceViewModel.startNewAnalysisWithText(text, isExternal = true)
                    workspaceViewModel.startAnalysis(text, null, forceNavigate = true)
                    pagerState.animateScrollToPage(0)
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
            
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = drawerState.isOpen,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = if (uiState.wallpaperUri.isNotBlank()) Color.Transparent else WashiBg,
                        modifier = Modifier.width(310.dp).fillMaxHeight()
                    ) {
                        HistorySidebar(
                            historyList = history,
                            selectedRecord = uiState.selectedRecord,
                            onSelectRecord = { record -> workspaceViewModel.selectRecord(record) },
                            onClearSelection = { workspaceViewModel.clearSelectedRecord() },
                            onDeleteRecord = { record -> recordToDelete = record },
                            onExportAll = {
                                coroutineScope.launch { drawerState.close() }
                                workspaceViewModel.loadAllHistoryForExport()
                                showExportDialog = true
                            },
                            onExportRecord = { record -> workspaceViewModel.exportRecord(record) },
                            onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
                        )
                    }
                }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = drawerState.currentValue == DrawerValue.Closed
                    ) { page ->
                        when (page) {
                            0 -> {
                                WorkspaceScreen(
                                    navController = navController,
                                    viewModel = workspaceViewModel,
                                    onOpenDrawer = {
                                        coroutineScope.launch { drawerState.open() }
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
                    
                    // Edge Swipe Interceptor for opening the drawer
                    if (pagerState.currentPage == 0 && drawerState.isClosed) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(30.dp)
                                .align(Alignment.CenterStart)
                                .pointerInput(Unit) {
                                    var totalDrag = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { _ -> totalDrag = 0f },
                                        onHorizontalDrag = { _, dragAmount ->
                                            totalDrag += dragAmount
                                            if (totalDrag > 10f) { // Swiping right
                                                coroutineScope.launch { drawerState.open() }
                                                totalDrag = 0f
                                            }
                                        }
                                    )
                                }
                        )
                    }
                }
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
                    containerColor = Color.White
                )
            }

            // Export Selection Dialog
            if (showExportDialog) {
                ExportSelectionDialog(
                    historyList = allHistoryForExport,
                    onDismiss = { showExportDialog = false },
                    onExportSelected = { selectedRecords ->
                        workspaceViewModel.exportAllHistory(selectedRecords)
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
            CameraScreen(
                navController = navController,
                galleryImageUriString = galleryImageUriString
            )
        }
    }
}