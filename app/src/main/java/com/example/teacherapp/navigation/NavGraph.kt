package com.example.teacherapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.teacherapp.StartScreen
import com.example.teacherapp.navigation.auth.LoginScreen
import com.example.teacherapp.navigation.auth.RegisterScreen
import com.example.teacherapp.navigation.discussions.DiscussionScreen
import com.example.teacherapp.navigation.groups.GroupDetailsScreen
import com.example.teacherapp.navigation.discussions.GroupThreadsScreen
import com.example.teacherapp.navigation.discussions.ThreadDetailScreen
import com.example.teacherapp.navigation.admin.AdminHomeScreen
import com.example.teacherapp.navigation.admin.AdminTicketDetailScreen
import com.example.teacherapp.navigation.admin.AdminTicketsInboxScreen
import com.example.teacherapp.navigation.admin.AdminUserFormScreen
import com.example.teacherapp.navigation.admin.AdminUsersScreen
import com.example.teacherapp.navigation.groups.ManageGroupsScreen
import com.example.teacherapp.navigation.support.MyTicketsScreen
import com.example.teacherapp.navigation.support.SubmitTicketScreen
import com.example.teacherapp.navigation.support.TicketDetailScreen
import com.example.teacherapp.navigation.user.ProfileScreen
import com.example.teacherapp.navigation.user.PublicProfileScreen
import com.example.teacherapp.navigation.user.SetupProfileScreen

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
    const val ADMIN_HOME = "admin_home"
    const val ADMIN_USERS = "admin_users"
    const val ADMIN_USER_EDIT = "admin_user_edit/{uidArg}"
    const val ADMIN_TICKETS_INBOX = "admin_tickets_inbox"
    const val SUBMIT_TICKET = "submit_support_ticket"
    const val MY_TICKETS = "my_support_tickets"
    const val TICKET_DETAIL = "ticket_detail/{ticketId}"
    const val ADMIN_TICKET_DETAIL = "admin_ticket_detail/{ticketId}"

}


@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN // CHANGED: Skip StartScreen, go directly to Login
    ) {
        // Direct route to Login (now the starting screen)
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
        composable(Routes.ADMIN_HOME) {
            AdminHomeScreen(navController = navController)
        }
        composable(Routes.ADMIN_USERS) {
            AdminUsersScreen(navController = navController)
        }
        composable(Routes.ADMIN_TICKETS_INBOX) {
            AdminTicketsInboxScreen(navController = navController)
        }
        composable(
            route = Routes.ADMIN_USER_EDIT,
            arguments = listOf(navArgument("uidArg") { type = NavType.StringType })
        ) { backStackEntry ->
            val uidArg = backStackEntry.arguments?.getString("uidArg") ?: "new"
            AdminUserFormScreen(navController = navController, uidArg = uidArg)
        }
        composable(Routes.SUBMIT_TICKET) {
            SubmitTicketScreen(navController = navController)
        }
        composable(Routes.MY_TICKETS) {
            MyTicketsScreen(navController = navController)
        }
        composable(
            route = Routes.TICKET_DETAIL,
            arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString("ticketId") ?: ""
            TicketDetailScreen(navController = navController, ticketId = ticketId)
        }
        composable(
            route = Routes.ADMIN_TICKET_DETAIL,
            arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString("ticketId") ?: ""
            AdminTicketDetailScreen(navController = navController, ticketId = ticketId)
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
        composable(Routes.MANAGE_GROUPS){
            ManageGroupsScreen(navController = navController)
        }


        composable(Routes.GROUP_DETAILS){
            val groupId = it.arguments?.getString("groupId")
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

        composable("alerts_screen") {
            AlertsScreen(navController = navController)  // Your existing alerts screen
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
