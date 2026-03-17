package com.example.teacherapp.navigation.user

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.example.teacherapp.models.AppData
import com.example.teacherapp.navigation.CustomBottomNavigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

val ProfilePurple = Color(0xFF6200EE)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Firestore fields
    var name by remember { mutableStateOf("...") }
    var email by remember { mutableStateOf("...") }
    var roleRaw by remember { mutableStateOf("") } // "student" / "teacher"
    var roleDisplay by remember { mutableStateOf("User") }
    var isLoading by remember { mutableStateOf(true) }

    // Student fields
    var grade by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf(listOf<String>()) }

    // Teacher fields
    var subjects by remember { mutableStateOf(listOf<String>()) }
    var availability by remember { mutableStateOf(listOf<String>()) }

    // UI
    var showEdit by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()


    // Fetch data from Firestore
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    name = doc.getString("name") ?: "No Name"

                    // Prefer Firestore email, fallback to Auth email
                    email = doc.getString("email")
                        ?: auth.currentUser?.email
                                ?: "No Email"

                    roleRaw = (doc.getString("role") ?: "").lowercase()
                    roleDisplay = roleRaw.replaceFirstChar { it.uppercase() }.ifBlank { "User" }

                    grade = doc.getString("grade") ?: ""
                    interests = doc.get("interests") as? List<String> ?: emptyList()

                    subjects = doc.get("subjects") as? List<String> ?: emptyList()
                    availability = doc.get("availability") as? List<String> ?: emptyList()

                    isLoading = false
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    scope.launch { snackbarHostState.showSnackbar("Failed to load profile: ${e.message}") }
                }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Profile",
                        fontSize = 20.sp, // Compact font size
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showEdit = true }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = Color.White
                        )
                    }
                },
                // Removes status bar padding to shrink the height
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ProfilePurple,
                    scrolledContainerColor = ProfilePurple, // Keep it purple when scrolling
                    titleContentColor = Color.White,
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            if (!isLoading && roleRaw.isNotBlank() && roleRaw != "administrator") {
                Column {
                    Divider()
                    CustomBottomNavigation(navController)
                }
            }
        }
    ) { padding ->

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Initial-based Avatar (Matches Discovery/Inbox style)
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape, // Changed to Circle for consistency
                color = ProfilePurple.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = ProfilePurple,
                        fontSize = 40.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = name, fontSize = 26.sp, fontWeight = FontWeight.Bold) // Increased size
            Text(
                text = roleDisplay,
                fontSize = 18.sp, // Increased size
                color = ProfilePurple, // Match theme
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp), // Small padding to show the shadow
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp) // High elevation for 3D effect
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Email Row
                    ProfileInfoRow(icon = Icons.Default.Email, label = "Email", value = email)

                    if (roleRaw == "student") {
                        if (grade.isNotBlank()) {
                            Divider(modifier = Modifier.padding(horizontal = 8.dp), thickness = 0.5.dp, color = Color.LightGray)
                            ProfileInfoRow(icon = Icons.Default.School, label = "Grade", value = grade)
                        }
                        if (interests.isNotEmpty()) {
                            Divider(modifier = Modifier.padding(horizontal = 8.dp), thickness = 0.5.dp, color = Color.LightGray)
                            ProfileInfoRow(
                                icon = Icons.Default.Star,
                                label = "Interests",
                                value = interests.joinToString(", ")
                            )
                        }
                    }

                    if (roleRaw == "teacher") {
                        if (subjects.isNotEmpty()) {
                            Divider(modifier = Modifier.padding(horizontal = 8.dp), thickness = 0.5.dp, color = Color.LightGray)
                            ProfileInfoRow(
                                icon = Icons.Default.Book,
                                label = "Subjects",
                                value = subjects.joinToString(", ")
                            )
                        }
                        if (availability.isNotEmpty()) {
                            Divider(modifier = Modifier.padding(horizontal = 8.dp), thickness = 0.5.dp, color = Color.LightGray)
                            ProfileInfoRow(
                                icon = Icons.Default.CalendarToday,
                                label = "Availability",
                                value = availability.joinToString(", ")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Change password (send reset email) — with feedback
            OutlinedButton(
                onClick = {
                    val emailToSend = auth.currentUser?.email ?: email
                    if (emailToSend.isBlank() || emailToSend == "No Email") {
                        scope.launch { snackbarHostState.showSnackbar("No email found for this account") }
                        return@OutlinedButton
                    }

                    auth.sendPasswordResetEmail(emailToSend)
                        .addOnSuccessListener {
                            scope.launch {
                                snackbarHostState.showSnackbar("Password reset email sent to $emailToSend")
                            }
                        }
                        .addOnFailureListener { e ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to send reset email: ${e.message}")
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Password")
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout
            Button(
                onClick = {
                    auth.signOut()
                    navController.navigate("login_screen") { popUpTo(0) }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Log Out")
            }
        }

        // ===== EDIT DIALOG (chips + dropdown + day chips, same as SetupProfileScreen) =====
        if (showEdit) {

            // shared
            var newName by remember { mutableStateOf(name) }

            // student states
            var newGrade by remember { mutableStateOf(grade) }
            var interestInput by remember { mutableStateOf("") }
            var interestsExpanded by remember { mutableStateOf(false) }
            var selectedInterests by remember { mutableStateOf(interests.toSet()) }

            val filteredInterests = AppData.allSubjects.filter {
                it.contains(interestInput, ignoreCase = true) && !selectedInterests.contains(it)
            }

            // teacher states
            var subjectInput by remember { mutableStateOf("") }
            var subjectsExpanded by remember { mutableStateOf(false) }
            var selectedSubjects by remember { mutableStateOf(subjects.toSet()) }
            var selectedDays by remember { mutableStateOf(availability.toSet()) }

            val filteredSubjects = AppData.allSubjects.filter {
                it.contains(subjectInput, ignoreCase = true) && !selectedSubjects.contains(it)
            }

            AlertDialog(
                onDismissRequest = { showEdit = false },
                title = { Text("Edit Profile") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        when (roleRaw) {
                            "student" -> {

                                Text(
                                    "I want to learn:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    selectedInterests.forEach { interest ->
                                        InputChip(
                                            selected = true,
                                            onClick = { selectedInterests = selectedInterests - interest },
                                            label = { Text(interest) },
                                            trailingIcon = {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                            }
                                        )
                                    }
                                }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = interestInput,
                                        onValueChange = {
                                            interestInput = it
                                            interestsExpanded = it.isNotEmpty()
                                        },
                                        label = { Text("Search subjects...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = { Icon(Icons.Default.Search, null) }
                                    )

                                    DropdownMenu(
                                        expanded = interestsExpanded && filteredInterests.isNotEmpty(),
                                        onDismissRequest = { interestsExpanded = false },
                                        properties = PopupProperties(focusable = false)
                                    ) {
                                        filteredInterests.forEach { subject ->
                                            DropdownMenuItem(
                                                text = { Text(subject) },
                                                onClick = {
                                                    selectedInterests = selectedInterests + subject
                                                    interestInput = ""
                                                    interestsExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            "teacher" -> {
                                Text(
                                    "Subjects I Teach",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    selectedSubjects.forEach { subject ->
                                        InputChip(
                                            selected = true,
                                            onClick = { selectedSubjects = selectedSubjects - subject },
                                            label = { Text(subject) },
                                            trailingIcon = {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                            }
                                        )
                                    }
                                }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = subjectInput,
                                        onValueChange = {
                                            subjectInput = it
                                            subjectsExpanded = it.isNotEmpty()
                                        },
                                        label = { Text("Add a Subject") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                                    )

                                    DropdownMenu(
                                        expanded = subjectsExpanded && filteredSubjects.isNotEmpty(),
                                        onDismissRequest = { subjectsExpanded = false },
                                        properties = PopupProperties(focusable = false)
                                    ) {
                                        filteredSubjects.forEach { selection ->
                                            DropdownMenuItem(
                                                text = { Text(selection) },
                                                onClick = {
                                                    selectedSubjects = selectedSubjects + selection
                                                    subjectInput = ""
                                                    subjectsExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Text(
                                    "Select Availability",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AppData.daysOfWeek.forEach { day ->
                                        val isSelected = selectedDays.contains(day)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                                            },
                                            label = { Text(day) },
                                            leadingIcon = if (isSelected) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val uid = auth.currentUser?.uid
                        if (uid == null) {
                            Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }

                        if (newName.trim().isEmpty()) {
                            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }

                        // Build update map
                        val updateMap = mutableMapOf<String, Any>(
                            "name" to newName.trim()
                        )

                        when (roleRaw) {
                            "student" -> {
                                updateMap["interests"] = selectedInterests.toList().sorted()
                            }

                            "teacher" -> {
                                updateMap["subjects"] = selectedSubjects.toList().sorted()
                                updateMap["availability"] =
                                    selectedDays.toList().sortedBy { AppData.daysOfWeek.indexOf(it) }
                            }
                        }

                        db.collection("users").document(uid)
                            .update(updateMap)
                            .addOnSuccessListener {
                                // Update local UI state immediately
                                name = newName.trim()
                                if (roleRaw == "student") {
                                    grade = newGrade.trim()
                                    interests = (updateMap["interests"] as? List<String>) ?: emptyList()
                                } else if (roleRaw == "teacher") {
                                    subjects = (updateMap["subjects"] as? List<String>) ?: emptyList()
                                    availability = (updateMap["availability"] as? List<String>) ?: emptyList()
                                }

                                showEdit = false
                                scope.launch { snackbarHostState.showSnackbar("Profile updated") }
                            }
                            .addOnFailureListener { e ->
                                scope.launch { snackbarHostState.showSnackbar("Update failed: ${e.message}") }
                            }
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEdit = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rounded icon background
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = ProfilePurple.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = ProfilePurple,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}
