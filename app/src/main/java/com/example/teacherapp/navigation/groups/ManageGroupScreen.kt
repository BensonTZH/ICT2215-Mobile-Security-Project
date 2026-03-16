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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageGroupsScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val teacherId = auth.currentUser?.uid

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
                    title = { Text("Manage Groups") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
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
                            ManageGroupCard(group) {
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
    onAddMembers: (List<String>) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var students by remember { mutableStateOf<List<StudentUser>>(emptyList()) }
    val selectedIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("role", "student")
            .get()
            .addOnSuccessListener { snapshot ->
                students = snapshot.toObjects(StudentUser::class.java)
            }
    }

    Column(modifier = Modifier.fillMaxHeight(0.8f).padding(16.dp)) {

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(students) { student ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (selectedIds.contains(student.uid)) selectedIds.remove(student.uid)
                        else selectedIds.add(student.uid)
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedIds.contains(student.uid),
                        onCheckedChange = null
                    )
                    Text(student.name, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Button(
            onClick = { onAddMembers(selectedIds.toList()) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF505D8A))
        ) {
            Text("Add Selected (${selectedIds.size})")
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
fun ManageGroupCard(group: Group, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = "Group",
                tint = Color(0xFF505D8A),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Invite: ${group.inviteCode}", color = Color.Gray, fontSize = 13.sp)
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Go",
                tint = Color.Gray
            )
        }
    }
}