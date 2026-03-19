package com.example.teacherapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Announcement
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.teacherapp.navigation.admin.AdminHomeScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MainScreen(navController: NavController) {
    var userRole by remember { mutableStateOf("student") }
    var userName by remember { mutableStateOf("User") }
    var userSpecialty by remember { mutableStateOf("") }
    var userLevel by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    var totalStudents by remember { mutableIntStateOf(0) }
    var activeGroups by remember { mutableIntStateOf(0) }
    var pendingResponses by remember { mutableIntStateOf(0) }
    var resourcesShared by remember { mutableIntStateOf(0) }

    var totalSessions by remember { mutableIntStateOf(0) }
    var totalTeachers by remember { mutableIntStateOf(0) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid

    LaunchedEffect(uid) {
        isLoading = true
        if (uid == null) {
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                userRole = doc.getString("role") ?: "student"
                userName = doc.getString("name") ?: "User"

                val subjects = doc.get("subjects") as? List<*>
                userSpecialty = subjects?.firstOrNull()?.toString() ?: ""
                userLevel = doc.getString("grade") ?: "Student"

                if (userRole == "teacher" || userRole == "administrator") {
                    fetchTeacherStats(uid, db) { students, groups, pending, resources ->
                        totalStudents = students
                        activeGroups = groups
                        pendingResponses = pending
                        resourcesShared = resources
                        isLoading = false
                    }
                } else {
                    fetchStudentStats(uid, db) { sessions, teachers ->
                        totalSessions = sessions
                        totalTeachers = teachers
                        isLoading = false
                    }
                }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Scaffold(
        bottomBar = {
            if (!isLoading && userRole.isNotBlank() && userRole != "administrator") {
                Column {
                    HorizontalDivider()
                    CustomBottomNavigation(navController)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                userRole == "administrator" -> {
                    AdminHomeScreen(navController = navController)
                }
                userRole == "teacher" -> {
                    TeacherHomeScreen(
                        userName = userName,
                        specialty = userSpecialty,
                        totalStudents = totalStudents,
                        activeGroups = activeGroups,
                        pendingResponses = pendingResponses,
                        resourcesShared = resourcesShared,
                        navController = navController,
                        onSubmitTicket = { navController.navigate(Routes.SUBMIT_TICKET) },
                        onMyTickets = { navController.navigate(Routes.MY_TICKETS) }
                    )
                }
                else -> {
                    StudentHomeScreen(
                        userName = userName,
                        level = userLevel,
                        totalSessions = totalSessions,
                        activeGroups = totalSessions,
                        totalTeachers = totalTeachers,
                        navController = navController,
                        onSubmitTicket = { navController.navigate(Routes.SUBMIT_TICKET) },
                        onMyTickets = { navController.navigate(Routes.MY_TICKETS) }
                    )
                }
            }
        }
    }
}

private fun fetchTeacherStats(
    teacherId: String,
    db: FirebaseFirestore,
    onComplete: (students: Int, groups: Int, pending: Int, resources: Int) -> Unit
) {
    var totalStudents = 0
    var activeGroups = 0
    var pendingResponses = 0
    var resourcesShared = 0

    db.collection("groups")
        .whereEqualTo("teacherId", teacherId)
        .get()
        .addOnSuccessListener { groupDocs ->
            activeGroups = groupDocs.size()
            groupDocs.documents.forEach { group ->
                val members = group.get("members") as? List<*>
                totalStudents += members?.size ?: 0
            }

            db.collection("chats")
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("needsResponse", true)
                .get()
                .addOnSuccessListener { chatDocs ->
                    pendingResponses = chatDocs.size()

                    db.collection("resources")
                        .whereEqualTo("uploaderId", teacherId)
                        .get()
                        .addOnSuccessListener { resourceDocs ->
                            resourcesShared = resourceDocs.size()
                            onComplete(totalStudents, activeGroups, pendingResponses, resourcesShared)
                        }
                        .addOnFailureListener {
                            onComplete(totalStudents, activeGroups, pendingResponses, 0)
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

private fun fetchStudentStats(
    studentId: String,
    db: FirebaseFirestore,
    onComplete: (sessions: Int, teachers: Int) -> Unit
) {
    db.collection("groups")
        .whereArrayContains("members", studentId)
        .get()
        .addOnSuccessListener { groupDocs ->
            val totalSessions = groupDocs.size()
            val teacherIds = mutableSetOf<String>()
            groupDocs.documents.forEach { g ->
                g.getString("teacherId")?.let { teacherIds.add(it) }
            }
            onComplete(totalSessions, teacherIds.size)
        }
        .addOnFailureListener {
            onComplete(0, 0)
        }
}

@Composable
private fun TeacherHomeScreen(
    userName: String,
    specialty: String,
    totalStudents: Int,
    activeGroups: Int,
    pendingResponses: Int,
    resourcesShared: Int,
    navController: NavController,
    onSubmitTicket: () -> Unit,
    onMyTickets: () -> Unit
) {
    val scrollState = rememberScrollState()

// Teacher Theme Gradient: Forest -> Emerald -> Teal
    val teacherGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            Color(0xFF3B82F6), // Blue
            Color(0xFF6366F1), // Indigo
            Color(0xFFA855F7)  // Purple
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Management Gradient Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(teacherGradient)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Welcome Text inside the Card
                Column {
                    Text(
                        text = "Welcome, $userName",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Teacher${if (specialty.isNotBlank()) " • $specialty" else ""}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Transparent Inner Cards for Teacher Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Students Count (Transparent)
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Students", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Text("$totalStudents Total", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }

                    // Active Groups (Transparent)
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Groups", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Text("$activeGroups Active", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
            }
        }

        // 2. Secondary Stat Row (Pending & Resources)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompactStatCard(
                icon = Icons.Default.Chat,
                label = "Pending",
                value = pendingResponses.toString(),
                modifier = Modifier.weight(1f)
            )
            CompactStatCard(
                icon = Icons.Default.UploadFile,
                label = "Resources",
                value = resourcesShared.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        ActionButton(
            icon = Icons.Filled.Group,
            label = "Manage Groups (Teacher)",
            onClick = { navController.navigate(Routes.MANAGE_GROUPS) { launchSingleTop = true } }
        )
        ActionButton(
            icon = Icons.Filled.UploadFile,
            label = "Upload Resources (Teacher)",
            onClick = { navController.navigate(Routes.UPLOAD) { launchSingleTop = true } }
        )

        Button(onClick = onSubmitTicket, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("Submit Ticket to Support")
        }
        OutlinedButton(onClick = onMyTickets, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("My Support Tickets")
        }
    }
}

@Composable
private fun StudentHomeScreen(
    userName: String,
    level: String,
    totalSessions: Int,
    totalTeachers: Int,
    activeGroups: Int,
    navController: NavController,
    onSubmitTicket: () -> Unit,
    onMyTickets: () -> Unit
) {
    val scrollState = rememberScrollState()
// Updated Gradient: Blue -> Indigo -> Purple
    val mainGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            Color(0xFF3B82F6), // Blue
            Color(0xFF6366F1), // Indigo
            Color(0xFFA855F7)  // Purple
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // 1. Main Gradient Card (Contains Welcome & Stats)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(mainGradient)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Welcome Text inside the Card
                Column {
                    Text(
                        text = "Welcome, $userName",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Ready to learn today?",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Transparent Inner Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Level (Transparent)
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Level", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Text(level, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }

                    // Active Groups (Transparent)
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Groups", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Text("$activeGroups Active", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompactStatCard(
                icon = Icons.Default.MenuBook,
                label = "Sessions",
                value = totalSessions.toString(),
                modifier = Modifier.weight(1f)
            )
            CompactStatCard(
                icon = Icons.Default.School,
                label = "Teachers",
                value = totalTeachers.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        ActionButton(
            icon = Icons.Filled.Search,
            label = "Discover Teachers",
            onClick = { navController.navigate(Routes.DISCOVERY) { launchSingleTop = true } }
        )
        ActionButton(
            icon = Icons.Filled.Chat,
            label = "Inbox",
            onClick = { navController.navigate(Routes.INBOX) { launchSingleTop = true } }
        )
        ActionButton(
            icon = Icons.Filled.Announcement,
            label = "Discussions",
            onClick = { navController.navigate(Routes.DISCUSSIONS) { launchSingleTop = true } }
        )
        ActionButton(
            icon = Icons.Filled.Notifications,
            label = "Alerts",
            onClick = { navController.navigate(Routes.ALERTS) { launchSingleTop = true } }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionChip(
                icon = Icons.Filled.Settings,
                label = "Settings",
                onClick = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } }
            )
            ActionChip(
                icon = Icons.Filled.Person,
                label = "Profile",
                onClick = { navController.navigate(Routes.PROFILE) { launchSingleTop = true } }
            )
        }

        Button(onClick = onSubmitTicket, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("Submit Ticket to Support")
        }
        OutlinedButton(onClick = onMyTickets, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("My Support Tickets")
        }
    }
}

@Composable
private fun RowScope.DashboardCard(title: String, value: String) {
    Card(
        modifier = Modifier
            .weight(1f)
            .height(92.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label)
    }
}

@Composable
private fun RowScope.ActionChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, maxLines = 1)
    }
}

@Composable
private fun CompactStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FE)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                color = Color(0xFF6366F1).copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.padding(8.dp).size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2D3243)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }
        }
    }
}