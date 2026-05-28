package com.example.japanesegrammarapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.japanesegrammarapp.ui.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val viewModel = remember { AppViewModel(context.applicationContext) }

    NavHost(navController = navController, startDestination = "workspace") {
        composable("workspace") {
            WorkspaceScreen(navController, viewModel)
        }
        composable("settings") {
            SettingsScreen(navController)
        }
        // Retain for fallback / reference, but redirected to workspace
        composable("home") {
            WorkspaceScreen(navController, viewModel)
        }
        composable("input") {
            WorkspaceScreen(navController, viewModel)
        }
        composable("result") {
            WorkspaceScreen(navController, viewModel)
        }
    }
}