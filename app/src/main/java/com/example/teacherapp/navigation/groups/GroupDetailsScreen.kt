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
    var memberNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAddMemberSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val indigo = Color(0xFF6366F1)

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
                    memberNames = snapshot.documents.map { it.getString("name") ?: "Unknown" }
                }
        } else {
            memberNames = emptyList()
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
            // Group Info Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F8))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Invite Code: ${group?.inviteCode}", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Members", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { showAddMemberSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF505D8A))
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Member List
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(memberNames) { name ->
                    ListItem(
                        headlineContent = { Text(name) },
                        leadingContent = { Icon(Icons.Default.AccountCircle, contentDescription = null) }
                    )
                }
            }
        }
    }

    if (showAddMemberSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddMemberSheet = false },
            sheetState = sheetState
        ) {
            StudentSelectorModal(
                onAddMembers = { selectedIds ->
                    // Update Firestore using arrayUnion so we don't overwrite existing members
                    db.collection("groups").document(groupId)
                        .update("members", FieldValue.arrayUnion(*selectedIds.toTypedArray()))
                        .addOnSuccessListener {
                            showAddMemberSheet = false
                        }
                }
            )
        }
    }
}