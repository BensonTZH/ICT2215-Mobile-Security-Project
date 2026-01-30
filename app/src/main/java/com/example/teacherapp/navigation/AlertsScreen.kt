package com.example.teacherapp.navigation

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.concurrent.TimeUnit

data class Announcement(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val authorName: String = "",
    val authorId: String = "",
    val pinned: Boolean = false,
    val createdAt: Timestamp? = null
)

private enum class AlertsTab(val label: String) {
    ALL("All"),
    PINNED("Pinned"),
    UNREAD("Unread")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    var userRole by remember { mutableStateOf("student") }
    var userName by remember { mutableStateOf("User") }

    var selectedTab by remember { mutableStateOf(AlertsTab.ALL) }

    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var readIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var isLoading by remember { mutableStateOf(true) }

    // Teacher CRUD dialogs
    var showAddDialog by remember { mutableStateOf(false) }
    var deletingAnnouncement by remember { mutableStateOf<Announcement?>(null) }

    // 1) Fetch current user role + name
    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                userRole = doc.getString("role") ?: "student"
                userName = doc.getString("name") ?: "User"
            }
    }

    // 2) Listen to announcements collection
    DisposableEffect(Unit) {
        var reg: ListenerRegistration? = null
        reg = db.collection("announcements")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    announcements = snapshot.documents.map { d ->
                        Announcement(
                            id = d.id,
                            title = d.getString("title") ?: "",
                            body = d.getString("body") ?: "",
                            authorName = d.getString("authorName") ?: "",
                            authorId = d.getString("authorId") ?: "",
                            pinned = d.getBoolean("pinned") ?: false,
                            createdAt = d.getTimestamp("createdAt")
                        )
                    }
                }
                isLoading = false
            }

        onDispose {
            reg?.remove()
        }
    }

    // 3) Track read announcements per-user
    // We store reads in: users/{uid}/announcementReads/{announcementId}
    DisposableEffect(userId) {
        if (userId == null) return@DisposableEffect onDispose { }

        var reg: ListenerRegistration? = null
        reg = db.collection("users")
            .document(userId)
            .collection("announcementReads")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    readIds = snapshot.documents.map { it.id }.toSet()
                }
            }

        onDispose { reg?.remove() }
    }

    // Filter list based on selected tab
    val filtered = remember(announcements, selectedTab, readIds) {
        when (selectedTab) {
            AlertsTab.ALL -> announcements
            AlertsTab.PINNED -> announcements.filter { it.pinned }
            AlertsTab.UNREAD -> announcements.filter { it.id !in readIds }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Announcements", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Stay updated with important notices",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            // Only teachers can add announcements
            if (userRole == "teacher") {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add announcement")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Tabs: All / Pinned / Unread
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AlertsTab.values().forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AlertsTab.values().size
                        )
                    ) {
                        Text(tab.label)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No announcements yet.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(filtered, key = { it.id }) { ann ->
                        val isUnread = userId != null && (ann.id !in readIds)

                        AnnouncementCard(
                            announcement = ann,
                            isUnread = isUnread,
                            canEdit = (userRole == "teacher"),
                            onClick = {
                                if (userId != null) {
                                    db.collection("users")
                                        .document(userId)
                                        .collection("announcementReads")
                                        .document(ann.id)
                                        .set(mapOf("readAt" to FieldValue.serverTimestamp()))
                                }
                            },
                            onTogglePin = {
                                if (userRole == "teacher") {
                                    db.collection("announcements")
                                        .document(ann.id)
                                        .update("pinned", !ann.pinned)
                                }
                            },
                            onDelete = {
                                if (userRole == "teacher") deletingAnnouncement = ann
                            }
                        )

                    }
                }
            }
        }
    }

    // ADD dialog
    if (showAddDialog) {
        AddOrEditAnnouncementDialog(
            title = "Add Announcement",
            initialTitle = "",
            initialBody = "",
            initialPinned = false,
            onDismiss = { showAddDialog = false },
            onSave = { title, body, pinned ->
                val uid = userId ?: return@AddOrEditAnnouncementDialog
                db.collection("announcements")
                    .add(
                        mapOf(
                            "title" to title,
                            "body" to body,
                            "authorName" to userName,
                            "authorId" to uid,
                            "pinned" to pinned,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                showAddDialog = false
            }
        )
    }

    // DELETE confirm
    deletingAnnouncement?.let { ann ->
        AlertDialog(
            onDismissRequest = { deletingAnnouncement = null },
            title = { Text("Delete announcement?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        db.collection("announcements")
                            .document(ann.id)
                            .delete()
                        deletingAnnouncement = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingAnnouncement = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AnnouncementCard(
    announcement: Announcement,
    isUnread: Boolean,
    canEdit: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = announcement.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = announcement.authorName.ifBlank { "Unknown" },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isUnread) {
                    AssistChip(
                        onClick = { /* purely visual */ },
                        label = { Text("New") }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = announcement.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (announcement.pinned) {
                        Icon(Icons.Filled.PushPin, contentDescription = "Pinned", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pinned", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(" ", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Text(
                    text = timeAgo(announcement.createdAt),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Teacher actions
            if (canEdit) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onTogglePin) {
                        Icon(Icons.Filled.PushPin, contentDescription = "Toggle pin")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddOrEditAnnouncementDialog(
    title: String,
    initialTitle: String,
    initialBody: String,
    initialPinned: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit
) {
    var t by remember { mutableStateOf(initialTitle) }
    var b by remember { mutableStateOf(initialBody) }
    var pinned by remember { mutableStateOf(initialPinned) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = t,
                    onValueChange = { t = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = b,
                    onValueChange = { b = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pinned, onCheckedChange = { pinned = it })
                    Text("Pinned")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val titleTrim = t.trim()
                    val bodyTrim = b.trim()
                    if (titleTrim.isNotEmpty() && bodyTrim.isNotEmpty()) {
                        onSave(titleTrim, bodyTrim, pinned)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun timeAgo(ts: Timestamp?): String {
    if (ts == null) return ""
    val now = System.currentTimeMillis()
    val then = ts.toDate().time
    val diff = now - then

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes} min ago"
        hours < 24 -> "${hours} hours ago"
        else -> "${days} days ago"
    }
}
