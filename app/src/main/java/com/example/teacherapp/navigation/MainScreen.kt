package com.example.teacherapp.navigation

import android.util.Log
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
import androidx.compose.material.icons.filled.LibraryBooks
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import com.google.firebase.firestore.Query

@Composable
fun MainScreen(navController: NavController) {
    var userRole by remember { mutableStateOf("student") }
    var userName by remember { mutableStateOf("User") }
    var userSpecialty by remember { mutableStateOf("") }
    var userLevel by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Stats
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
                userLevel = doc.getString("grade") ?: "Student"
                val subjects = doc.get("subjects") as? List<*>
                userSpecialty = subjects?.firstOrNull()?.toString() ?: ""

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
            .addOnFailureListener { isLoading = false }
    }

    Scaffold(
        bottomBar = {
            Column {
                HorizontalDivider()
                CustomBottomNavigation(navController = navController, userRole = userRole)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(paddingValues)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (userRole == "teacher" || userRole == "administrator") {
                    TeacherHomeScreen(userName, userSpecialty.ifBlank { "Teaching" }, totalStudents, activeGroups, pendingResponses, resourcesShared, navController)
                } else {
                    StudentHomeScreen(userName, userLevel.ifBlank { "Student" }, totalSessions, totalTeachers, navController)
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
    var activeGroups = 0
    var pendingResponses = 0
    var resourcesShared = 0
    val uniqueStudents = mutableSetOf<String>()

    db.collection("groups")
        .whereEqualTo("teacherId", teacherId)
        .get()
        .addOnSuccessListener { groupDocs ->
            activeGroups = groupDocs.size()
            groupDocs.documents.forEach { group ->
                val members = group.get("members") as? List<String> ?: emptyList()
                uniqueStudents.addAll(members) // Handle Unique students from different groups
            }
            val totalStudents = uniqueStudents.size

            db.collection("chats")
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("needsResponse", true)
                .get()
                .addOnSuccessListener { chatDocs ->
                    pendingResponses = chatDocs.size()

                    db.collection("resources")
                        .whereEqualTo("uploaderUid", teacherId)
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
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var showMemoDialog by remember { mutableStateOf(false) }
    var memoList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedMemoId by remember { mutableStateOf<String?>(null) }
    var selectedMemoData by remember { mutableStateOf<Map<String, String>?>(null) }

    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("users").document(uid).collection("personal_memos")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    memoList = snapshot?.documents?.map { doc ->
                        mapOf(
                            "id" to doc.id,
                            "title" to (doc.getString("title") ?: ""),
                            "professor" to (doc.getString("professor") ?: ""),
                            "dateTime" to (doc.getString("dateTime") ?: ""),
                            "location" to (doc.getString("location") ?: "")
                        )
                    } ?: emptyList()
                }
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TeacherHeader(userName, specialty, activeGroups)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TeacherStatCard("Students", totalStudents.toString(), Icons.Default.Person, Color(0xFF3B82F6))
            TeacherStatCard("Pending", pendingResponses.toString(), Icons.Default.Chat, Color(0xFF10B981))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TeacherStatCard("Resources", resourcesShared.toString(), Icons.Default.UploadFile, Color(0xFF8B5CF6))
            TeacherStatCard("Groups", activeGroups.toString(), Icons.Default.Group, Color(0xFFF59E0B))
        }

        // Schedule Section (Identical to Student)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Your Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { selectedMemoId = null; selectedMemoData = null; showMemoDialog = true }) {
                Icon(Icons.Default.Add, "Add New", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (memoList.isNotEmpty()) {
            memoList.forEach { memo ->
                SessionMemoCard(
                    title = memo["title"] as String,
                    professorName = memo["professor"] as String,
                    dateTime = memo["dateTime"] as String,
                    location = memo["location"] as String,
                    onClick = {
                        selectedMemoId = memo["id"] as String
                        selectedMemoData = mapOf("title" to (memo["title"] as String), "professor" to (memo["professor"] as String), "dateTime" to (memo["dateTime"] as String), "location" to (memo["location"] as String))
                        showMemoDialog = true
                    }
                )
            }
        }

        Text("Management Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionChip(Icons.Filled.Announcement, "Discussions", { navController.navigate(Routes.DISCUSSIONS) })
                ActionChip(Icons.Filled.UploadFile, "Upload", { navController.navigate(Routes.UPLOAD) })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionChip(Icons.Filled.Settings, "Settings", { navController.navigate(Routes.SETTINGS) })
                Spacer(modifier = Modifier.weight(1f)) // Balanced dimension fix
            }
        }
    }

    if (showMemoDialog) {
        AddMemoDialog(
            initialTitle = selectedMemoData?.get("title") ?: "",
            initialProf = selectedMemoData?.get("professor") ?: "",
            initialTime = selectedMemoData?.get("dateTime") ?: "",
            initialLoc = selectedMemoData?.get("location") ?: "",
            isEditMode = selectedMemoId != null,
            onDismiss = { showMemoDialog = false },
            onDelete = {
                if (uid != null && selectedMemoId != null) {
                    db.collection("users").document(uid).collection("personal_memos").document(selectedMemoId!!).delete()
                }
                showMemoDialog = false
            },
            onSave = { title, prof, time, loc ->
                if (uid != null) {
                    val data = hashMapOf("title" to title, "professor" to prof, "dateTime" to time, "location" to loc, "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp())
                    val col = db.collection("users").document(uid).collection("personal_memos")
                    if (selectedMemoId != null) col.document(selectedMemoId!!).set(data) else col.add(data)
                }
                showMemoDialog = false
            }
        )
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
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var showMemoDialog by remember { mutableStateOf(false) }

    // Updated to hold a List of Memos instead of just one
    var memoList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedMemoId by remember { mutableStateOf<String?>(null) }
    var selectedMemoData by remember { mutableStateOf<Map<String, String>?>(null) }

    // Fetch ALL personal memos
    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("users").document(uid).collection("personal_memos")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener

                    memoList = snapshot?.documents?.map { doc ->
                        mapOf(
                            "id" to doc.id,
                            "title" to (doc.getString("title") ?: ""),
                            "professor" to (doc.getString("professor") ?: ""),
                            "dateTime" to (doc.getString("dateTime") ?: ""),
                            "location" to (doc.getString("location") ?: "")
                        )
                    } ?: emptyList()
                }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StudentHeader(userName, level, totalSessions)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StudentStatCard("Sessions", totalSessions.toString(), Icons.Default.Event, Color(0xFF3B82F6))
            StudentStatCard("Teachers", totalTeachers.toString(), Icons.Default.Groups, Color(0xFF10B981))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = {
                selectedMemoId = null
                selectedMemoData = null
                showMemoDialog = true
            }) {
                Icon(Icons.Default.Add, "Add New", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (memoList.isNotEmpty()) {
            memoList.forEach { memo ->
                SessionMemoCard(
                    title = memo["title"] as String,
                    professorName = memo["professor"] as String,
                    dateTime = memo["dateTime"] as String,
                    location = memo["location"] as String,
                    onClick = {
                        selectedMemoId = memo["id"] as String
                        selectedMemoData = mapOf(
                            "title" to (memo["title"] as String),
                            "professor" to (memo["professor"] as String),
                            "dateTime" to (memo["dateTime"] as String),
                            "location" to (memo["location"] as String)
                        )
                        showMemoDialog = true
                    }
                )
            }
        } else {
            // Empty state placeholder
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                onClick = { showMemoDialog = true }
            ) {
                Text("No sessions noted. Tap + to add.", modifier = Modifier.padding(20.dp), color = Color.Gray)
            }
        }

        // Quick Actions Section
        Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionChip(Icons.Filled.Announcement, "Discussions", { navController.navigate(Routes.DISCUSSIONS) })
                ActionChip(Icons.Filled.LibraryBooks, "Resources", { navController.navigate(Routes.RESOURCES) })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionChip(Icons.Filled.Settings, "Settings", { navController.navigate(Routes.SETTINGS) })
                Spacer(modifier = Modifier.weight(1f)) // Balanced dimension fix
            }
        }
    }

    if (showMemoDialog) {
        AddMemoDialog(
            initialTitle = selectedMemoData?.get("title") ?: "",
            initialProf = selectedMemoData?.get("professor") ?: "",
            initialTime = selectedMemoData?.get("dateTime") ?: "",
            initialLoc = selectedMemoData?.get("location") ?: "",
            isEditMode = selectedMemoId != null,
            onDismiss = { showMemoDialog = false },
            onDelete = {
                if (uid != null && selectedMemoId != null) {
                    db.collection("users").document(uid).collection("personal_memos")
                        .document(selectedMemoId!!).delete()
                }
                showMemoDialog = false
            },
            onSave = { title, prof, time, loc ->
                if (uid != null) {
                    val data = hashMapOf(
                        "title" to title,
                        "professor" to prof,
                        "dateTime" to time,
                        "location" to loc,
                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    val collection = db.collection("users").document(uid).collection("personal_memos")
                    if (selectedMemoId != null) collection.document(selectedMemoId!!).set(data)
                    else collection.add(data)
                }
                showMemoDialog = false
            }
        )
    }
}

@Composable
fun SessionMemoCard(
    title: String,
    professorName: String,
    dateTime: String,
    location: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FE)),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = "Upcoming",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = professorName, 
                color = Color.Black, 
                style = MaterialTheme.typography.bodyLarge
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, null, modifier = Modifier.size(18.dp), tint = Color(0xFF3B82F6))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateTime, 
                    style = MaterialTheme.typography.bodyMedium, 
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, null, modifier = Modifier.size(18.dp), tint = Color(0xFFE53935))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = location, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun AddMemoDialog(
    initialTitle: String,
    initialProf: String,
    initialTime: String,
    initialLoc: String,
    isEditMode: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var prof by remember { mutableStateOf(initialProf) }
    var time by remember { mutableStateOf(initialTime) }
    var loc by remember { mutableStateOf(initialLoc) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditMode) "Edit Session" else "New Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Subject") })
                OutlinedTextField(value = prof, onValueChange = { prof = it }, label = { Text("Professor") })
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time/Date") })
                OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("Location") })
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, prof, time, loc) }) {
                Text(if (isEditMode) "Update" else "Save")
            }
        },
        dismissButton = {
            Row {
                if (isEditMode) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
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

@Composable
private fun TeacherHeader(userName: String, specialty: String, groupCount: Int) {
    // Exact same Blue -> Indigo -> Purple gradient as StudentHeader
    val unifiedGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF3B82F6), // Blue
            Color(0xFF6366F1), // Indigo
            Color(0xFF8B5CF6), // Purple
            Color(0xFFA855F7)  // Bright Purple
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
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
                .background(unifiedGradient)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                Text(
                    text = "Welcome, $userName!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Role: Teacher",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TransparentStatCard(
                    label = "Specialty",
                    value = specialty.uppercase(),
                    modifier = Modifier.weight(1f)
                )
                TransparentStatCard(
                    label = "Managed Groups",
                    value = "$groupCount Active",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RowScope.TeacherStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .height(80.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Column {
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