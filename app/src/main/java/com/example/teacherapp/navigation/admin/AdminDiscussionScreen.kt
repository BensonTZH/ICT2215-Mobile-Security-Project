package com.example.teacherapp.navigation.admin

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.teacherapp.navigation.groups.GroupItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlin.collections.filter
import kotlin.text.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDiscussionScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val groups = remember { mutableStateListOf<Group>() }
    var searchQuery by remember { mutableStateOf("") }

    val setGroups: (List<Group>) -> Unit = { updatedGroups ->
        groups.clear()
        groups.addAll(updatedGroups)
    }

    LaunchedEffect(true) {
        db.collection("groups")
            .get()
            .addOnSuccessListener { result ->
                groups.clear()
                for (document in result) {
                    val groupName = document.getString("name") ?: ""
                    val teacherId = document.getString("teacherId") ?: ""

                    if (groupName.isNotBlank() && teacherId.isNotBlank()) {
                        // Get user info from the 'users' collection using teacherId
                        db.collection("users").document(teacherId).get()
                            .addOnSuccessListener { userDoc ->
                                val teacherName = userDoc.getString("name") ?: "Unknown"
                                groups.add(Group(groupName, teacherName, teacherId))
                            }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("FirestoreError", "Error getting groups: ", exception)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Discussions") },
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
            GroupList(groups = groups, searchQuery = searchQuery, setSearchQuery = { searchQuery = it }, setGroups = setGroups)
        }
    }
}

@Composable
fun GroupList(groups: List<Group>, searchQuery: String, setSearchQuery: (String) -> Unit, setGroups: (List<Group>) -> Unit) {
    val filteredGroups = groups.filter { group ->
        group.name.contains(searchQuery, ignoreCase = true) // Filter by group name
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = setSearchQuery,
                label = { Text("Search by group") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }
        if (filteredGroups.isEmpty()) {
            item {
                Text(
                    text = "No groups match your search.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        items(filteredGroups) { group ->
            GroupItem(group = group, setGroups = setGroups, groups = groups)
        }
    }
}

@Composable
fun GroupItem(group: Group, setGroups: (List<Group>) -> Unit, groups: List<Group>) {
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    ListItem(
        headlineContent = {
            Text(group.name.orEmpty().ifBlank { "Unnamed" }, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Text("Created by: ${group.teacherName}")
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.clickable {
                        scope.launch {
                            db.collection("groups")
                                .whereEqualTo("teacherId", group.teacherId)  // Match teacherId
                                .whereEqualTo("name", group.name)      // Match group name
                                .get()
                                .addOnSuccessListener { result ->
                                    for (document in result) {
                                        db.collection("groups").document(document.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                Log.d("Firestore", "Group deleted successfully")
                                                setGroups(groups.filter { it != group })
                                            }
                                            .addOnFailureListener { exception ->
                                                Log.w("FirestoreError", "Error deleting group: ", exception)
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

// Data class to hold group details and teacher name
data class Group(val name: String, val teacherName: String, val teacherId: String)