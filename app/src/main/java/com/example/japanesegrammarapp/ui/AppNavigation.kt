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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import com.example.japanesegrammarapp.ui.screens.*
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
                            drawerState = drawerState,
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