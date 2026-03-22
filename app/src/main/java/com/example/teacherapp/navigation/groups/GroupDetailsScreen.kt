package com.example.teacherapp.navigation.groups

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
import androidx.navigation.NavController
import com.example.teacherapp.models.Group
import com.example.teacherapp.navigation.groups.StudentSelectorModal
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(navController: NavController, groupId: String) {
    val db = FirebaseFirestore.getInstance()
    var group by remember { mutableStateOf<Group?>(null) }
    var membersData by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showAddMemberSheet by remember { mutableStateOf(false) }
    var showRemoveMemberSheet by remember { mutableStateOf(false) }


    val sheetState = rememberModalBottomSheetState()
    val indigo = Color(0xFF6366F1)
    val softRed = Color(0xFFEF4444)

    // 1. Fetch Group Details
    LaunchedEffect(groupId) {
        db.collection("groups").document(groupId).addSnapshotListener { snapshot, _ ->
            group = snapshot?.toObject(Group::class.java)
        }
    }

    LaunchedEffect(group?.members) {
        val ids = group?.members ?: emptyList()
        if (ids.isNotEmpty()) {
            db.collection("users").whereIn("uid", ids).get()
                .addOnSuccessListener { snapshot ->
                    membersData = snapshot.documents.map {
                        it.id to (it.getString("name") ?: "Unknown")
                    }
                }
        } else {
            membersData = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        group?.name ?: "Loading...",
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
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp)) {
            // Group Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F8))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Invite Code: ${group?.inviteCode}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Members", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { showAddMemberSheet = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = indigo)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { showRemoveMemberSheet = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = softRed),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Remove")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Member List with Navigation
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(membersData) { (id, name) ->
                    Card(
                        onClick = { navController.navigate("public_profile_screen/$id") },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        ListItem(
                            headlineContent = { Text(name, fontWeight = FontWeight.Medium) },
                            leadingContent = {
                                Icon(Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = indigo,
                                    modifier = Modifier.size(40.dp))
                            },
                            trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }

    if (showAddMemberSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddMemberSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = indigo) }
        ) {
            StudentSelectorModal(
                existingMemberIds = group?.members ?: emptyList(),
                onAddMembers = { selectedIds ->
                    db.collection("groups").document(groupId)
                        .update("members", FieldValue.arrayUnion(*selectedIds.toTypedArray()))
                        .addOnSuccessListener {
                            showAddMemberSheet = false
                        }
                }
            )
        }
    }
    if (showRemoveMemberSheet) {
        ModalBottomSheet(onDismissRequest = { showRemoveMemberSheet = false }) {
            StudentRemovalModal(
                currentMembers = membersData,
                onRemoveMembers = { selectedIds ->
                    db.collection("groups").document(groupId)
                        .update("members", FieldValue.arrayRemove(*selectedIds.toTypedArray()))
                        .addOnSuccessListener { showRemoveMemberSheet = false }
                }
            )
        }
    }
}

@Composable
fun StudentRemovalModal(
    currentMembers: List<Pair<String, String>>, // List of ID to Name
    onRemoveMembers: (List<String>) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<String>() }
    val softRed = Color(0xFFEF4444)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .padding(24.dp)
    ) {
        Text("Remove Members", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Select the students you wish to remove from this group.", color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(currentMembers) { (id, name) ->
                val isSelected = selectedIds.contains(id)
                Surface(
                    onClick = { if (isSelected) selectedIds.remove(id) else selectedIds.add(id) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFFFEF2F2) else Color(0xFFF9FAFB),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) softRed else Color.LightGray)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = if (isSelected) softRed else Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(name, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = softRed)
                        )
                    }
                }
            }
        }

        Button(
            onClick = { onRemoveMembers(selectedIds.toList()) },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = softRed),
            enabled = selectedIds.isNotEmpty(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Remove Selected (${selectedIds.size})", fontWeight = FontWeight.Bold)
        }
    }
}