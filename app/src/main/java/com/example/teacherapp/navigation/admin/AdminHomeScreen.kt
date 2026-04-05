package com.example.teacherapp.navigation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.teacherapp.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db   = FirebaseFirestore.getInstance()

    var sessionActive by remember { mutableStateOf(false) }

    
    DisposableEffect(Unit) {
        val reg = db.document("sessions/current")
            .addSnapshotListener { snapshot, _ ->
                sessionActive = snapshot?.getBoolean("active") ?: false
            }
        onDispose { reg.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Controls", style = TextStyle(fontSize = 34.sp)) },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.PROFILE) }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                    IconButton(onClick = {
                        auth.signOut()
                        navController.navigate(Routes.LOGIN) { popUpTo(0) }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { navController.navigate("admin_users") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Manage Users") }

            Button(
                onClick = { navController.navigate(Routes.ADMIN_TICKETS_INBOX) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Support Tickets Inbox") }

            Button(
                onClick = { navController.navigate(Routes.ADMIN_DISCUSSION) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Manage Discussions") }

            Button(
                onClick = { navController.navigate(Routes.ADMIN_ANNOUNCEMENT) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Manage Announcements") }

            
            Button(
                onClick = {
                    db.document("sessions/current")
                        .set(mapOf("active" to !sessionActive))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sessionActive) Color(0xFFDC2626) else Color(0xFF16A34A)
                )
            ) {
                Text(if (sessionActive) "Stop Online Lesson" else "Start Online Lesson")
            }
        }
    }
}
