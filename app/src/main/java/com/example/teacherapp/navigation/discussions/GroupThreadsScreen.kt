package com.example.teacherapp.navigation.discussions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.teacherapp.models.DiscussionThread
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupThreadsScreen(
    navController: NavController,
    groupId: String,   // Receiver for the ID from NavHost
    groupName: String  // Receiver for the Name from NavHost
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val teacherId = auth.currentUser?.uid

    var threads by remember { mutableStateOf<List<DiscussionThread>>(emptyList()) }
    var showCreateThreadDialog by remember { mutableStateOf(false) }
    var currentUserName by remember { mutableStateOf("User") }

    LaunchedEffect(auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                currentUserName = doc.getString("name") ?: "User"
            }
        }
    }

    LaunchedEffect(groupId) {
        db.collection("threads")
            .whereEqualTo("groupId", groupId)
            .addSnapshotListener { snapshot, _ ->
                threads = snapshot?.toObjects(DiscussionThread::class.java) ?: emptyList()
            }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateThreadDialog = true },
                containerColor = Color(0xFF505D8A),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AddComment, null) },
                text = { Text("New Topic") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp)) {
            // Display the actual Group Name passed from the previous screen
            Text(
                text = groupName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (threads.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No discussions yet for this group.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(threads) { thread ->
                        // Renamed helper function call
                        DiscussionThreadCard(thread) {
                            navController.navigate("thread_detail_screen/${thread.id}")
                        }
                    }
                }
            }
        }
    }

    if (showCreateThreadDialog) {
        DiscussionCreateThreadDialog(
            onDismiss = { showCreateThreadDialog = false },
            onCreate = { title, content ->
                val newId = db.collection("threads").document().id
                val newThread = DiscussionThread(
                    id = newId,
                    title = title,
                    content = content,
                    creatorName = currentUserName,
                    teacherId = teacherId ?: "",
                    groupId = groupId // This now works because groupId is in the function header!
                )
                db.collection("threads").document(newId).set(newThread)
                showCreateThreadDialog = false
            }
        )
    }
}
@Composable
fun DiscussionCreateThreadDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var topicTitle by remember { mutableStateOf("") }
    var topicContent by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Text("Start a Discussion", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = topicTitle,
                    onValueChange = { topicTitle = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = topicContent,
                    onValueChange = { topicContent = it },
                    label = { Text("Details") },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onCreate(topicTitle, topicContent) }) { Text("Post") }
                }
            }
        }
    }
}

@Composable
fun DiscussionThreadCard(thread: DiscussionThread, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(thread.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(thread.content, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Color.Gray)
        }
    }
}