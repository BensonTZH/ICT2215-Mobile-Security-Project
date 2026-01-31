//// MainScreen.kt
//package com.example.teacherapp.navigation
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import androidx.wear.compose.material3.AppScaffold
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MainScreen(navController: NavController) {
//    // Firebase setup
//    var userRole by remember { mutableStateOf("") }
//    var userName by remember { mutableStateOf("User") }
//    var isLoading by remember { mutableStateOf(true) }
//    val auth = FirebaseAuth.getInstance()
//    val db = FirebaseFirestore.getInstance()
//    val userId = auth.currentUser?.uid
//
//    // Fetch user data (role and name)
//    LaunchedEffect(userId) {
//        if (userId != null) {
//            db.collection("users").document(userId).get()
//                .addOnSuccessListener { document ->
//                    if (document != null) {
//                        userRole = document.getString("role") ?: "student"
//                        userName = document.getString("name") ?: "User"
//                    }
//                    isLoading = false
//                }
//                .addOnFailureListener {
//                    isLoading = false
//                }
//        }
//    }
//
//    // Use shared scaffold (bottom bar + divider)
//    BotNavBar(navController = navController, showBottomBar = true) { innerPadding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//                .padding(20.dp)
//        ) {
//            Text(
//                text = "Dashboard",
//                style = MaterialTheme.typography.headlineMedium,
//                fontWeight = FontWeight.Bold
//            )
//            Spacer(modifier = Modifier.height(24.dp))
//
//            if (userRole == "teacher") {
//                TeacherDashboard(navController)
//            } else {
//                StudentDashboard(navController)
//            }
//        }
//    }
//}
//
//@Composable
//fun TeacherDashboard(navController: NavController) {
//    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
//        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
//            SquareActionButton("Upload\nResource", Icons.Default.CloudUpload, Modifier.weight(1f)) {
//                navController.navigate("upload_resources")
//            }
//            SquareActionButton("Manage\nGroups", Icons.Default.Groups, Modifier.weight(1f)) {
//                navController.navigate("manage_groups")
//            }
//        }
//
//        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
//            SquareActionButton("Messages", Icons.Default.QuestionAnswer, Modifier.weight(1f)) {
//                navController.navigate(Routes.INBOX)
//            }
//            Spacer(modifier = Modifier.weight(1f))
//        }
//    }
//}
//
//@Composable
//fun StudentDashboard(navController: NavController) {
//    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
//        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
//            SquareActionButton("Find a\nTeacher", Icons.Default.Search, Modifier.weight(1f)) {
//                navController.navigate(Routes.DISCOVERY)
//            }
//            SquareActionButton("My Study\nGroups", Icons.Default.Class, Modifier.weight(1f)) {
//                // navController.navigate("student_groups")
//            }
//        }
//
//        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
//            SquareActionButton("Study\nResources", Icons.Default.LibraryBooks, Modifier.weight(1f)) {
//                navController.navigate("view_resources")
//            }
//            Spacer(modifier = Modifier.weight(1f))
//        }
//
//        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
//            SquareActionButton("Inbox", Icons.Default.QuestionAnswer, Modifier.weight(1f)) {
//                navController.navigate(Routes.INBOX)
//            }
//            Spacer(modifier = Modifier.weight(1f))
//        }
//    }
//}
//
//@Composable
//fun SquareActionButton(
//    title: String,
//    icon: ImageVector,
//    modifier: Modifier = Modifier,
//    onClick: () -> Unit
//) {
//    Card(
//        onClick = onClick,
//        modifier = modifier.aspectRatio(1f),
//        shape = RoundedCornerShape(20.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.White)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            Icon(
//                imageVector = icon,
//                contentDescription = title,
//                modifier = Modifier.size(36.dp),
//                tint = Color(0xFF505D8A)
//            )
//            Spacer(modifier = Modifier.height(12.dp))
//            Text(
//                text = title,
//                textAlign = TextAlign.Center,
//                fontSize = 14.sp,
//                fontWeight = FontWeight.Bold,
//                lineHeight = 18.sp
//            )
//        }
//    }
//}
