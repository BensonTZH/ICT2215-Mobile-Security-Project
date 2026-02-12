package com.example.teacherapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.teacherapp.navigation.CustomBottomNavigation
import androidx.compose.material3.HorizontalDivider

@Composable
fun MainScreen(navController: NavController) {
    var userRole by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("User") }
    var userSpecialty by remember { mutableStateOf("") }
    var userLevel by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Teacher stats
    var totalStudents by remember { mutableStateOf(0) }
    var activeGroups by remember { mutableStateOf(0) }
    var pendingMessages by remember { mutableStateOf(0) }
    var resourcesShared by remember { mutableStateOf(0) }

    // Student stats
    var totalSessions by remember { mutableStateOf(0) }
    var totalTeachers by remember { mutableStateOf(0) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        userRole = document.getString("role") ?: "student"
                        userName = document.getString("name") ?: "User"

                        // Get specialty/subject for teachers
                        val subjects = document.get("subjects") as? List<*>
                        userSpecialty = subjects?.firstOrNull()?.toString() ?: "Teaching"

                        // Get level for students
                        userLevel = document.getString("grade") ?: "Student"

                        // Fetch stats based on role
                        if (userRole == "teacher") {
                            fetchTeacherStats(userId, db) { students, groups, messages, resources ->
                                totalStudents = students
                                activeGroups = groups
                                pendingMessages = messages
                                resourcesShared = resources
                                isLoading = false
                            }
                        } else {
                            fetchStudentStats(userId, db) { sessions, teachers ->
                                totalSessions = sessions
                                totalTeachers = teachers
                                isLoading = false
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    isLoading = false
                }
        }
    }

    Scaffold(
        bottomBar = {
            Column {
                HorizontalDivider()
                CustomBottomNavigation(navController)
            }
        }
    )

    { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (userRole == "teacher") {
                    TeacherHomeScreen(
                        userName,
                        userSpecialty,
                        totalStudents,
                        activeGroups,
                        pendingMessages,
                        resourcesShared,
                        navController
                    )
                } else {
                    StudentHomeScreen(
                        userName,
                        userLevel,
                        totalSessions,
                        totalTeachers,
                        navController
                    )
                }
            }
        }
    }
}

// Fetch teacher stats from Firestore
fun fetchTeacherStats(
    teacherId: String,
    db: FirebaseFirestore,
    onComplete: (students: Int, groups: Int, messages: Int, resources: Int) -> Unit
) {
    var totalStudents = 0
    var activeGroups = 0
    var pendingMessages = 0
    var resourcesShared = 0

    // Count active groups where teacher is the creator/manager
    db.collection("groups")
        .whereEqualTo("teacherId", teacherId)
        .get()
        .addOnSuccessListener { groupDocs ->
            activeGroups = groupDocs.size()

            // Count total students across all groups
            groupDocs.documents.forEach { group ->
                val members = group.get("members") as? List<*>
                totalStudents += members?.size ?: 0
            }

            // Count unread alerts/announcements for teacher
            db.collection("alerts")
                .whereEqualTo("recipientId", teacherId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener { alertDocs ->
                    pendingMessages = alertDocs.size()

                    // Count resources shared by teacher
                    db.collection("resources")
                        .whereEqualTo("uploaderId", teacherId)
                        .get()
                        .addOnSuccessListener { resourceDocs ->
                            resourcesShared = resourceDocs.size()

                            onComplete(totalStudents, activeGroups, pendingMessages, resourcesShared)
                        }
                        .addOnFailureListener {
                            onComplete(totalStudents, activeGroups, pendingMessages, 0)
                        }
                }
                .addOnFailureListener {
                    onComplete(totalStudents, activeGroups, 0, 0)
                }
        }
        .addOnFailureListener {
            onComplete(0, 0, 0, 0)
        }
}

// Fetch student stats from Firestore
fun fetchStudentStats(
    studentId: String,
    db: FirebaseFirestore,
    onComplete: (sessions: Int, teachers: Int) -> Unit
) {
    var totalSessions = 0
    var totalTeachers = 0

    // Count groups student is member of
    db.collection("groups")
        .whereArrayContains("members", studentId)
        .get()
        .addOnSuccessListener { groupDocs ->
            totalSessions = groupDocs.size()

            // Count unique teachers from those groups
            val teacherIds = mutableSetOf<String>()
            groupDocs.documents.forEach { group ->
                val teacherId = group.getString("teacherId")
                if (teacherId != null) {
                    teacherIds.add(teacherId)
                }
            }
            totalTeachers = teacherIds.size

            onComplete(totalSessions, totalTeachers)
        }
        .addOnFailureListener {
            onComplete(0, 0)
        }
}

@Composable
fun TeacherHomeScreen(
    userName: String,
    specialty: String,
    totalStudents: Int,
    activeGroups: Int,
    pendingMessages: Int,
    resourcesShared: Int,
    navController: NavController
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Purple Gradient Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF3B82F6),
                            Color(0xFF8B5CF6)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Welcome, $userName!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Here's your teaching overview",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Specialty Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Specialty",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = specialty,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Stats Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Row 1: Total Students & Active Groups
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    icon = Icons.Default.Person,
                    iconColor = Color(0xFF3B82F6),
                    value = totalStudents.toString(),
                    label = "Total Students",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.Groups,
                    iconColor = Color(0xFF10B981),
                    value = activeGroups.toString(),
                    label = "Active Groups",
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Alerts & Resources Shared
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    icon = Icons.Default.Notifications,
                    iconColor = Color(0xFFF59E0B),
                    value = pendingMessages.toString(),
                    label = "Alerts",
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("alerts_screen") }
                )
                StatCard(
                    icon = Icons.Default.Description,
                    iconColor = Color(0xFF8B5CF6),
                    value = resourcesShared.toString(),
                    label = "Resources Shared",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StudentHomeScreen(
    userName: String,
    level: String,
    totalSessions: Int,
    totalTeachers: Int,
    navController: NavController
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Purple Gradient Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF3B82F6),
                            Color(0xFF8B5CF6)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Welcome back, $userName!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ready to learn today?",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Level & Groups Info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Level",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = level,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Groups",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "$totalSessions Active",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Stats Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    icon = Icons.Default.CalendarToday,
                    iconColor = Color(0xFF3B82F6),
                    value = totalSessions.toString(),
                    label = "Sessions",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.Person,
                    iconColor = Color(0xFF10B981),
                    value = totalTeachers.toString(),
                    label = "Teachers",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick ?: {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}
