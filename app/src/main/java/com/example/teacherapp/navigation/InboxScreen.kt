package com.example.teacherapp.navigation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

val InboxRed = Color(0xFFD32F2F)

data class ChatSummary(
    val chatId: String,
    val participants: List<String>,
    val lastMessage: String,
    val lastTimestamp: Timestamp,
    val lastSenderId: String
)

private fun formatTime(ts: Timestamp): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(ts.toDate())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser ?: return

    var chats by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    val nameCache = remember { mutableStateMapOf<String, String>() }
    var searchQuery by remember { mutableStateOf("") }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Listen for chats involving the current user
    LaunchedEffect(currentUser.uid) {
        db.collection("chats")
            .whereArrayContains("participants", currentUser.uid)
            .orderBy("lastTimestamp")
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) {
                    Log.e("InboxScreen", "Chat snapshot error", e)
                    return@addSnapshotListener
                }

                chats = snap.documents.mapNotNull { doc ->
                    val participantsAny = doc.get("participants") as? List<*> ?: return@mapNotNull null
                    val participants = participantsAny.filterIsInstance<String>()
                    if (participants.isEmpty()) return@mapNotNull null

                    val lastTs = doc.getTimestamp("lastTimestamp") ?: Timestamp.now()

                    ChatSummary(
                        chatId = doc.id,
                        participants = participants,
                        lastMessage = doc.getString("lastMessage") ?: "",
                        lastTimestamp = lastTs,
                        lastSenderId = doc.getString("lastSenderId") ?: ""
                    )
                }.sortedByDescending { it.lastTimestamp.toDate().time }
            }
    }

    // Filtered chats based on the search query
    val filteredChats = chats.filter { chat ->
        chat.participants.any { participant ->
            nameCache[participant]?.contains(searchQuery, ignoreCase = true) ?: false
        }
    }

    // Fetch names for the other users
    LaunchedEffect(chats) {
        chats.forEach { chat ->
            val otherUid = chat.participants.firstOrNull { it != currentUser.uid } ?: return@forEach
            if (otherUid.isBlank()) return@forEach

            if (nameCache[otherUid] == null) {
                db.collection("users").document(otherUid).get()
                    .addOnSuccessListener { doc ->
                        nameCache[otherUid] = doc.getString("name") ?: "Unknown"
                    }
                    .addOnFailureListener {
                        nameCache[otherUid] = "Unknown"
                    }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = BackgroundGray,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Inbox",
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
                    containerColor = InboxRed,
                    scrolledContainerColor = InboxRed, // Keeps it red when scrolling
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = { CustomBottomNavigation(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 3D Search Bar (Same style as Discovery)
            SearchBarSection(searchQuery) { searchQuery = it }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // We use chats.filter here directly to ensure names are loaded
                val displayChats = chats.filter { chat ->
                    val otherUid = chat.participants.firstOrNull { it != currentUser.uid } ?: ""
                    val name = nameCache[otherUid] ?: ""
                    name.contains(searchQuery, ignoreCase = true) || searchQuery.isEmpty()
                }

                items(displayChats) { chat ->
                    val otherUid = chat.participants.firstOrNull { it != currentUser.uid } ?: return@items
                    val otherName = nameCache[otherUid] ?: "Loading..."

                    InboxChatCard(
                        name = otherName,
                        lastMessage = chat.lastMessage,
                        time = formatTime(chat.lastTimestamp),
                        onClick = {
                            navController.navigate("${Routes.CHAT}/$otherUid") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InboxChatCard(
    name: String,
    lastMessage: String,
    time: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp) // Small gap between cards
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Icon
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = InboxRed.copy(alpha = 0.1f) // Fixed line
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = InboxRed,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Text(
                        text = time,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastMessage,
                    color = Color.DarkGray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}