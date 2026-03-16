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
import androidx.compose.material.icons.filled.Group
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (userRole == "administrator") {
                    AdminHomeScreen(navController = navController)
                } else if (userRole == "teacher") {
                    TeacherHomeScreen(
                        userName,
                        userSpecialty,
                        totalStudents,
                        activeGroups,
                        pendingMessages,
                        resourcesShared,
                        navController,
                        onSubmitTicket = { navController.navigate(Routes.SUBMIT_TICKET) },
                        onMyTickets = { navController.navigate(Routes.MY_TICKETS) }
                    )
                } else {
                    StudentHomeScreen(
                        userName,
                        userLevel,
                        totalSessions,
                        totalTeachers,
                        navController,
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

/* ----------------------------- UI Composables ----------------------------- */

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
            DashboardCard(title = "Students", value = totalStudents.toString())
            DashboardCard(title = "Groups", value = activeGroups.toString())
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardCard(title = "Pending", value = pendingResponses.toString())
            DashboardCard(title = "Resources", value = resourcesShared.toString())
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

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

            Button(
                onClick = onSubmitTicket,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Submit Ticket to Support")
            }
            OutlinedButton(
                onClick = onMyTickets,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("My Support Tickets")
            }
        }
    }
}

@Composable
private fun StudentHomeScreen(
    userName: String,
    level: String,
    totalSessions: Int,
    totalTeachers: Int,
    navController: NavController,
    onSubmitTicket: () -> Unit,
    onMyTickets: () -> Unit
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
            text = "Role: Student • $level",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardCard(title = "Sessions", value = totalSessions.toString())
            DashboardCard(title = "Teachers", value = totalTeachers.toString())
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

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

/**
 * FIX: weight() only exists in RowScope/ColumnScope.
 * So this must be a RowScope extension to legally call Modifier.weight().
 */
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

            Button(
                onClick = onSubmitTicket,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Submit Ticket to Support")
            }
            OutlinedButton(
                onClick = onMyTickets,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("My Support Tickets")
            }
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