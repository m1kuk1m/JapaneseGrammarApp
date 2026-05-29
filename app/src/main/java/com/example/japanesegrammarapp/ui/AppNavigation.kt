package com.example.japanesegrammarapp.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.japanesegrammarapp.ui.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: AppViewModel = hiltViewModel()

    NavHost(
        navController = navController, 
        startDestination = "workspace",
        enterTransition = { 
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(150)) + fadeIn(animationSpec = tween(150)) 
        },
        exitTransition = { 
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)) 
        },
        popEnterTransition = { 
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(150)) + fadeIn(animationSpec = tween(150)) 
        },
        popExitTransition = { 
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)) 
        }
    ) {
        composable("workspace") {
            WorkspaceScreen(navController, viewModel)
        }
        composable("settings") {
            SettingsScreen(navController, viewModel)
        }
    }
}