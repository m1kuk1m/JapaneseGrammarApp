package com.example.japanesegrammarapp.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.japanesegrammarapp.ui.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "workspace",
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
        composable("workspace") {
            val workspaceViewModel: WorkspaceViewModel = hiltViewModel()
            WorkspaceScreen(navController, workspaceViewModel)
        }
        composable("settings") {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(navController, settingsViewModel)
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