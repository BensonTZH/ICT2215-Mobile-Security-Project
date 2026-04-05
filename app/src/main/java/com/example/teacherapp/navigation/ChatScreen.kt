package com.example.teacherapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

data class ChatItem(
    val id: String = "",
    val type: String = "", 
    val name: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0L,
    val unreadCount: Int = 0,
    val avatarInitial: String = "",
    val teacherId: String = "", 
    val groupId: String = "" 
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(navController: NavController) {
    var userRole by remember { mutableStateOf("student") }
    var chats by remember { mutableStateOf(listOf<ChatItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    
    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    userRole = document.getString("role") ?: "student"
                }
                .addOnFailureListener {
                    userRole = "student"
                }
        } else {
            isLoading = false
        }
    }

    
    LaunchedEffect(userId, userRole) {
        if (userId != null && userRole.isNotEmpty()) {
            try {
                
                db.collection("conversations")
                    .whereArrayContains("participants", userId)
                    .get()
                    .addOnSuccessListener { conversationSnapshot ->
                        val privateChats = mutableListOf<ChatItem>()
                        val conversationDocs = conversationSnapshot.documents

                        if (conversationDocs.isEmpty()) {
                            
                            loadGroups(db, userId, userRole) { groupChats ->
                                chats = groupChats.sortedByDescending { it.timestamp }
                                isLoading = false
                            }
                        } else {
                            var processedCount = 0
                            conversationDocs.forEach { doc ->
                                val participants = (doc.get("participants") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                                val otherUserId = participants.firstOrNull { it != userId }

                                if (otherUserId != null) {
                                    
                                    db.collection("users").document(otherUserId).get()
                                        .addOnSuccessListener { userDoc ->
                                            val otherUserName = userDoc.getString("name") ?: "User"
                                            val otherUserRole = userDoc.getString("role") ?: "student"

                                            
                                            
                                            val shouldShow = if (userRole == "teacher") {
                                                otherUserRole == "student"
                                            } else {
                                                otherUserRole == "teacher"
                                            }

                                            if (shouldShow) {
                                                val chatItem = ChatItem(
                                                    id = doc.id,
                                                    type = "private",
                                                    name = otherUserName,
                                                    lastMessage = doc.getString("lastMessage") ?: "No messages yet",
                                                    timestamp = doc.getLong("timestamp") ?: 0L,
                                                    unreadCount = 0,
                                                    avatarInitial = otherUserName.firstOrNull()?.toString() ?: "U",
                                                    teacherId = otherUserId
                                                )
                                                privateChats.add(chatItem)
                                            }

                                            processedCount++
                                            if (processedCount == conversationDocs.size) {
                                                
                                                loadGroups(db, userId, userRole) { groupChats ->
                                                    chats = (privateChats + groupChats).sortedByDescending { it.timestamp }
                                                    isLoading = false
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                            processedCount++
                                            if (processedCount == conversationDocs.size) {
                                                loadGroups(db, userId, userRole) { groupChats ->
                                                    chats = (privateChats + groupChats).sortedByDescending { it.timestamp }
                                                    isLoading = false
                                                }
                                            }
                                        }
                                } else {
                                    processedCount++
                                    if (processedCount == conversationDocs.size) {
                                        loadGroups(db, userId, userRole) { groupChats ->
                                            chats = (privateChats + groupChats).sortedByDescending { it.timestamp }
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        loadGroups(db, userId, userRole) { groupChats ->
                            chats = groupChats
                            isLoading = false
                        }
                    }
            } catch (e: Exception) {
                isLoading = false
            }
        } else if (userId != null) {
            isLoading = false
        }
    }

    val filteredChats = when (selectedTab) {
        1 -> chats.filter { it.type == "private" }
        2 -> chats.filter { it.type == "group" }
        else -> chats
    }

    Scaffold(
        bottomBar = {
            Column {
                Divider()
                CustomBottomNavigation(navController)
            }
        },

                topBar = {
            TopAppBar(
                title = { Text("Chats") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6366F1),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (userRole == "teacher" && selectedTab != 0) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == 1) {
                            
                            navController.navigate("find_student")
                        } else if (selectedTab == 2) {
                            
                            showCreateGroupDialog = true
                        }
                    },
                    containerColor = Color(0xFF6366F1)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = if (selectedTab == 1) "Message Student" else "Create Group",
                        tint = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF6366F1)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Private") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Groups") }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (filteredChats.isEmpty()) {
                    EmptyChatsState(userRole, navController)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filteredChats) { chat ->
                            ChatItemCard(
                                chat = chat,
                                onClick = {
                                    if (chat.type == "private") {
                                        navController.navigate("private_chat/${chat.teacherId}")
                                    } else {
                                        navController.navigate("group_chat/${chat.groupId}")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        
        if (showCreateGroupDialog && userRole == "teacher" && userId != null) {
            CreateGroupDialog(
                userId = userId,
                db = db,
                onDismiss = { showCreateGroupDialog = false },
                onGroupCreated = { groupId ->
                    showCreateGroupDialog = false
                    navController.navigate("group_details/$groupId")
                }
            )
        }
    }
}

@Composable
fun CreateGroupDialog(
    userId: String,
    db: FirebaseFirestore,
    onDismiss: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf((100000..999999).random().toString()) }
    var teacherName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    
    LaunchedEffect(userId) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                teacherName = doc.getString("name") ?: "Teacher"
            }
    }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Create New Group", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g., Math Study Group") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it },
                    label = { Text("Invite Code") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating,
                    trailingIcon = {
                        IconButton(
                            onClick = { inviteCode = (100000..999999).random().toString() },
                            enabled = !isCreating
                        ) {
                            Icon(Icons.Default.Refresh, "Regenerate Code")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Students can join using this code",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank() && !isCreating) {
                        isCreating = true
                        val newDocRef = db.collection("groups").document()

                        val newGroup = hashMapOf(
                            "id" to newDocRef.id,
                            "name" to groupName.trim(),
                            "inviteCode" to inviteCode.trim(),
                            "teacherId" to userId,
                            "teacherName" to teacherName,
                            "members" to emptyList<String>(),
                            "createdAt" to System.currentTimeMillis(),
                            "lastMessage" to "",
                            "lastMessageTime" to 0L
                        )

                        newDocRef.set(newGroup)
                            .addOnSuccessListener {
                                onGroupCreated(newDocRef.id)
                            }
                            .addOnFailureListener {
                                isCreating = false
                            }
                    }
                },
                enabled = groupName.isNotBlank() && !isCreating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

private fun loadGroups(
    db: FirebaseFirestore,
    userId: String,
    userRole: String,
    onComplete: (List<ChatItem>) -> Unit
) {
    val query = if (userRole == "teacher") {
        
        db.collection("groups").whereEqualTo("teacherId", userId)
    } else {
        
        db.collection("groups").whereArrayContains("members", userId)
    }

    query.get()
        .addOnSuccessListener { groupSnapshot ->
            val groupChats = groupSnapshot.documents.mapNotNull { doc ->
                try {
                    ChatItem(
                        id = doc.id,
                        type = "group",
                        name = doc.getString("name") ?: "Study Group",
                        lastMessage = doc.getString("lastMessage") ?: "No messages yet",
                        timestamp = doc.getLong("lastMessageTime") ?: 0L,
                        unreadCount = 0,
                        avatarInitial = (doc.getString("name") ?: "G").firstOrNull()?.toString() ?: "G",
                        groupId = doc.id
                    )
                } catch (e: Exception) {
                    null
                }
            }
            onComplete(groupChats)
        }
        .addOnFailureListener {
            onComplete(emptyList())
        }
}

@Composable
fun ChatItemCard(
    chat: ChatItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (chat.type == "private") Color(0xFF10B981) else Color(0xFF6366F1)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (chat.type == "private") {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )

                    if (chat.timestamp > 0) {
                        Text(
                            text = formatTimestamp(chat.timestamp),
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage,
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (chat.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444)
                        ) {
                            Text(
                                text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatsState(userRole: String, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubble,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFD1D5DB)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Chats Yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B7280)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (userRole == "teacher")
                "Start chatting with students or create groups"
            else
                "Start chatting with teachers or join groups",
            fontSize = 14.sp,
            color = Color(0xFF9CA3AF)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (userRole == "teacher") {
                    navController.navigate("find_student")
                } else {
                    navController.navigate("discovery_screen")
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6366F1)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (userRole == "teacher") "Find Students" else "Find Teachers")
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}

