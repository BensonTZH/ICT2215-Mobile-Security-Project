//package com.example.teacherapp.navigation
//
//import android.util.Log
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.google.firebase.Timestamp
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.ListenerRegistration
//import com.google.firebase.firestore.Query
//import com.google.firebase.firestore.SetOptions
//import java.text.SimpleDateFormat
//import java.util.*
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MessageScreen(navController: NavController, otherUserId: String) {
//    val db = FirebaseFirestore.getInstance()
//    val auth = FirebaseAuth.getInstance()
//    val currentUser = auth.currentUser ?: return
//
//    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
//    var chatName by remember { mutableStateOf("Loading...") }
//    var chatSubtitle by remember { mutableStateOf("") }
//    var isGroupChat by remember { mutableStateOf(false) }
//    var messageText by remember { mutableStateOf("") }
//    val listState = rememberLazyListState()
//
//    val chatId = chatIdFor(currentUser.uid, otherUserId)
//
//    // Fetch chat info - determine if it's a group or private chat
//    LaunchedEffect(otherUserId, chatId) {
//        // First, check if this is a group chat
//        db.collection("chats").document(chatId).get()
//            .addOnSuccessListener { chatDoc ->
//                if (chatDoc.exists()) {
//                    // Check if it's a group chat
//                    val type = chatDoc.getString("type")
//                    isGroupChat = type == "group"
//
//                    if (isGroupChat) {
//                        // It's a group chat - get group info
//                        chatName = chatDoc.getString("groupName") ?: "Group Chat"
//                        val memberCount = (chatDoc.get("participants") as? List<*>)?.size ?: 0
//                        chatSubtitle = "$memberCount members"
//                    } else {
//                        // It's a private chat - get other user's info
//                        db.collection("users").document(otherUserId).get()
//                            .addOnSuccessListener { userDoc ->
//                                chatName = userDoc.getString("name") ?: "Unknown User"
//                                chatSubtitle = "Active now"
//                            }
//                            .addOnFailureListener { e ->
//                                Log.e("MessageScreen", "Failed to fetch user", e)
//                                chatName = "Unknown User"
//                                chatSubtitle = ""
//                            }
//                    }
//                } else {
//                    // No chat document exists yet - it's a new private chat
//                    isGroupChat = false
//                    db.collection("users").document(otherUserId).get()
//                        .addOnSuccessListener { userDoc ->
//                            chatName = userDoc.getString("name") ?: "Unknown User"
//                            chatSubtitle = "Active now"
//                        }
//                        .addOnFailureListener { e ->
//                            Log.e("MessageScreen", "Failed to fetch user", e)
//                            chatName = "Unknown User"
//                            chatSubtitle = ""
//                        }
//                }
//            }
//            .addOnFailureListener { e ->
//                Log.e("MessageScreen", "Failed to fetch chat", e)
//                // Fallback to private chat
//                isGroupChat = false
//                db.collection("users").document(otherUserId).get()
//                    .addOnSuccessListener { userDoc ->
//                        chatName = userDoc.getString("name") ?: "Unknown User"
//                        chatSubtitle = "Active now"
//                    }
//            }
//    }
//
//    // Real-time message listener
//    DisposableEffect(chatId) {
//        val reg: ListenerRegistration = db.collection("messages")
//            .whereEqualTo("chatId", chatId)
//            .orderBy("timestamp", Query.Direction.ASCENDING)
//            .addSnapshotListener { snapshot, e ->
//                if (e != null) {
//                    Log.e("MessageScreen", "Snapshot error", e)
//                    return@addSnapshotListener
//                }
//
//                if (snapshot != null && !snapshot.isEmpty) {
//                    messages = snapshot.documents.mapNotNull { doc ->
//                        val senderId = doc.getString("senderId") ?: return@mapNotNull null
//                        val recipientId = doc.getString("recipientId") ?: ""
//                        val messageText = doc.getString("message") ?: return@mapNotNull null
//                        val timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now()
//
//                        Message(
//                            message = messageText,
//                            senderId = senderId,
//                            recipientId = recipientId,
//                            timestamp = timestamp,
//                            senderName = doc.getString("senderName") ?: "Unknown"
//                        )
//                    }
//                }
//            }
//
//        onDispose { reg.remove() }
//    }
//
//    // Auto-scroll to bottom when new messages arrive
//    LaunchedEffect(messages.size) {
//        if (messages.isNotEmpty()) {
//            listState.animateScrollToItem(messages.size - 1)
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            ChatTopBar(
//                chatName = chatName,
//                chatSubtitle = chatSubtitle,
//                isGroupChat = isGroupChat,
//                onBackClick = { navController.popBackStack() },
//                onInfoClick = {
//                    // Navigate to group info or user profile
//                    if (isGroupChat) {
//                        // TODO: Navigate to group info screen
//                    } else {
//                        // Navigate to user profile
//                        navController.navigate("${Routes.PUBLIC_PROFILE}/$otherUserId")
//                    }
//                }
//            )
//        },
//        containerColor = Color(0xFFF5F7FA)
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//        ) {
//            // Messages List
//            LazyColumn(
//                state = listState,
//                modifier = Modifier
//                    .weight(1f)
//                    .fillMaxWidth(),
//                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
//                verticalArrangement = Arrangement.spacedBy(4.dp)
//            ) {
//                items(messages) { msg ->
//                    MessageBubble(
//                        message = msg,
//                        isCurrentUser = msg.senderId == currentUser.uid,
//                        showSenderName = isGroupChat && msg.senderId != currentUser.uid
//                    )
//                }
//            }
//
//            // Message Input Bar
//            MessageInputBar(
//                messageText = messageText,
//                onMessageChange = { messageText = it },
//                onSend = {
//                    val text = messageText.trim()
//                    if (text.isNotEmpty()) {
//                        val now = Timestamp.now()
//                        val msgRef = db.collection("messages").document()
//                        val chatRef = db.collection("chats").document(chatId)
//
//                        val msgData = hashMapOf(
//                            "chatId" to chatId,
//                            "message" to text,
//                            "senderId" to currentUser.uid,
//                            "recipientId" to otherUserId,
//                            "senderName" to (auth.currentUser?.displayName ?: "You"),
//                            "participants" to listOf(currentUser.uid, otherUserId),
//                            "timestamp" to now
//                        )
//
//                        val chatData = hashMapOf<String, Any>(
//                            "participants" to listOf(currentUser.uid, otherUserId),
//                            "lastMessage" to text,
//                            "lastTimestamp" to now,
//                            "lastSenderId" to currentUser.uid
//                        )
//
//                        // Add type only if it doesn't exist (for new chats)
//                        if (!isGroupChat) {
//                            chatData["type"] = "private"
//                        }
//
//                        db.runBatch { batch ->
//                            batch.set(msgRef, msgData)
//                            batch.set(chatRef, chatData, SetOptions.merge())
//                        }.addOnSuccessListener {
//                            messageText = ""
//                        }.addOnFailureListener { e ->
//                            Log.e("MessageScreen", "Send failed", e)
//                        }
//                    }
//                }
//            )
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ChatTopBar(
//    chatName: String,
//    chatSubtitle: String,
//    isGroupChat: Boolean,
//    onBackClick: () -> Unit,
//    onInfoClick: () -> Unit
//) {
//    Surface(
//        color = Color.White,
//        shadowElevation = 2.dp
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 8.dp, vertical = 12.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            // Back Button
//            IconButton(onClick = onBackClick) {
//                Icon(
//                    Icons.Default.ArrowBack,
//                    contentDescription = "Back",
//                    tint = Color(0xFF1F2937)
//                )
//            }
//
//            // Avatar
//            Box(
//                modifier = Modifier
//                    .size(48.dp)
//                    .clip(CircleShape)
//                    .background(
//                        Brush.horizontalGradient(
//                            colors = listOf(
//                                Color(0xFF3B82F6),
//                                Color(0xFF8B5CF6)
//                            )
//                        )
//                    ),
//                contentAlignment = Alignment.Center
//            ) {
//                Text(
//                    text = chatName.take(1).uppercase(),
//                    fontSize = 20.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = Color.White
//                )
//            }
//
//            Spacer(modifier = Modifier.width(12.dp))
//
//            // Chat Name and Subtitle
//            Column(modifier = Modifier.weight(1f)) {
//                Text(
//                    text = chatName,
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = Color(0xFF1F2937)
//                )
//                if (chatSubtitle.isNotEmpty()) {
//                    Text(
//                        text = chatSubtitle,
//                        fontSize = 13.sp,
//                        color = if (isGroupChat) Color(0xFF6B7280) else Color(0xFF10B981)
//                    )
//                }
//            }
//
//            // Info Button
//            IconButton(onClick = onInfoClick) {
//                Icon(
//                    if (isGroupChat) Icons.Default.Group else Icons.Default.Person,
//                    contentDescription = if (isGroupChat) "Group Info" else "User Info",
//                    tint = Color(0xFF6B7280)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun MessageBubble(
//    message: Message,
//    isCurrentUser: Boolean,
//    showSenderName: Boolean
//) {
//    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
//    val bubbleColor = if (isCurrentUser) Color(0xFF3B82F6) else Color.White
//    val textColor = if (isCurrentUser) Color.White else Color(0xFF1F2937)
//
//    val bubbleShape = if (isCurrentUser) {
//        RoundedCornerShape(
//            topStart = 20.dp,
//            topEnd = 20.dp,
//            bottomStart = 20.dp,
//            bottomEnd = 4.dp
//        )
//    } else {
//        RoundedCornerShape(
//            topStart = 20.dp,
//            topEnd = 20.dp,
//            bottomStart = 4.dp,
//            bottomEnd = 20.dp
//        )
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 4.dp),
//        horizontalAlignment = alignment
//    ) {
//        // Show sender name for group chats
//        if (showSenderName && !isCurrentUser) {
//            Text(
//                text = message.senderName,
//                fontSize = 12.sp,
//                fontWeight = FontWeight.Medium,
//                color = Color(0xFF6B7280),
//                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
//            )
//        }
//
//        // Message Bubble
//        Surface(
//            shape = bubbleShape,
//            color = bubbleColor,
//            shadowElevation = if (isCurrentUser) 0.dp else 1.dp
//        ) {
//            Text(
//                text = message.message,
//                fontSize = 15.sp,
//                color = textColor,
//                modifier = Modifier
//                    .padding(horizontal = 16.dp, vertical = 10.dp)
//                    .widthIn(max = 280.dp)
//            )
//        }
//
//        // Timestamp
//        Text(
//            text = formatTimestamp(message.timestamp),
//            fontSize = 11.sp,
//            color = Color(0xFF9CA3AF),
//            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
//        )
//    }
//}
//
//@Composable
//fun MessageInputBar(
//    messageText: String,
//    onMessageChange: (String) -> Unit,
//    onSend: () -> Unit
//) {
//    Surface(
//        color = Color.White,
//        shadowElevation = 8.dp
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(12.dp),
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            // Attachment Button
//            IconButton(
//                onClick = { /* Handle attachment */ },
//                modifier = Modifier.size(40.dp)
//            ) {
//                Icon(
//                    Icons.Default.AttachFile,
//                    contentDescription = "Attach",
//                    tint = Color(0xFF6B7280),
//                    modifier = Modifier.size(22.dp)
//                )
//            }
//
//            // Image Button
//            IconButton(
//                onClick = { /* Handle image */ },
//                modifier = Modifier.size(40.dp)
//            ) {
//                Icon(
//                    Icons.Default.Image,
//                    contentDescription = "Image",
//                    tint = Color(0xFF6B7280),
//                    modifier = Modifier.size(22.dp)
//                )
//            }
//
//            // Location Button
//            IconButton(
//                onClick = { /* Handle location */ },
//                modifier = Modifier.size(40.dp)
//            ) {
//                Icon(
//                    Icons.Default.LocationOn,
//                    contentDescription = "Location",
//                    tint = Color(0xFF6B7280),
//                    modifier = Modifier.size(22.dp)
//                )
//            }
//
//            // Message Input Field
//            OutlinedTextField(
//                value = messageText,
//                onValueChange = onMessageChange,
//                modifier = Modifier.weight(1f),
//                placeholder = {
//                    Text(
//                        "Type a message...",
//                        fontSize = 14.sp,
//                        color = Color(0xFF9CA3AF)
//                    )
//                },
//                shape = RoundedCornerShape(24.dp),
//                colors = OutlinedTextFieldDefaults.colors(
//                    focusedBorderColor = Color(0xFFE5E7EB),
//                    unfocusedBorderColor = Color(0xFFE5E7EB),
//                    focusedContainerColor = Color(0xFFF9FAFB),
//                    unfocusedContainerColor = Color(0xFFF9FAFB)
//                ),
//                singleLine = true,
//                textStyle = TextStyle(fontSize = 14.sp)
//            )
//
//            // Send Button
//            IconButton(
//                onClick = onSend,
//                modifier = Modifier
//                    .size(48.dp)
//                    .clip(CircleShape)
//                    .background(Color(0xFF3B82F6)),
//                enabled = messageText.isNotBlank()
//            ) {
//                Icon(
//                    Icons.Default.Send,
//                    contentDescription = "Send",
//                    tint = Color.White,
//                    modifier = Modifier.size(20.dp)
//                )
//            }
//        }
//    }
//}
//
//// Message Data Class
//data class Message(
//    val message: String,
//    val senderId: String,
//    val recipientId: String,
//    val timestamp: Timestamp,
//    val senderName: String = ""
//)
//
//// Helper Functions
//fun formatTimestamp(timestamp: Timestamp): String {
//    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
//    return sdf.format(timestamp.toDate())
//}
//
//fun chatIdFor(uid1: String, uid2: String): String =
//    listOf(uid1, uid2).sorted().joinToString("_")