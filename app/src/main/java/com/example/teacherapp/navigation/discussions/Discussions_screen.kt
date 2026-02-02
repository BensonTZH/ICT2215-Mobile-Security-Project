package com.example.teacherapp.navigation.discussions
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun DiscussionScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid

    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var userRole by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("users").document(userId).get().addOnSuccessListener { doc ->
                userRole = doc.getString("role") ?: "student"

                val groupQuery = if (userRole == "teacher") {
                    db.collection("groups").whereEqualTo("teacherId", userId)
                } else {
                    db.collection("groups").whereArrayContains("members", userId)
                }

                groupQuery.addSnapshotListener { snapshot, _ ->
                    groups = snapshot?.toObjects(Group::class.java) ?: emptyList()
                    isLoading = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            text = "Your Discussion Groups",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (groups.isEmpty()) {
            Text("You haven't joined any groups yet.", color = Color.Gray)
        } else {
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

@Composable
fun GroupCard(group: Group, onClick: () -> Unit) {
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