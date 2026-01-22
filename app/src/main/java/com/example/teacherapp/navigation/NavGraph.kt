package com.example.teacherapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavHostController
import com.example.teacherapp.StartScreen

object Routes {
    const val AUTH_GRAPH = "auth_graph"
    const val MAIN_GRAPH = "main_graph"

    const val START = "start_screen"
    const val LOGIN = "login_screen"
    const val REGISTER = "register_screen"
    const val MAIN = "main_screen"
    const val PROFILE = "profile_screen"

    const val SETUP = "setup_profile_screen"

    const val DISCOVERY = "discovery_screen"

    const val SETTINGS = "settings_screen"
    const val UPLOAD = "upload_resources"
}


@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.START // Always starts here
    ) {
        // Direct route to Start
        composable(Routes.START) {
            StartScreen(navController = navController)
        }

        // Direct route to Login
        composable(Routes.LOGIN) {
            LoginScreen(navController = navController)
        }

        // Direct route to Register
        composable(Routes.REGISTER) {
            RegisterScreen(navController = navController)
        }

        composable(Routes.MAIN){
            MainScreen(navController = navController)
        }

        composable(Routes.PROFILE){
            ProfileScreen(navController = navController)
        }

        composable(Routes.SETUP){
            SetupProfileScreen(navController = navController)
        }
        composable(Routes.DISCOVERY){
            DiscoveryScreen(navController = navController)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }

        composable(Routes.UPLOAD){
            UploadScreen(navController = navController)
        }
    }
}