package com.example.teacherapp.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    // These should be fetched from your Firestore 'users' document
    var userRole by remember { mutableStateOf("teacher") }
    val userName by remember { mutableStateOf("User") }

    Scaffold(
        topBar = { /* Your TopBar with Mail, Profile, Cog */ }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (userRole == "teacher") {
                TeacherDashboard(navController)
            } else {
                StudentDashboard(navController)
            }
        }
    }
}

@Composable
fun TeacherDashboard(navController: NavController) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Row 1: Max 2 buttons
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SquareActionButton("Upload\nResource", Icons.Default.CloudUpload, Modifier.weight(1f)) {
                navController.navigate("upload_resources")
            }
            SquareActionButton("Manage\nGroups", Icons.Default.Groups, Modifier.weight(1f)) {
                navController.navigate("manage_groups")
            }
        }
        // Row 2: 1 button
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SquareActionButton("Messages", Icons.Default.QuestionAnswer, Modifier.weight(1f)) {
                navController.navigate("inbox")
            }
            // Empty spacer to keep the button on the left as a square
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StudentDashboard(navController: NavController) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Row 1: Max 2 buttons
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SquareActionButton("Find a\nTeacher", Icons.Default.Search, Modifier.weight(1f)) {
                navController.navigate("teacher_discovery")
            }
            SquareActionButton("My Study\nGroups", Icons.Default.Class, Modifier.weight(1f)) {
                navController.navigate("student_groups")
            }
        }
        // Row 2: 1 button
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SquareActionButton("Study\nResources", Icons.Default.LibraryBooks, Modifier.weight(1f)) {
                navController.navigate("view_resources")
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
@Composable
fun SquareActionButton(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(36.dp),
                tint = Color(0xFF505D8A) // Your theme color
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp
            )
        }
    }
}