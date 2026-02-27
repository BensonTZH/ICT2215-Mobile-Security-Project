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
fun PublicProfileScreen(navController: NavController, teacherId: String?) {
    val db = FirebaseFirestore.getInstance()
    var teacherData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentRole by remember { mutableStateOf("student") }
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    // Fetch Teacher Details
    LaunchedEffect(teacherId) {
        if (currentUid != null) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { doc ->
                    currentRole = doc.getString("role") ?: "student"
                }
        }

        if (teacherId != null) {
            db.collection("users").document(teacherId).get()
                .addOnSuccessListener { document ->
                    teacherData = document.data
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentRole != "administrator") {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("${Routes.CHAT}/$teacherId") },
                    icon = { Icon(Icons.Default.Email, contentDescription = null) },
                    text = { Text("Message Teacher") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (teacherData != null) {
            val name = teacherData!!["name"] as? String ?: "Anonymous"
            val subjects = (teacherData!!["subjects"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val availability = (teacherData!!["availability"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Header
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFF3F3F3)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp).padding(20.dp),
                        tint = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = "Verified Teacher", color = Color.Gray, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(32.dp))

                // Subjects Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Subjects Offered", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subjects.forEach { subject ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(subject) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Availability Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Weekly Availability", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        allDays.forEach { day ->
                            val isAvailable = availability.contains(day)
                            DayIndicator(day = day, isAvailable = isAvailable)
                        }
                    }
                }
            }
        }
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
