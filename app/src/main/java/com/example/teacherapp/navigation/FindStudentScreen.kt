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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Student(
    val id: String = "",
    val name: String = "",
    val grade: String = "",
    val interests: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindStudentScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var students by remember { mutableStateOf(listOf<Student>()) }
    var isLoading by remember { mutableStateOf(true) }

    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(Unit) {
        // Fetch all students
        db.collection("users")
            .whereEqualTo("role", "student")
            .get()
            .addOnSuccessListener { snapshot ->
                students = snapshot.documents.mapNotNull { doc ->
                    try {
                        Student(
                            id = doc.id,
                            name = doc.getString("name") ?: "Unknown",
                            grade = doc.getString("grade") ?: "",
                            interests = (doc.get("interests") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    val filteredStudents = students.filter { student ->
        student.name.contains(searchQuery, ignoreCase = true) ||
                student.grade.contains(searchQuery, ignoreCase = true) ||
                student.interests.any { it.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Student") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6366F1),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by name, grade, or interest...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF6B7280))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, "Clear", tint = Color(0xFF6B7280))
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredStudents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFFD1D5DB)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "No students found" else "No results",
                            fontSize = 16.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredStudents) { student ->
                        StudentCard(
                            student = student,
                            onClick = {
                                // Start conversation with student
                                if (currentUserId != null) {
                                    startConversation(db, currentUserId, student.id, navController)
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
fun StudentCard(
    student: Student,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )

                if (student.grade.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = student.grade,
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                if (student.interests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = student.interests.take(2).joinToString(", "),
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF),
                        maxLines = 1
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Message,
                contentDescription = "Message",
                tint = Color(0xFF6366F1),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun startConversation(
    db: FirebaseFirestore,
    teacherId: String,
    studentId: String,
    navController: NavController
) {
    // Check if conversation already exists
    db.collection("conversations")
        .whereArrayContains("participants", teacherId)
        .get()
        .addOnSuccessListener { snapshot ->
            val existingConversation = snapshot.documents.firstOrNull { doc ->
                val participants = (doc.get("participants") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                participants.contains(studentId)
            }

            if (existingConversation != null) {
                // Navigate to existing conversation
                navController.navigate("private_chat/${studentId}") {
                    popUpTo("find_student") { inclusive = true }
                }
            } else {
                // Create new conversation
                val conversationData = hashMapOf(
                    "participants" to arrayListOf(teacherId, studentId),
                    "lastMessage" to "",
                    "timestamp" to System.currentTimeMillis(),
                    "unreadCount_$teacherId" to 0,
                    "unreadCount_$studentId" to 0
                )

                db.collection("conversations")
                    .add(conversationData)
                    .addOnSuccessListener {
                        navController.navigate("private_chat/${studentId}") {
                            popUpTo("find_student") { inclusive = true }
                        }
                    }
            }
        }
}