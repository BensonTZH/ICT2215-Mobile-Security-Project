package com.example.teacherapp.navigation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun AdminHomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Admin Controls",
            style = MaterialTheme.typography.headlineSmall
        )

        Button(
            onClick = { navController.navigate("admin_users") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage Users")
        }

        Button(
            onClick = { navController.navigate("inbox_screen") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Support Tickets Inbox")
        }
    }
}
