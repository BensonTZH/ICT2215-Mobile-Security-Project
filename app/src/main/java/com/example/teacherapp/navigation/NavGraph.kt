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
    const val INBOX = "inbox_screen"

    const val CHAT = "chat_screen"

    const val PUBLIC_PROFILE = "public_profile_screen"
    const val UPLOAD = "upload_resources"
    const val RESOURCES = "view_resources"
    const val ALERTS = "alerts_screen"
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
            MainScreen_2(navController = navController)
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

        composable(Routes.INBOX) {
            InboxScreen(navController = navController)
        }

        composable("${Routes.CHAT}/{otherUserId}") { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: return@composable
            MessageScreen_3(navController = navController, otherUserId = otherUserId)
        }

        composable(
            route = "${Routes.PUBLIC_PROFILE}/{teacherId}"
        ) { backStackEntry ->
            val teacherId = backStackEntry.arguments?.getString("teacherId")
            PublicProfileScreen(navController, teacherId)
        }
        composable(Routes.UPLOAD){
            UploadScreen(navController = navController)
        }
        composable(Routes.RESOURCES){
            ResourceScreen(navController = navController)
        }
        composable(Routes.ALERTS) {
            AlertsScreen(navController = navController)
        }
    }
}