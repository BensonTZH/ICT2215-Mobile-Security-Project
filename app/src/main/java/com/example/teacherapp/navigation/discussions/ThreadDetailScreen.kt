package com.example.teacherapp.navigation.discussions
import androidx.compose.foundation.background
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

    val indigo = Color(0xFF6366F1)

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
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Discussion",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    windowInsets = WindowInsets(0.dp),
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = indigo
                    )
                )
            }
        },
        bottomBar = {
            CommentInputBar(onCommentSend = { text ->
                saveComment(threadId, text, currentUserName)
            })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                PostHeader(thread)
                // Thicker visual break between post and comments
                HorizontalDivider(thickness = 8.dp, color = Color(0xFFF0F2F8))
            }

            if (comments.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No comments yet. Be the first to reply!", color = Color.Gray)
                    }
                }
            }

            items(comments) { comment ->
                CommentRow(comment)
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = Color(0xFFEEEEEE)
                )
            }
        }
    }
}

@Composable
fun PostHeader(thread: DiscussionThread?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF6366F1)
            )
            Spacer(modifier = Modifier.width(6.dp))
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
            fontWeight = FontWeight.ExtraBold,
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
                color = Color(0xFF6366F1)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "• Just now",
                fontSize = 12.sp,
                color = Color.LightGray
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = comment.text,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 15.sp,
            color = Color.Black
        )
    }
}
@Composable
fun CommentInputBar(onCommentSend: (String) -> Unit) {
    var textState by remember { mutableStateOf("") }
    val indigo = Color(0xFF6366F1)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 0.dp,
        shadowElevation = 16.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                placeholder = {
                    Text(
                        "Add a comment...",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,

                    unfocusedBorderColor = Color(0xFFF0F0F0),
                    focusedBorderColor = indigo,

                    cursorColor = indigo
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textState.isNotBlank()) {
                        onCommentSend(textState)
                        textState = ""
                    }
                },
                enabled = textState.isNotBlank(),
                modifier = Modifier.background(
                    color = if (textState.isNotBlank()) indigo else Color(0xFFF1F3F4),
                    shape = RoundedCornerShape(50)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (textState.isNotBlank()) Color.White else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

fun saveComment(threadId: String, commentText: String, creatorName: String) {
    val db = FirebaseFirestore.getInstance()

    val newComment = hashMapOf(
        "creatorName" to creatorName,
        "text" to commentText,
        "timestamp" to System.currentTimeMillis()
    )

    db.collection("threads")
        .document(threadId)
        .collection("comments")
        .add(newComment)
}