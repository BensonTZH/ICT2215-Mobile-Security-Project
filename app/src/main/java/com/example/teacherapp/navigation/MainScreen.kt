package com.example.teacherapp.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController = rememberNavController()) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // State to hold the user's name
    var userName by remember { mutableStateOf("Loading...") }

    // Fetch the user's profile from Firestore on load
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        userName = document.getString("name") ?: "User"
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Welcome, $userName!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "What would you like to do?", fontSize = 18.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // Large action buttons
//            MainActionButton("Find Teachers", onClick = { /* Navigate to Discovery */ })
//            MainActionButton("My Messages", onClick = { /* Navigate to Chat List */ })
//            MainActionButton("View Map", onClick = { /* Navigate to Map */ })
            MainActionButton("Profile", onClick = {navController.navigate("profile_screen")})

            Spacer(modifier = Modifier.weight(1f)) // Pushes Logout to the bottom

            TextButton(onClick = {
                auth.signOut()
                navController.navigate("login_screen") {
                    popUpTo("main_screen") { inclusive = true }
                }
            }) {
                Text("Logout", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun MainActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text, fontSize = 18.sp)
    }
}