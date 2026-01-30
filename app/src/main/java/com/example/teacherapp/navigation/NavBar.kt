package com.example.teacherapp.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun BotNavBar(
    navController: NavController,
    showBottomBar: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column {
                    HorizontalDivider()
                    CustomBottomNavigation(navController)
                }
            }
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}

@Composable
fun CustomBottomNavigation(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            label = "Home",
            icon = Icons.Filled.Home,
            onClick = {
                navController.navigate(Routes.MAIN) {
                    launchSingleTop = true
                }
            }
        )
        BottomNavItem(
            label = "Discover",
            icon = Icons.Filled.Search,
            onClick = {
                navController.navigate(Routes.DISCOVERY) {
                    launchSingleTop = true
                }
            }
        )
        BottomNavItem(
            label = "Inbox",
            icon = Icons.Filled.QuestionAnswer,
            onClick = {
                navController.navigate(Routes.INBOX) {
                    launchSingleTop = true
                }
            }
        )
        BottomNavItem(
            label = "Alerts",
            icon = Icons.Filled.Notifications,
            onClick = {
                navController.navigate(Routes.ALERTS) {
                    launchSingleTop = true
                }
            }
        )
        BottomNavItem(
            label = "Profile",
            icon = Icons.Filled.Person,
            onClick = {
                navController.navigate(Routes.PROFILE) {
                    launchSingleTop = true
                }
            }
        )
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 10.dp)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}