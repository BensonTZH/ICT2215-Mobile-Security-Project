package com.example.teacherapp.navigation.groups
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.teacherapp.navigation.BotNavBar
import com.example.teacherapp.models.Group
import com.example.teacherapp.models.StudentUser
import com.example.teacherapp.navigation.CustomBottomNavigation
import com.example.teacherapp.navigation.EducationBlue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageGroupsScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val teacherId = auth.currentUser?.uid
    val indigo = Color(0xFF6366F1)

    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(teacherId) {
        if (teacherId != null) {
            db.collection("groups")
                .whereEqualTo("teacherId", teacherId)
                .addSnapshotListener { snapshot, _ ->
                    groups = snapshot?.toObjects(Group::class.java) ?: emptyList()
                }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Manage Groups",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    windowInsets = WindowInsets(0.dp),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = indigo,
                        scrolledContainerColor = indigo,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                )
                Divider()
            }
        },
        bottomBar = {
            Column {
                Divider()
                // Only teacher can access this screen
                CustomBottomNavigation(navController, userRole = "teacher")
            }
        }
    ){ innerPadding ->
        Scaffold(
            modifier = Modifier.padding(innerPadding),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = Color(0xFF505D8A),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Group")
                }
            },
            // This ensures the FAB doesn't overlap with the Bottom Bar
            floatingActionButtonPosition = FabPosition.End
        ) { scaffoldPadding ->
            // 3. Main Content
            Column(
                modifier = Modifier
                    .padding(scaffoldPadding)
                    .padding(horizontal = 20.dp)
                    .fillMaxSize()
            ) {
                if (groups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No groups created yet. Tap + to start.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(groups) { group ->
                            ManageGroupCard(group, themeColor = indigo) {
                                if (group.id.isNotEmpty()) {
                                    navController.navigate("group_details/${group.id}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name,code ->
                val newDocRef = db.collection("groups").document()

                val newGroup = Group(
                    id = newDocRef.id,
                    name = name,
                    inviteCode = code,
                    teacherId = teacherId ?: ""
                )

                newDocRef.set(newGroup)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Group created with ID: ${newDocRef.id}")
                        showCreateDialog = false
                    }
            }
        )
    }
}

@Composable
fun StudentSelectorModal(
    existingMemberIds: List<String>, // Added this parameter
    onAddMembers: (List<String>) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var allStudents by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    val selectedIds = remember { mutableStateListOf<String>() }
    val indigo = Color(0xFF6366F1)

    // Fetch all students and filter out existing members
    LaunchedEffect(existingMemberIds) {
        db.collection("users")
            .whereEqualTo("role", "student") // Assuming you have a role field
            .get()
            .addOnSuccessListener { snapshot ->
                allStudents = snapshot.documents
                    .map { it.id to (it.getString("name") ?: "Unknown") }
                    .filter { (id, _) -> !existingMemberIds.contains(id) } // The Filter Logic
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f) // Modal takes up 70% of screen
            .padding(24.dp)
    ) {
        Text(
            text = "Add Students",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937)
        )

        Text(
            text = "Select students to invite to this group",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Student List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(allStudents) { (id, name) ->
                val isSelected = selectedIds.contains(id)

                Surface(
                    onClick = {
                        if (isSelected) selectedIds.remove(id) else selectedIds.add(id)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF9FAFB),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) indigo else Color(0xFFE5E7EB)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = if (isSelected) indigo else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            modifier = Modifier.weight(1f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null, // Handled by Surface onClick
                            colors = CheckboxDefaults.colors(checkedColor = indigo)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Button
        Button(
            onClick = { onAddMembers(selectedIds.toList()) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = selectedIds.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = indigo)
        ) {
            Text("Add ${selectedIds.size} Students", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("JC") }
    var inviteCode by remember { mutableStateOf((100000..999999).random().toString()) }
    val levels = listOf("JC", "Poly", "Secondary")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Create New Group", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it },
                    label = { Text("Invite Code") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { inviteCode = (100000..999999).random().toString() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (groupName.isNotBlank()) onCreate(groupName, inviteCode) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF505D8A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageGroupCard(group: Group, themeColor: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp), // Tiny horizontal padding helps shadow rendering
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FE)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container - Using the Dark Green themeColor
            Surface(
                color = themeColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.padding(10.dp).size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Invite: ${group.inviteCode}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}