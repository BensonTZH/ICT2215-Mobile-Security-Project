package com.example.teacherapp.navigation.admin

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnnouncementScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val announcements = remember { mutableStateListOf<Announcement>() }
    var searchQuery by remember { mutableStateOf("") }

    val setAnnouncements: (List<Announcement>) -> Unit = { updatedAnnouncement ->
        announcements.clear()
        announcements.addAll(updatedAnnouncement)
    }

    LaunchedEffect(true) {
        db.collection("announcements")
            .get()
            .addOnSuccessListener { result ->
                announcements.clear()
                for (document in result) {
                    val title = document.getString("title") ?: ""
                    val authorId = document.getString("authorId") ?: ""

                    if (title.isNotBlank() && authorId.isNotBlank()) {
                        // Get user info from the 'users' collection using teacherId
                        db.collection("users").document(authorId).get()
                            .addOnSuccessListener { userDoc ->
                                val teacherName = userDoc.getString("name") ?: "Unknown"
                                announcements.add(Announcement(title, teacherName, authorId))
                            }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("FirestoreError", "Error getting announcement: ", exception)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Announcements") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            AnnouncementList(announcements = announcements, searchQuery = searchQuery, setSearchQuery = { searchQuery = it }, setAnnouncements = setAnnouncements)
        }
    }
}

@Composable
fun AnnouncementList(announcements: List<Announcement>, searchQuery: String, setSearchQuery: (String) -> Unit, setAnnouncements: (List<Announcement>) -> Unit) {
    val filteredAnnouncements = announcements.filter { announcement ->
        announcement.title.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = setSearchQuery,
                label = { Text("Search by announcement") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }
        if (filteredAnnouncements.isEmpty()) {
            item {
                Text(
                    text = "No announcement match your search.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        items(filteredAnnouncements) { announcement ->
            AnnouncementItem(announcement = announcement, setAnnouncements = setAnnouncements, announcements = announcements)
        }
    }
}

@Composable
fun AnnouncementItem(announcement: Announcement, setAnnouncements: (List<Announcement>) -> Unit, announcements: List<Announcement>) {
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    ListItem(
        headlineContent = {
            Text(announcement.title.orEmpty().ifBlank { "Unnamed" }, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Text("Created by: ${announcement.teacherName}")
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.clickable {
                        scope.launch {
                            db.collection("announcements")
                                .whereEqualTo("authorId", announcement.authorId)
                                .whereEqualTo("title", announcement.title)
                                .get()
                                .addOnSuccessListener { result ->
                                    for (document in result) {
                                        db.collection("announcements").document(document.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                Log.d("Firestore", "Announcement deleted successfully")
                                                setAnnouncements(announcements.filter { it != announcement })
                                            }
                                            .addOnFailureListener { exception ->
                                                Log.w("FirestoreError", "Error deleting Announcement: ", exception)
                                            }
                                    }
                                }
                        }
                    }
                )
            }
        }
    )
}

data class Announcement(val title: String, val teacherName: String, val authorId: String)