//package com.example.teacherapp.navigation
//
//import android.util.Log
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.layout.widthIn
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.BasicTextField
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material3.Button
//import androidx.compose.material3.Divider
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.material3.TopAppBar
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.DisposableEffect
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.input.TextFieldValue
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.google.firebase.Timestamp
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import java.text.SimpleDateFormat
//import java.util.Locale
//import com.google.firebase.firestore.ListenerRegistration
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MessageScreen(navController: NavController, otherUserId: String)
//{
//    //Firebase setup
//    val db = FirebaseFirestore.getInstance()
//    val auth = FirebaseAuth.getInstance()
//    val currentUser = auth.currentUser ?: return
//
//    //Track messages + other user's name
//    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
//    var otherUserName by remember { mutableStateOf("Unknown") }
//    val chatId = chatIdFor(currentUser.uid, otherUserId)
//
//    //Get the other user name
//    LaunchedEffect(otherUserId) {
//        db.collection("users").document(otherUserId).get()
//            .addOnSuccessListener { doc ->
//                otherUserName = doc.getString("name") ?: "Unknown"
//            }
//            .addOnFailureListener { e ->
//                Log.e("MessageScreen", "Failed to fetch other user name", e)
//            }
//    }
//
//
//    // Realtime chat
//    DisposableEffect(chatId) {
//        val reg: ListenerRegistration = db.collection("messages")
//            .whereEqualTo("chatId", chatId)
//            .orderBy("timestamp")
//            .addSnapshotListener { snapshot, e ->
//                if (e != null || snapshot == null) {
//                    Log.e("MessageScreen", "Snapshot error", e)
//                    return@addSnapshotListener
//                }
//
//                messages = snapshot.documents.mapNotNull { doc ->
//                    val senderId = doc.getString("senderId") ?: return@mapNotNull null
//                    val recipientId = doc.getString("recipientId") ?: return@mapNotNull null
//
//                    Message(
//                        message = doc.getString("message") ?: "",
//                        senderId = senderId,
//                        recipientId = recipientId,
//                        timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now()
//                    )
//                }
//            }
//
//        onDispose { reg.remove() }
//    }
//
//
//
//    Scaffold (
//        topBar = {
//            Column {
//                TopAppBar(
//                    title = { Text("Chat with $otherUserName") },
//                    navigationIcon = {
//                        IconButton(onClick = { navController.popBackStack() }) {
//                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
//                        }
//                    }
//                )
//                Divider()
//            }
//        }
//
//    ){ paddingValues ->
//        MessageScreenUI(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues),
//            messages = messages,
//            currentUserId = currentUser.uid,
//            onSend = { text ->
//                val now = Timestamp.now()
//
//                messages = messages + Message(
//                    message = text,
//                    senderId = currentUser.uid,
//                    recipientId = otherUserId,
//                    timestamp = now
//                )
//
//                val msgRef = db.collection("messages").document()
//                val chatRef = db.collection("chats").document(chatId)
//
//                val msgData = mapOf(
//                    "chatId" to chatId,
//                    "message" to text,
//                    "senderId" to currentUser.uid,
//                    "recipientId" to otherUserId,
//                    "participants" to listOf(currentUser.uid, otherUserId),
//                    "timestamp" to now
//                )
//
//                val chatData = mapOf(
//                    "participants" to listOf(currentUser.uid, otherUserId),
//                    "lastMessage" to text,
//                    "lastTimestamp" to now,
//                    "lastSenderId" to currentUser.uid
//                )
//
//                db.runBatch { batch ->
//                    batch.set(msgRef, msgData)
//                    batch.set(chatRef, chatData, com.google.firebase.firestore.SetOptions.merge())
//                }.addOnFailureListener { e ->
//                    Log.e("MessageScreen", "Send failed", e)
//                }
//            }
//
//
//        )
//    }
//
//
//}
//
////message bubble
//@Composable
//fun MessageItem(message: Message, isCurrentUser: Boolean) {
//    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
//    val bubbleColor = if (isCurrentUser) Color(0xFF1A73E8) else Color.White
//    val textColor = if (isCurrentUser) Color.White else Color(0xFF111827)
//    val timeColor = Color(0xFF6B7280)
//
//    // Tail effect: less rounded on the "tail" corner
//    val bubbleShape = if (isCurrentUser) {
//        RoundedCornerShape(
//            topStart = 20.dp,
//            topEnd = 20.dp,
//            bottomStart = 20.dp,
//            bottomEnd = 6.dp
//        )
//    } else {
//        RoundedCornerShape(
//            topStart = 20.dp,
//            topEnd = 20.dp,
//            bottomStart = 6.dp,
//            bottomEnd = 20.dp
//        )
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 12.dp, vertical = 6.dp),
//        horizontalAlignment = alignment
//    ) {
//        // Bubble
//        Surface(
//            shape = bubbleShape,
//            shadowElevation = 2.dp,
//            color = bubbleColor
//        ) {
//            Text(
//                text = message.message,
//                fontSize = 16.sp,
//                color = textColor,
//                modifier = Modifier
//                    .padding(horizontal = 14.dp, vertical = 10.dp)
//                    .widthIn(max = 280.dp) // prevents full-width bubbles
//            )
//        }
//
//        Text(
//            text = formatTimestamp(message.timestamp),
//            fontSize = 12.sp,
//            color = timeColor,
//            modifier = Modifier
//                .padding(top = 4.dp, start = 6.dp, end = 6.dp)
//        )
//    }
//}
//
//
////Message model
//data class Message(
//    val message: String,
//    val senderId: String,
//    val recipientId: String,
//    val timestamp: Timestamp
//)
//
//
//@Composable
//fun MessageScreenUI(
//    modifier: Modifier = Modifier,
//    messages: List<Message>,
//    currentUserId: String,
//    onSend: (String) -> Unit
//) {
//    var messageText by remember { mutableStateOf(TextFieldValue("")) }
//    val listState = rememberLazyListState()
//
//    LaunchedEffect(messages.size) {
//        if (messages.isNotEmpty()) {
//            listState.animateScrollToItem(messages.size - 1)
//        }
//    }
//
//    Column(
//        modifier = modifier.fillMaxSize(),
//        verticalArrangement = Arrangement.SpaceBetween
//    ) {
//        LazyColumn(
//            state = listState,
//            modifier = Modifier
//                .weight(1f)
//                .fillMaxWidth(),
//            contentPadding = PaddingValues(8.dp)
//        ) {
//            items(messages) { msg ->
//                MessageItem(
//                    message = msg,
//                    isCurrentUser = msg.senderId == currentUserId
//                )
//            }
//        }
//        Divider()
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            BasicTextField(
//                value = messageText,
//                onValueChange = { messageText = it },
//                modifier = Modifier
//                    .weight(1f)
//                    .background(Color.LightGray, CircleShape)
//                    .padding(horizontal = 12.dp, vertical = 10.dp),
//                decorationBox = { innerTextField ->
//                    if (messageText.text.isEmpty()) {
//                        Text(
//                            text = "Message",
//                            color = Color.Gray
//                        )
//                    }
//                    innerTextField()
//                }
//            )
//
//
//            Spacer(modifier = Modifier.width(8.dp))
//
//            Button(onClick = {
//                val text = messageText.text.trim()
//                if (text.isNotEmpty()) {
//                    onSend(text)
//                    messageText = TextFieldValue("")
//                }
//            }) {
//                Text("Send")
//            }
//        }
//    }
//}
//
////Timestamp testing
//fun formatTimestamp(timestamp: Timestamp): String {
//    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
//    return sdf.format(timestamp.toDate())
//}
//
//fun chatIdFor(uid1: String, uid2: String): String =
//    listOf(uid1, uid2).sorted().joinToString("_")
//
//
//// just for me to see UI can ignore
//@OptIn(ExperimentalMaterial3Api::class)
//@Preview(showBackground = true, widthDp = 360, heightDp = 720)
//@Composable
//fun MessageScreenWithTopBarPreview() {
//    val mockMessages = listOf(
//        Message("Hi, are you free for consultation?", "student", "teacher", Timestamp.now()),
//        Message("Yes, I am available at 4pm.", "teacher", "student", Timestamp.now()),
//        Message("Great, see you then!", "student", "teacher", Timestamp.now())
//    )
//
//    MaterialTheme {
//        Scaffold(
//            topBar = {
//                androidx.compose.material3.TopAppBar(
//                    title = { Text("Chat with Alice") },
//                    navigationIcon = {
//                        IconButton(onClick = { }) { // no-op in preview
//                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
//                        }
//                    }
//                )
//            }
//        ) { paddingValues ->
//            MessageScreenUI(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(paddingValues),
//                messages = mockMessages,
//                currentUserId = "student",
//                onSend = {}
//            )
//        }
//    }
//}
//
