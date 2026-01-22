package com.example.teacherapp.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.window.PopupProperties
import com.example.teacherapp.models.AppData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("All") }
    val subjects = remember { listOf("All") + AppData.allSubjects }
    var selectedDay by remember { mutableStateOf("Any") }

    var teacherList by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    val days = listOf("Any", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    LaunchedEffect(selectedSubject, selectedDay) {
        var query: Query = db.collection("users").whereEqualTo("role", "teacher")

        // Filter by Subject
        if (selectedSubject != "All") {
            query = query.whereEqualTo("subject", selectedSubject)
        }

        // Filter by Day (Using whereArrayContains)
        if (selectedDay != "Any") {
            query = query.whereArrayContains("availability", selectedDay)
        }

        query.addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                teacherList = snapshot.documents.mapNotNull { it.data }
            }
        }
    }

    val filteredTeachers = remember(searchQuery, teacherList) {
        if (searchQuery.isEmpty()) {
            teacherList
        } else {
            teacherList.filter { teacher ->
                val name = teacher["name"] as? String ?: ""
                name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find a Teacher") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name or keyword...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* Execute Search */ })
            )

            // 1. SUBJECT CHIPS
            Text("Subject", modifier = Modifier.padding(start = 16.dp), style = MaterialTheme.typography.labelLarge)
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(subjects) { subject ->
                    FilterChip(
                        selected = selectedSubject == subject,
                        onClick = { selectedSubject = subject },
                        label = { Text(subject) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // 2. DAY CHIPS
            Text("Available on", modifier = Modifier.padding(start = 16.dp, top = 8.dp), style = MaterialTheme.typography.labelLarge)
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(days) { day ->
                    FilterChip(
                        selected = selectedDay == day,
                        onClick = { selectedDay = day },
                        label = { Text(day) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTeachers) { teacher ->
                    val name = teacher["name"] as? String ?: "Anonymous"
                    val subject = teacher["subject"] as? String ?: "General"

                    val availability = (teacher["availability"] as? List<*>)
                        ?.filterIsInstance<String>()
                        ?: emptyList()

                    TeacherCard(
                        name = name,
                        subject = subject,
                        availability = availability
                    )
                }
            }
        }
    }
}
@Composable
fun TeacherCard(name: String, subject: String, availability: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Profile Icon
            Surface(modifier = Modifier.size(60.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFFF3F3F3)) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = subject, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)

                // Show Days
                Text(
                    text = "Days: ${availability.joinToString(", ")}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}