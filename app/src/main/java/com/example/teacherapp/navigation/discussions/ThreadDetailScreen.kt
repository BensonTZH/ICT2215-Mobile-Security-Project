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
import com.example.teacherapp.models.Comment
import com.example.teacherapp.models.DiscussionThread
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadDetailScreen(navController: NavController, threadId: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var thread by remember { mutableStateOf<DiscussionThread?>(null) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var currentUserName by remember { mutableStateOf("User") }

    LaunchedEffect(threadId) {
        //Fetch current User Name
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                currentUserName = doc.getString("name") ?: "User"
            }
        }

        db.collection("threads").document(threadId).get().addOnSuccessListener { doc ->
            thread = doc.toObject(DiscussionThread::class.java)
        }

        db.collection("threads").document(threadId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                comments = snap?.toObjects(Comment::class.java) ?: emptyList()
            }
    }

    Scaffold(
        bottomBar = {
            CommentInputBar(onCommentSend = { text ->
                saveComment(threadId, text, currentUserName)
            })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                // The "Reddit Post" Header
                PostHeader(thread)
                HorizontalDivider(thickness = 8.dp, color = Color(0xFFF0F2F8))
            }

            items(comments) { comment ->
                CommentRow(comment)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun PostHeader(thread: DiscussionThread?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "u/${thread?.creatorName ?: "Anonymous"}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Text(
                text = " • 2h ago", // You can format actual timestamp here later
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title
        Text(
            text = thread?.title ?: "",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D3243)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Body Content
        Text(
            text = thread?.content ?: "",
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 22.sp,
            color = Color.DarkGray
        )
    }
}

@Composable
fun CommentRow(comment: Comment) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = comment.creatorName,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF505D8A)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "1h ago",
                fontSize = 11.sp,
                color = Color.LightGray
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = comment.text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black
        )
    }
}
@Composable
fun CommentInputBar(onCommentSend: (String) -> Unit) {
    var textState by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                placeholder = { Text("Add a comment...", fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF505D8A),
                    unfocusedBorderColor = Color.LightGray
                )
            )

            IconButton(
                onClick = {
                    if (textState.isNotBlank()) {
                        onCommentSend(textState)
                        textState = ""
                    }
                },
                enabled = textState.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (textState.isNotBlank()) Color(0xFF505D8A) else Color.Gray
                )
            }
        }
    }
}

fun saveComment(threadId: String, commentText: String, creatorName: String) {
    val db = FirebaseFirestore.getInstance()

    val newComment = hashMapOf(
        "creatorName" to creatorName, // Use the passed name instead of hardcoded fallback
        "text" to commentText,
        "timestamp" to System.currentTimeMillis()
    )

    db.collection("threads")
        .document(threadId)
        .collection("comments")
        .add(newComment)
}