package com.example.teacherapp.navigation

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    val nameCache = remember { mutableStateMapOf<String, String>() } // uid -> name

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
        topBar = { TopAppBar(title = { Text("Inbox") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(chats) { chat ->
                val otherUid = chat.participants.firstOrNull { it != currentUser.uid } ?: return@items
                val otherName = nameCache[otherUid] ?: "Loading..."

                ListItem(
                    headlineContent = { Text(otherName) },
                    supportingContent = { Text(chat.lastMessage) },
                    trailingContent = { Text(formatTime(chat.lastTimestamp)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (otherUid.isNotBlank()) {
                                navController.navigate("${Routes.CHAT}/$otherUid") {
                                    launchSingleTop = true
                                }
                            }
                        }
                )
                Divider()
            }
        }
    }
}
