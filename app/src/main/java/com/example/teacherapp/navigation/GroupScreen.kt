package com.example.teacherapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Group(
    val id: String = "",
    val name: String = "",
    val subject: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val members: List<String> = emptyList(),
    val description: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(navController: NavController) {
    var userRole by remember { mutableStateOf("") }
    var groups by remember { mutableStateOf(listOf<Group>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    LaunchedEffect(userId) {
        if (userId != null) {
            // Get user role
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    userRole = document.getString("role") ?: "student"

                    // Fetch groups based on role
                    if (userRole == "teacher") {
                        // Teachers see groups they created
                        db.collection("groups")
                            .whereEqualTo("teacherId", userId)
                            .addSnapshotListener { snapshot, _ ->
                                if (snapshot != null) {
                                    groups = snapshot.documents.mapNotNull { doc ->
                                        Group(
                                            id = doc.id,
                                            name = doc.getString("name") ?: "",
                                            subject = doc.getString("subject") ?: "",
                                            teacherId = doc.getString("teacherId") ?: "",
                                            teacherName = doc.getString("teacherName") ?: "",
                                            members = (doc.get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                            description = doc.getString("description") ?: ""
                                        )
                                    }
                                    isLoading = false
                                }
                            }
                    } else {
                        // Students see groups they're members of
                        db.collection("groups")
                            .whereArrayContains("members", userId)
                            .addSnapshotListener { snapshot, _ ->
                                if (snapshot != null) {
                                    groups = snapshot.documents.mapNotNull { doc ->
                                        Group(
                                            id = doc.id,
                                            name = doc.getString("name") ?: "",
                                            subject = doc.getString("subject") ?: "",
                                            teacherId = doc.getString("teacherId") ?: "",
                                            teacherName = doc.getString("teacherName") ?: "",
                                            members = (doc.get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                            description = doc.getString("description") ?: ""
                                        )
                                    }
                                    isLoading = false
                                }
                            }
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (userRole == "teacher") "Manage Groups" else "My Groups"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6366F1),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            GroupsBottomNavigationBar(navController)
        },
        floatingActionButton = {
            if (userRole == "teacher") {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = Color(0xFF6366F1)
                ) {
                    Icon(Icons.Default.Add, "Create Group", tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                if (groups.isEmpty()) {
                    EmptyGroupsState(userRole, navController)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(groups) { group ->
                            GroupCard(
                                group = group,
                                userRole = userRole,
                                onClick = {
                                    navController.navigate("group_details/${group.id}")
                                }
                            )
                        }
                    }
                }
            }
        }

        // Create Group Dialog
        if (showCreateDialog && userRole == "teacher" && userId != null) {
            CreateGroupDialogSimple(
                userId = userId,
                db = db,
                onDismiss = { showCreateDialog = false },
                onGroupCreated = { groupId ->
                    showCreateDialog = false
                    navController.navigate("group_details/$groupId")
                }
            )
        }
    }
}

@Composable
fun CreateGroupDialogSimple(
    userId: String,
    db: FirebaseFirestore,
    onDismiss: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf((100000..999999).random().toString()) }
    var teacherName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    // Get teacher name
    LaunchedEffect(userId) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                teacherName = doc.getString("name") ?: "Teacher"
            }
    }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Create New Group", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g., Math Study Group") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it },
                    label = { Text("Invite Code") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating,
                    trailingIcon = {
                        IconButton(
                            onClick = { inviteCode = (100000..999999).random().toString() },
                            enabled = !isCreating
                        ) {
                            Icon(Icons.Default.Refresh, "Regenerate")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank() && !isCreating) {
                        isCreating = true
                        val newDocRef = db.collection("groups").document()

                        val newGroup = hashMapOf(
                            "id" to newDocRef.id,
                            "name" to groupName.trim(),
                            "inviteCode" to inviteCode.trim(),
                            "teacherId" to userId,
                            "teacherName" to teacherName,
                            "members" to emptyList<String>(),
                            "createdAt" to System.currentTimeMillis(),
                            "lastMessage" to "",
                            "lastMessageTime" to 0L
                        )

                        newDocRef.set(newGroup)
                            .addOnSuccessListener {
                                onGroupCreated(newDocRef.id)
                            }
                            .addOnFailureListener {
                                isCreating = false
                            }
                    }
                },
                enabled = groupName.isNotBlank() && !isCreating,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun GroupCard(
    group: Group,
    userRole: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = group.subject,
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280)
                        )
                    }

                    if (userRole == "student") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = group.teacherName,
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }

                // Member count badge
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF6366F1).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = group.members.size.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1)
                        )
                    }
                }
            }

            if (group.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = group.description,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun EmptyGroupsState(userRole: String, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFD1D5DB)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (userRole == "teacher") "No Groups Yet" else "Not in Any Groups",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B7280)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (userRole == "teacher")
                "Create your first group to start teaching"
            else
                "Join groups to start learning",
            fontSize = 14.sp,
            color = Color(0xFF9CA3AF)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (userRole == "teacher") {
            Button(
                onClick = { navController.navigate("create_group") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Group")
            }
        } else {
            Button(
                onClick = { navController.navigate("discovery_screen") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Find Teachers")
            }
        }
    }
}

@Composable
fun GroupsBottomNavigationBar(navController: NavController) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("main_screen") },
            icon = { Icon(Icons.Default.Home, "Home") },
            label = { Text("Home") }
        )

        NavigationBarItem(
            selected = true, // Groups is selected for teachers
            onClick = { navController.navigate("groups_screen") },
            icon = { Icon(Icons.Default.Groups, "Groups") },
            label = { Text("Groups") }
        )

        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("chats_screen") },
            icon = { Icon(Icons.Default.Message, "Chats") },
            label = { Text("Chats") }
        )

        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("alerts_screen") },
            icon = { Icon(Icons.Default.Notifications, "Alerts") },
            label = { Text("Alerts") }
        )

        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("profile_screen") },
            icon = { Icon(Icons.Default.Person, "Profile") },
            label = { Text("Profile") }
        )
    }
}