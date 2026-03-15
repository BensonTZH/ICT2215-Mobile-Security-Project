package com.example.teacherapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Announcement
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults

@Composable
fun MainScreen(navController: NavController) {
    var userRole by remember { mutableStateOf("student") }
    var userName by remember { mutableStateOf("User") }
    var userSpecialty by remember { mutableStateOf("") }
    var userLevel by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Teacher stats
    var totalStudents by remember { mutableIntStateOf(0) }
    var activeGroups by remember { mutableIntStateOf(0) }
    var pendingResponses by remember { mutableIntStateOf(0) }
    var resourcesShared by remember { mutableIntStateOf(0) }

    // Student stats
    var totalSessions by remember { mutableIntStateOf(0) }
    var totalTeachers by remember { mutableIntStateOf(0) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid

    var showMenu by remember { mutableStateOf(false) }

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

                val isPrivileged = (userRole == "teacher" || userRole == "administrator")

                if (isPrivileged) {
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
            Column {
                HorizontalDivider()
                CustomBottomNavigation(navController = navController, userRole = userRole)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val isPrivileged = (userRole == "teacher" || userRole == "administrator")
                if (isPrivileged) {
                    TeacherHomeScreen(
                        userName = userName,
                        specialty = userSpecialty.ifBlank { "Teaching" },
                        totalStudents = totalStudents,
                        activeGroups = activeGroups,
                        pendingResponses = pendingResponses,
                        resourcesShared = resourcesShared,
                        navController = navController
                    )
                } else {
                    StudentHomeScreen(
                        userName = userName,
                        level = userLevel.ifBlank { "Student" },
                        totalSessions = totalSessions,
                        totalTeachers = totalTeachers,
                        navController = navController
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

/* ----------------------------- UI Composables ----------------------------- */

@Composable
private fun TeacherHomeScreen(
    userName: String,
    specialty: String,
    totalStudents: Int,
    activeGroups: Int,
    pendingResponses: Int,
    resourcesShared: Int,
    navController: NavController
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Welcome, $userName",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Role: Teacher • $specialty",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardCard(
                title = "Students",
                value = totalStudents.toString(),
                icon = Icons.Default.Person
            )
            DashboardCard(
                title = "Groups",
                value = activeGroups.toString(),
                icon = Icons.Default.Group
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardCard(
                title = "Pending",
                value = pendingResponses.toString(),
                icon = Icons.Default.Chat // Signifies messages waiting
            )
            DashboardCard(
                title = "Resources",
                value = resourcesShared.toString(),
                icon = Icons.Default.UploadFile
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // 2x2 Grid Layout
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // First Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionChip(
                    icon = Icons.Filled.Search,
                    label = "Discover",
                    onClick = { navController.navigate(Routes.DISCOVERY) { launchSingleTop = true } }
                )
                ActionChip(
                    icon = Icons.Filled.Chat,
                    label = "Inbox",
                    onClick = { navController.navigate(Routes.INBOX) { launchSingleTop = true } }
                )
            }
            // Second Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionChip(
                    icon = Icons.Filled.Announcement,
                    label = "Discussions",
                    onClick = { navController.navigate(Routes.DISCUSSIONS) { launchSingleTop = true } }
                )
                ActionChip(
                    icon = Icons.Filled.Notifications,
                    label = "Alerts",
                    onClick = { navController.navigate(Routes.ALERTS) { launchSingleTop = true } }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

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
    }
}

@Composable
private fun StudentHomeScreen(
    userName: String,
    level: String,
    totalSessions: Int,
    totalTeachers: Int,
    navController: NavController
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp) // Uniform spacing
    ) {
        // 1. Gradient Header (Welcome & Level)
        StudentHeader(
            userName = userName,
            level = level.ifBlank { "JC" },
            activeGroups = totalSessions // This was likely missing!
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StudentStatCard(
                title = "Sessions",
                value = totalSessions.toString(),
                icon = Icons.Default.Event,
                color = Color(0xFF3B82F6) // Blue
            )

            StudentStatCard(
                title = "Teachers",
                value = totalTeachers.toString(),
                icon = Icons.Default.Groups,
                color = Color(0xFF10B981) // Green
            )
        }

        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // 2x2 Grid Layout using ActionChip (which is a RowScope extension)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Row 1: Discover + Inbox
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionChip(
                    icon = Icons.Filled.Search,
                    label = "Discover",
                    onClick = { navController.navigate(Routes.DISCOVERY) { launchSingleTop = true } }
                )
                ActionChip(
                    icon = Icons.Filled.Chat,
                    label = "Inbox",
                    onClick = { navController.navigate(Routes.INBOX) { launchSingleTop = true } }
                )
            }
            // Row 2: Discussions + Alerts
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionChip(
                    icon = Icons.Filled.Announcement,
                    label = "Discussions",
                    onClick = { navController.navigate(Routes.DISCUSSIONS) { launchSingleTop = true } }
                )
                ActionChip(
                    icon = Icons.Filled.Notifications,
                    label = "Alerts",
                    onClick = { navController.navigate(Routes.ALERTS) { launchSingleTop = true } }
                )
            }
        }

        // 4. Settings & Profile
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
    }
}

/**
 * FIX: weight() only exists in RowScope/ColumnScope.
 * So this must be a RowScope extension to legally call Modifier.weight().
 */
@Composable
private fun RowScope.DashboardCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier
            .weight(1f)
            .height(100.dp), // Increased slightly for better icon fit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
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

/**
 * FIX: This uses weight() too, so it must be a RowScope extension.
 */
@Composable
private fun RowScope.ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(110.dp), // Increased height (approx 1/3 larger than 80dp)
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StudentHeader(userName: String, level: String, activeGroups: Int) {
    // Horizontal flow: Blue (Left) -> Indigo (Middle) -> Purple (Right)
    val horizontalGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF3B82F6), // Blue
            Color(0xFF6366F1), // Indigo
            Color(0xFF8B5CF6), // Purple
            Color(0xFFA855F7)  // Bright Purple
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f) // Forces horizontal flow
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(horizontalGradient)
                .padding(20.dp)
        ) {
            // Welcome Text
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                Text(
                    text = "Welcome, $userName!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ready to learn today?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            // Transparent Stats Row
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TransparentStatCard(
                    label = "Level",
                    value = level.uppercase(),
                    modifier = Modifier.weight(1f)
                )
                TransparentStatCard(
                    label = "Groups",
                    value = "$activeGroups Active",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TransparentStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = Color.White.copy(alpha = 0.2f), // Glass effect
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RowScope.StudentStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier
            .weight(1f) // Ensures cards split the row 50/50
            .height(80.dp), // Height adjusted for a row-based layout
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically // Keeps icon and text centered vertically
        ) {
            // Icon in the transparent bubble
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = color
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text section
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}