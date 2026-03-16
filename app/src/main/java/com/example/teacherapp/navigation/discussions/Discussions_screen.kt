package com.example.teacherapp.navigation.discussions

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
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
import com.example.teacherapp.navigation.CustomBottomNavigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()

    // --- FIX: reactive UID (works after login / app restart) ---
    var userId by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser?.uid) }
    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { fa ->
            userId = fa.currentUser?.uid
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var userRole by remember { mutableStateOf<String?>(null) } // null = loading
    var isLoading by remember { mutableStateOf(true) }
    var showJoinDialog by remember { mutableStateOf(false) }

    fun isPrivileged(role: String?): Boolean = role == "teacher" || role == "administrator"

    LaunchedEffect(userId) {
        groups = emptyList()
        userRole = null
        isLoading = true

        val uid = userId
        if (uid == null) {
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                userRole = doc.getString("role") ?: "student"

                val query = if (isPrivileged(userRole)) {
                    db.collection("groups").whereEqualTo("teacherId", uid)
                } else {
                    db.collection("groups").whereArrayContains("members", uid)
                }

                query.addSnapshotListener { snapshot, err ->
                    if (err != null) {
                        Log.e("DiscussionScreen", "groups listener error", err)
                        groups = emptyList()
                        isLoading = false
                        return@addSnapshotListener
                    }
                    groups = snapshot?.toObjects(Group::class.java) ?: emptyList()
                    isLoading = false
                }
            }
            .addOnFailureListener { e ->
                Log.e("DiscussionScreen", "failed to read user role", e)
                userRole = "student"
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Discussion") },
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
                CustomBottomNavigation(navController, userRole=userRole)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Only students join groups
                if (userRole == "student") {
                    TextButton(onClick = { showJoinDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Join Group")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF505D8A))
                    }
                }

                groups.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No groups available.", color = Color.Gray)
                    }
                }

                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(groups) { group ->
                            GroupCard(group = group) {
                                navController.navigate("group_threads/${group.id}/${group.name}")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showJoinDialog) {
        JoinGroupDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { inviteCode ->
                joinGroupWithCode(db, userId, inviteCode) { showJoinDialog = false }
            }
        )
    }
}

@Composable
private fun GroupCard(group: Group, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF505D8A))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = group.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = "Click to view discussions", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun JoinGroupDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Group") },
        text = {
            Column {
                Text("Enter the invite code shared by your teacher.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it },
                    label = { Text("6-Digit Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (code.length == 6) onJoin(code) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF505D8A))
            ) { Text("Join") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun joinGroupWithCode(
    db: FirebaseFirestore,
    userId: String?,
    code: String,
    onComplete: () -> Unit
) {
    if (userId == null) return

    db.collection("groups")
        .whereEqualTo("inviteCode", code)
        .get()
        .addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty) {
                val groupDoc = snapshot.documents[0]
                groupDoc.reference
                    .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                    .addOnSuccessListener { onComplete() }
            } else {
                Log.e("JoinGroup", "Invalid Invite Code")
            }
        }
        .addOnFailureListener { e ->
            Log.e("JoinGroup", "Join failed", e)
        }
}