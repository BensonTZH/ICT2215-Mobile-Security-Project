package com.example.teacherapp.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.teacherapp.models.AppData
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()

    // 1. STATE VARIABLES
    var searchQuery by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf("Any") }
    // Now a Set to allow multiple selections
    var selectedSubjects by remember { mutableStateOf(setOf<String>()) }

    // Master data from Firebase
    var teacherList by remember { mutableStateOf(listOf<Map<String, Any>>()) }

    // Constants from your model/lists
    val subjectOptions = remember { AppData.allSubjects }
    val days = listOf("Any", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("role", "teacher")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
//                    teacherList = snapshot.documents.mapNotNull { it.data }
                    teacherList = snapshot.documents.map { doc ->
                        val data = doc.data ?: emptyMap()
                        data + mapOf("uid" to doc.id)
                    }

                }
            }
    }

    val filteredTeachers = remember(searchQuery, selectedSubjects, selectedDay, teacherList) {
        teacherList.filter { teacher ->
            val name = teacher["name"] as? String ?: ""
            val teacherSubjects = (teacher["subjects"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val availability = (teacher["availability"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            val matchesSearch = name.contains(searchQuery, ignoreCase = true)

            val matchesSubject = if (selectedSubjects.isEmpty()) true
            else teacherSubjects.any { it in selectedSubjects }

            val matchesDay = if (selectedDay == "Any") true
            else availability.contains(selectedDay)

            matchesSearch && matchesSubject && matchesDay
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
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // SUBJECT MULTI-SELECT CHIPS
            Text("Subjects", modifier = Modifier.padding(start = 16.dp), style = MaterialTheme.typography.labelLarge)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subjectOptions) { subject ->
                    val isSelected = selectedSubjects.contains(subject)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedSubjects = if (isSelected) selectedSubjects - subject else selectedSubjects + subject
                        },
                        label = { Text(subject) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // DAY CHIPS
            Text("Available on", modifier = Modifier.padding(start = 16.dp, top = 8.dp), style = MaterialTheme.typography.labelLarge)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(days) { day ->
                    FilterChip(
                        selected = selectedDay == day,
                        onClick = { selectedDay = day },
                        label = { Text(day) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

            // RESULTS LIST
            if (filteredTeachers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No teachers found matching your filters.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTeachers) { teacher ->
                        val tName = teacher["name"] as? String ?: "Anonymous"
                        val tSubjects = (teacher["subjects"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        val tAvailability = (teacher["availability"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        val teacherId = teacher["uid"] as? String ?: ""

                        TeacherCard(
                            name = tName,
                            subjects = tSubjects,
                            availability = tAvailability,
                            onCardClick = {
                                if (teacherId.isNotBlank()) {
                                    // Navigate to the Public Profile and pass the teacher's ID
                                    navController.navigate("${Routes.PUBLIC_PROFILE}/$teacherId")
                                }
                            },
                            onMessageClick = {
                                if (teacherId.isNotBlank()) {
                                    navController.navigate("${Routes.CHAT}/$teacherId")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun TeacherCard(
    name: String,
    subjects: List<String>,
    availability: List<String>,
    onCardClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF3F3F3)
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Text(
                    text = subjects.joinToString(", "),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )

                Text(
                    text = "Available: ${availability.joinToString(", ")}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onMessageClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Message")
            }
        }
    }
}
