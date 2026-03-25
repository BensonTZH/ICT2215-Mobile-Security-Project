package com.example.teacherapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem(Routes.MAIN, Icons.Default.School, "Home")
    object Discover : BottomNavItem(Routes.DISCOVERY, Icons.Default.Search, "Discover")
    object Inbox : BottomNavItem(Routes.INBOX, Icons.Default.Chat, "Inbox")
    object Profile : BottomNavItem(Routes.PROFILE, Icons.Default.Person, "Profile")
    object Groups : BottomNavItem(Routes.MANAGE_GROUPS, Icons.Default.Group, "Groups")
    object Tickets : BottomNavItem(Routes.MY_TICKETS, Icons.Default.ConfirmationNumber, "Tickets")
    object Alerts : BottomNavItem(Routes.ALERTS, Icons.Default.Notifications, "Alerts")
}

@Composable
fun CustomBottomNavigation(navController: NavController, userRole: String? = "student") {
    val role = userRole ?: "student"
    val items = if (role == "teacher") {
        listOf(
            BottomNavItem.Home,
            BottomNavItem.Groups,
            BottomNavItem.Inbox,
            BottomNavItem.Tickets,
            BottomNavItem.Profile
        )
    } else {
        listOf(
            BottomNavItem.Home,
            BottomNavItem.Discover,
            BottomNavItem.Inbox,
            BottomNavItem.Alerts,
            BottomNavItem.Profile
        )
    }

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun BotNavBar(navController: NavController, userRole: String? = "student") {
    CustomBottomNavigation(navController, userRole)
}
