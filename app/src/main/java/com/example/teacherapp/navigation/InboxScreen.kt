package com.example.teacherapp.navigation

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

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
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Inbox") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                Divider()
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )


            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(filteredChats) { chat ->
                    val otherUid = chat.participants.firstOrNull { it != currentUser.uid } ?: return@items
                    val otherName = nameCache[otherUid] ?: "Loading..."

                    //Chat card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (otherUid.isNotBlank()) {
                                    navController.navigate("${Routes.CHAT}/$otherUid") {
                                        launchSingleTop = true
                                    }
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = otherName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = chat.lastMessage,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = formatTime(chat.lastTimestamp),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
