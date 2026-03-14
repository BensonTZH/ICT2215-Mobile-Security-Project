package com.example.teacherapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.teacherapp.navigation.auth.LoginScreen
import com.example.teacherapp.navigation.auth.RegisterScreen
import com.example.teacherapp.navigation.discussions.DiscussionScreen
import com.example.teacherapp.navigation.discussions.GroupThreadsScreen
import com.example.teacherapp.navigation.discussions.ThreadDetailScreen
import com.example.teacherapp.navigation.groups.GroupDetailsScreen
import com.example.teacherapp.navigation.groups.ManageGroupsScreen
import com.example.teacherapp.navigation.user.ProfileScreen
import com.example.teacherapp.navigation.user.PublicProfileScreen
import com.example.teacherapp.navigation.user.SetupProfileScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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

    const val MANAGE_GROUPS = "manage_groups"

    const val GROUP_DETAILS = "group_details/{groupId}"

    const val DISCUSSIONS = "discussions_screen"

    const val THREAD_DETAILS = "thread_details/{threadId}"
}

@Composable
fun NavGraph(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()

    // --- Make UID reactive (fixes stale uid after login/logout) ---
    var uid by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser?.uid) }

    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            uid = firebaseAuth.currentUser?.uid
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // Used only for client-side navigation gating.
    // Firestore Security Rules must still enforce role permissions server-side.
    var userRole by remember { mutableStateOf<String?>(null) }

    // --- Fetch role whenever UID changes (null = loading) ---
    LaunchedEffect(uid) {
        userRole = null
        val currentUid = uid ?: return@LaunchedEffect

        try {
            val doc = db.collection("users").document(currentUid).get().await()
            userRole = doc.getString("role") ?: "student"
        } catch (_: Exception) {
            userRole = "student"
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(navController = navController)
        }

        composable(Routes.REGISTER) {
            RegisterScreen(navController = navController)
        }

        composable(Routes.MAIN) {
            MainScreen(navController = navController)
        }

        composable(Routes.PROFILE) {
            ProfileScreen(navController = navController)
        }

        composable(Routes.SETUP) {
            SetupProfileScreen(navController = navController)
        }

        composable(Routes.DISCOVERY) {
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

        composable(route = "${Routes.PUBLIC_PROFILE}/{teacherId}") { backStackEntry ->
            val teacherId = backStackEntry.arguments?.getString("teacherId")
            PublicProfileScreen(navController, teacherId)
        }

        composable(Routes.UPLOAD) {
            RequirePrivileged(
                userRole = userRole,
                onDenied = {
                    UnauthorizedScreen(
                        message = "Teachers only: upload resources.",
                        onGoHome = { navController.navigate(Routes.MAIN) { launchSingleTop = true } }
                    )
                }
            ) {
                UploadScreen(navController = navController)
            }
        }

        composable(Routes.RESOURCES) {
            ResourceScreen(navController = navController)
        }

        composable(Routes.ALERTS) {
            AlertsScreen(navController = navController)
        }

        composable(Routes.MANAGE_GROUPS) {
            RequirePrivileged(
                userRole = userRole,
                onDenied = {
                    UnauthorizedScreen(
                        message = "Teachers only: manage groups.",
                        onGoHome = { navController.navigate(Routes.MAIN) { launchSingleTop = true } }
                    )
                }
            ) {
                ManageGroupsScreen(navController = navController)
            }
        }

        composable(Routes.GROUP_DETAILS) { entry ->
            val groupId = entry.arguments?.getString("groupId")
            if (groupId != null) {
                GroupDetailsScreen(navController = navController, groupId = groupId)
            }
        }

        composable("chats_screen") {
            ChatsScreen(navController = navController)
        }

        composable("groups_screen") {
            GroupsScreen(navController = navController)
        }

        composable("find_student") {
            FindStudentScreen(navController = navController)
        }

        composable(Routes.DISCUSSIONS) {
            DiscussionScreen(navController = navController)
        }

        composable(
            route = "thread_detail_screen/{threadId}",
            arguments = listOf(navArgument("threadId") { type = NavType.StringType })
        ) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getString("threadId") ?: ""
            ThreadDetailScreen(navController, threadId)
        }

        composable(
            route = "group_threads/{groupId}/{groupName}",
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("groupName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val groupName = backStackEntry.arguments?.getString("groupName") ?: ""
            GroupThreadsScreen(navController, groupId, groupName)
        }
    }
}