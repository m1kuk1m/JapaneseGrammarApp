package com.example.japanesegrammarapp.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.japanesegrammarapp.ui.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val viewModel: AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = AppViewModelFactory(context.applicationContext)
    )

    NavHost(
        navController = navController, 
        startDestination = "workspace",
        enterTransition = { 
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(450)) + fadeIn(animationSpec = tween(450)) 
        },
        exitTransition = { 
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(450)) + fadeOut(animationSpec = tween(450)) 
        },
        popEnterTransition = { 
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(450)) + fadeIn(animationSpec = tween(450)) 
        },
        popExitTransition = { 
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(450)) + fadeOut(animationSpec = tween(450)) 
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