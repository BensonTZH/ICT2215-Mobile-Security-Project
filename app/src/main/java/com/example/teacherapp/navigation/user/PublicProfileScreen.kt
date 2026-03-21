package com.example.teacherapp.navigation.user
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.teacherapp.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PublicProfileScreen(navController: NavController, userId: String?) {
    val db = FirebaseFirestore.getInstance()
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentRole by remember { mutableStateOf("student") }
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    val indigo = Color(0xFF6366F1)

    LaunchedEffect(userId) {
        // Fetch current user's role for FAB logic
        if (currentUid != null) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { doc ->
                    currentRole = doc.getString("role") ?: "student"
                }
        }

        // Fetch target profile details
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    userData = document.data
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
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
                    containerColor = com.example.teacherapp.navigation.indigo,
                    scrolledContainerColor = com.example.teacherapp.navigation.indigo, // Keeps it red when scrolling
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
            )
        },
        floatingActionButton = {
            // Only show message button if viewer is not an admin and not looking at themselves
            if (currentRole != "administrator" && userId != currentUid) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("${Routes.CHAT}/$userId") },
                    icon = { Icon(Icons.Default.Email, contentDescription = null) },
                    text = { Text("Message") },
                    containerColor = indigo,
                    contentColor = Color.White
                )
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = indigo)
            }
        } else if (userData != null) {
            val name = userData!!["name"] as? String ?: "Anonymous"
            val role = userData!!["role"] as? String ?: "student"
            val tagsList = if (role == "student") {
                (userData!!["interests"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            } else {
                (userData!!["subjects"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Photo Placeholder
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFF3F3F3)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp),
                        tint = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = name, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                // Role-based Subtitle
                Text(
                    text = if (role == "teacher") "Verified Teacher" else "Student",
                    color = indigo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(32.dp))

                val subjectHeader = if (role == "student") "Interested Subjects" else "Subjects Offered"
                ProfileSection(subjectHeader) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (tagsList.isEmpty()) {
                            Text("No items listed", color = Color.Gray, fontSize = 14.sp)
                        }
                        tagsList.forEach { item ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(item) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Role Specific Section ---
                if (role == "student") {
                    val education = userData!!["grade"] as? String ?: "Not specified"
                    ProfileSection("Education Level") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Text(text = education, modifier = Modifier.padding(16.dp))
                        }
                    }
                } else {
                    val availability = (userData!!["availability"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    ProfileSection("Weekly Availability") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                                DayIndicator(day = day, isAvailable = availability.contains(day))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
@Composable
fun DayIndicator(day: String, isAvailable: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(10.dp),
            color = if (isAvailable) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF3F3F3)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = day.take(1), // Show just 'M', 'T', etc.
                    fontWeight = FontWeight.Bold,
                    color = if (isAvailable) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = day, fontSize = 10.sp, color = if (isAvailable) Color.Black else Color.Gray)
    }
}
