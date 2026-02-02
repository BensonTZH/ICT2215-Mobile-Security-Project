//package com.example.teacherapp.navigation
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ProfileScreen(navController: NavController) {
//    val auth = FirebaseAuth.getInstance()
//    val db = FirebaseFirestore.getInstance()
//
//    var name by remember { mutableStateOf("User") }
//    var email by remember { mutableStateOf("user@gmail.com") }
//    var role by remember { mutableStateOf("Student") }
//    var level by remember { mutableStateOf("JC") }
//    var teachersCount by remember { mutableStateOf(5) }
//    var groupsCount by remember { mutableStateOf(4) }
//    var resourcesCount by remember { mutableStateOf(12) }
//    var isLoading by remember { mutableStateOf(true) }
//
//    // Fetch data from Firestore
//    LaunchedEffect(Unit) {
//        val uid = auth.currentUser?.uid
//        if (uid != null) {
//            db.collection("users").document(uid).get()
//                .addOnSuccessListener { doc ->
//                    name = doc.getString("name") ?: "User"
//                    email = auth.currentUser?.email ?: doc.getString("email") ?: "user@gmail.com"
//                    role = doc.getString("role")?.replaceFirstChar { it.uppercase() } ?: "Student"
//                    level = doc.getString("level") ?: "JC"
//                    teachersCount = (doc.getLong("teachersCount") ?: 5L).toInt()
//                    groupsCount = (doc.getLong("groupsCount") ?: 4L).toInt()
//                    resourcesCount = (doc.getLong("resourcesCount") ?: 12L).toInt()
//                    isLoading = false
//                }
//        }
//    }
//
//    Scaffold(
//        containerColor = Color(0xFFF5F7FA),
//        bottomBar = {
//            ProfileBottomNavigationBar(navController)
//        }
//    ) { paddingValues ->
//        LazyColumn(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues),
//            contentPadding = PaddingValues(bottom = 16.dp)
//        ) {
//            // Header
//            item {
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .background(Color.White)
//                        .padding(horizontal = 20.dp, vertical = 16.dp)
//                ) {
//                    Column {
//                        Text(
//                            text = "Profile",
//                            fontSize = 28.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = Color(0xFF1F2937)
//                        )
//                        Text(
//                            text = "Manage your account",
//                            fontSize = 16.sp,
//                            color = Color(0xFF6B7280),
//                            modifier = Modifier.padding(top = 4.dp)
//                        )
//                    }
//                }
//            }
//
//            // Gradient Profile Card
//            item {
//                GradientProfileCard(
//                    name = name,
//                    email = email,
//                    level = level,
//                    teachersCount = teachersCount,
//                    groupsCount = groupsCount
//                )
//            }
//
//            // Contact Information
//            item {
//                ContactInformationSection(email = email, role = role)
//            }
//
//            // Menu Options
//            item {
//                ProfileMenuOption(
//                    icon = Icons.Default.Settings,
//                    title = "Account Settings",
//                    iconBackground = Color(0xFFF3F4F6),
//                    iconTint = Color(0xFF6B7280),
//                    onClick = { navController.navigate("settings_screen") }
//                )
//            }
//
//            item {
//                ProfileMenuOption(
//                    icon = Icons.Default.Description,
//                    title = "My Resources",
//                    iconBackground = Color(0xFFDEEBFF),
//                    iconTint = Color(0xFF3B82F6),
//                    badge = resourcesCount.toString(),
//                    onClick = { /* Navigate to resources */ }
//                )
//            }
//
//            item {
//                ProfileMenuOption(
//                    icon = Icons.Default.Group,
//                    title = "My Groups",
//                    iconBackground = Color(0xFFD1FAE5),
//                    iconTint = Color(0xFF10B981),
//                    badge = groupsCount.toString(),
//                    onClick = { /* Navigate to groups */ }
//                )
//            }
//
//            item {
//                ProfileMenuOption(
//                    icon = Icons.Default.MenuBook,
//                    title = "Learning Progress",
//                    iconBackground = Color(0xFFF3E8FF),
//                    iconTint = Color(0xFF8B5CF6),
//                    onClick = { /* Navigate to progress */ }
//                )
//            }
//
//            // Logout Button
//            item {
//                Spacer(modifier = Modifier.height(16.dp))
//
//                Button(
//                    onClick = {
//                        auth.signOut()
//                        navController.navigate("login_screen") {
//                            popUpTo(0) { inclusive = true }
//                        }
//                    },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 16.dp)
//                        .height(56.dp),
//                    shape = RoundedCornerShape(12.dp),
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = Color(0xFFFEF2F2)
//                    )
//                ) {
//                    Icon(
//                        Icons.Default.Logout,
//                        contentDescription = "Logout",
//                        tint = Color(0xFFEF4444),
//                        modifier = Modifier.size(20.dp)
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(
//                        "Logout",
//                        color = Color(0xFFEF4444),
//                        fontSize = 16.sp,
//                        fontWeight = FontWeight.SemiBold
//                    )
//                }
//            }
//
//            // Version
//            item {
//                Spacer(modifier = Modifier.height(16.dp))
//                Text(
//                    text = "Version 1.0.0",
//                    fontSize = 14.sp,
//                    color = Color(0xFF9CA3AF),
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 16.dp),
//                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun GradientProfileCard(
//    name: String,
//    email: String,
//    level: String,
//    teachersCount: Int,
//    groupsCount: Int
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(16.dp),
//        shape = RoundedCornerShape(24.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//    ) {
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(
//                    Brush.horizontalGradient(
//                        colors = listOf(
//                            Color(0xFF3B82F6),
//                            Color(0xFF8B5CF6)
//                        )
//                    )
//                )
//                .padding(24.dp)
//        ) {
//            Column(
//                horizontalAlignment = Alignment.Start
//            ) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(16.dp)
//                ) {
//                    // Avatar
//                    Box(
//                        modifier = Modifier
//                            .size(80.dp)
//                            .clip(CircleShape)
//                            .background(Color.White.copy(alpha = 0.3f)),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text(
//                            text = name.take(1).uppercase(),
//                            fontSize = 32.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = Color.White
//                        )
//                    }
//
//                    // Name and Email
//                    Column {
//                        Text(
//                            text = name,
//                            fontSize = 24.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = Color.White
//                        )
//                        Text(
//                            text = email,
//                            fontSize = 14.sp,
//                            color = Color.White.copy(alpha = 0.9f),
//                            modifier = Modifier.padding(top = 4.dp)
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(20.dp))
//
//                // Stats Row
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    StatBadge(
//                        label = "Level",
//                        value = level,
//                        modifier = Modifier.weight(1f)
//                    )
//                    StatBadge(
//                        label = "Teachers",
//                        value = teachersCount.toString(),
//                        modifier = Modifier.weight(1f)
//                    )
//                    StatBadge(
//                        label = "Groups",
//                        value = groupsCount.toString(),
//                        modifier = Modifier.weight(1f)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun StatBadge(label: String, value: String, modifier: Modifier = Modifier) {
//    Column(
//        modifier = modifier
//            .clip(RoundedCornerShape(12.dp))
//            .background(Color.White.copy(alpha = 0.2f))
//            .padding(horizontal = 12.dp, vertical = 12.dp),
//        horizontalAlignment = Alignment.Start
//    ) {
//        Text(
//            text = label,
//            fontSize = 12.sp,
//            color = Color.White.copy(alpha = 0.8f)
//        )
//        Text(
//            text = value,
//            fontSize = 18.sp,
//            fontWeight = FontWeight.Bold,
//            color = Color.White
//        )
//    }
//}
//
//@Composable
//fun ContactInformationSection(email: String, role: String) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp, vertical = 8.dp),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.White),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(20.dp)
//        ) {
//            Text(
//                text = "Contact Information",
//                fontSize = 18.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color(0xFF1F2937)
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Email
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                Icon(
//                    Icons.Default.Email,
//                    contentDescription = "Email",
//                    tint = Color(0xFF6B7280),
//                    modifier = Modifier.size(20.dp)
//                )
//                Text(
//                    text = email,
//                    fontSize = 15.sp,
//                    color = Color(0xFF1F2937)
//                )
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            // Role
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                Icon(
//                    Icons.Default.School,
//                    contentDescription = "Role",
//                    tint = Color(0xFF6B7280),
//                    modifier = Modifier.size(20.dp)
//                )
//                Text(
//                    text = "$role",
//                    fontSize = 15.sp,
//                    color = Color(0xFF1F2937)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun ProfileMenuOption(
//    icon: ImageVector,
//    title: String,
//    iconBackground: Color,
//    iconTint: Color,
//    badge: String? = null,
//    onClick: () -> Unit
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp, vertical = 6.dp),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.White),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//        onClick = onClick
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                // Icon with background
//                Box(
//                    modifier = Modifier
//                        .size(48.dp)
//                        .clip(RoundedCornerShape(12.dp))
//                        .background(iconBackground),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        imageVector = icon,
//                        contentDescription = title,
//                        tint = iconTint,
//                        modifier = Modifier.size(24.dp)
//                    )
//                }
//
//                // Title
//                Text(
//                    text = title,
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Medium,
//                    color = Color(0xFF1F2937)
//                )
//            }
//
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                // Badge if present
//                if (badge != null) {
//                    Surface(
//                        shape = CircleShape,
//                        color = Color(0xFFDEEBFF)
//                    ) {
//                        Text(
//                            text = badge,
//                            fontSize = 12.sp,
//                            fontWeight = FontWeight.SemiBold,
//                            color = Color(0xFF3B82F6),
//                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
//                        )
//                    }
//                }
//
//                // Arrow
//                Icon(
//                    Icons.Default.ChevronRight,
//                    contentDescription = "Navigate",
//                    tint = Color(0xFF9CA3AF),
//                    modifier = Modifier.size(20.dp)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun ProfileBottomNavigationBar(navController: NavController) {
//    val selectedColor = Color(0xFF3B82F6)
//    val unselectedColor = Color(0xFF9CA3AF)
//
//    NavigationBar(
//        containerColor = Color.White,
//        tonalElevation = 8.dp
//    ) {
//        NavigationBarItem(
//            icon = {
//                Icon(
//                    Icons.Default.Home,
//                    contentDescription = "Home",
//                    tint = unselectedColor
//                )
//            },
//            label = {
//                Text(
//                    "Home",
//                    color = unselectedColor,
//                    fontSize = 12.sp
//                )
//            },
//            selected = false,
//            onClick = { navController.navigate("main_screen") }
//        )
//
//        NavigationBarItem(
//            icon = {
//                Icon(
//                    Icons.Default.Search,
//                    contentDescription = "Discover",
//                    tint = unselectedColor
//                )
//            },
//            label = {
//                Text(
//                    "Discover",
//                    color = unselectedColor,
//                    fontSize = 12.sp
//                )
//            },
//            selected = false,
//            onClick = { navController.navigate("discovery_screen") }
//        )
//
//        NavigationBarItem(
//            icon = {
//                BadgedBox(
//                    badge = {
//                        Badge(
//                            containerColor = Color(0xFFEF4444)
//                        ) {
//                            Text(
//                                "3",
//                                color = Color.White,
//                                fontSize = 10.sp
//                            )
//                        }
//                    }
//                ) {
//                    Icon(
//                        Icons.Default.Chat,
//                        contentDescription = "Chats",
//                        tint = unselectedColor
//                    )
//                }
//            },
//            label = {
//                Text(
//                    "Chats",
//                    color = unselectedColor,
//                    fontSize = 12.sp
//                )
//            },
//            selected = false,
//            onClick = { /* Navigate to chats */ }
//        )
//
//        NavigationBarItem(
//            icon = {
//                BadgedBox(
//                    badge = {
//                        Badge(
//                            containerColor = Color(0xFFEF4444)
//                        ) {
//                            Text(
//                                "2",
//                                color = Color.White,
//                                fontSize = 10.sp
//                            )
//                        }
//                    }
//                ) {
//                    Icon(
//                        Icons.Default.Notifications,
//                        contentDescription = "Alerts",
//                        tint = unselectedColor
//                    )
//                }
//            },
//            label = {
//                Text(
//                    "Alerts",
//                    color = unselectedColor,
//                    fontSize = 12.sp
//                )
//            },
//            selected = false,
//            onClick = { /* Navigate to alerts */ }
//        )
//
//        NavigationBarItem(
//            icon = {
//                Icon(
//                    Icons.Default.Person,
//                    contentDescription = "Profile",
//                    tint = selectedColor
//                )
//            },
//            label = {
//                Text(
//                    "Profile",
//                    color = selectedColor,
//                    fontSize = 12.sp
//                )
//            },
//            selected = true,
//            onClick = { }
//        )
//    }
//}