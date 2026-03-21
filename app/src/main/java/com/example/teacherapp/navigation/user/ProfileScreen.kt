package com.example.teacherapp.navigation.user

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.teacherapp.R
import com.example.teacherapp.models.AppData
import com.example.teacherapp.navigation.CustomBottomNavigation
import com.example.teacherapp.navigation.EducationBlue
import com.example.teacherapp.upload.CloudinaryUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

val ProfilePurple = Color(0xFF6200EE)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val indigo = Color(0xFF6366F1)
    var education by remember { mutableStateOf("None") }
    var interests by remember { mutableStateOf(listOf<String>()) }

    var profileImageUrl by remember { mutableStateOf("") }

    fun uploadToCloudinary(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val uploadPreset = context.getString(R.string.cloudinary_upload_preset)

        CloudinaryUploader.uploadFile(
            uri = uri,
            uploadPreset = uploadPreset,
            onSuccess = { url, _, _ ->
                db.collection("users").document(uid)
                    .update("profileImageUrl", url)
                    .addOnSuccessListener { profileImageUrl = url }
            },
            onError = {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        )
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { uploadToCloudinary(it) } }

    var name by remember { mutableStateOf("...") }
    var email by remember { mutableStateOf("...") }
    var roleRaw by remember { mutableStateOf("") }
    var roleDisplay by remember { mutableStateOf("User") }
    var isLoading by remember { mutableStateOf(true) }

    var subjects by remember { mutableStateOf(listOf<String>()) }
    var availability by remember { mutableStateOf(listOf<String>()) }

    var showEdit by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    name = doc.getString("name") ?: "No Name"
                    profileImageUrl = doc.getString("profileImageUrl") ?: ""
                    email = doc.getString("email") ?: auth.currentUser?.email ?: "No Email"
                    roleRaw = (doc.getString("role") ?: "").lowercase()
                    roleDisplay = roleRaw.replaceFirstChar { it.uppercase() }

                    // Role-specific data
                    if (roleRaw == "teacher") {
                        subjects = doc.get("subjects") as? List<String> ?: emptyList()
                        availability = doc.get("availability") as? List<String> ?: emptyList()
                    } else {
                        education = doc.getString("grade") ?: "None"
                        interests = doc.get("interests") as? List<String> ?: emptyList()
                    }

                    isLoading = false
                }
        } else isLoading = false
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEdit = true }) {
                        Icon(Icons.Default.Edit, null, tint = Color.White)
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = indigo,
                    scrolledContainerColor = indigo,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
            )
        },
        bottomBar = {
            Column {
                Divider()
                CustomBottomNavigation(navController, userRole = roleRaw)
            }
        }
    ) { padding ->

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (profileImageUrl.isNotBlank()) {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp).clip(CircleShape)
                )
            } else {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = ProfilePurple.copy(0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(name.take(1).uppercase(), fontSize = 40.sp, color = ProfilePurple)
                    }
                }
            }

            TextButton(onClick = { imagePicker.launch("image/*") }) {
                Text("Change Profile Picture")
            }

            Spacer(Modifier.height(16.dp))

            Text(name, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(roleDisplay, color = ProfilePurple)

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    ProfileInfoRow(Icons.Default.Email, "Email", email)

                    if (roleRaw == "teacher") {
                        if (subjects.isNotEmpty()) {
                            Divider(); ProfileInfoRow(Icons.Default.Book, "Subjects", subjects.joinToString(", "))
                        }
                        if (availability.isNotEmpty()) {
                            Divider(); ProfileInfoRow(Icons.Default.CalendarToday, "Availability", availability.joinToString(", "))
                        }
                    } else {
                        // STUDENT VIEW
                        Divider()
                        ProfileInfoRow(Icons.Default.School, "Education", education)
                        if (interests.isNotEmpty()) {
                            Divider()
                            ProfileInfoRow(Icons.Default.Favorite, "Interests", interests.joinToString(", "))
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    auth.signOut()
                    navController.navigate("login_screen") { popUpTo(0) }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log Out")
            }
        }

        // EDIT DIALOG (TEACHER ONLY)
        // ONLY SHOW EDIT DIALOG
        if (showEdit) {
            var newName by remember { mutableStateOf(name) }
            var newEducation by remember { mutableStateOf(education) }
            var selectedSubjects by remember { mutableStateOf(subjects.toSet()) }
            var selectedInterests by remember { mutableStateOf(interests.toSet()) }
            var selectedDays by remember { mutableStateOf(availability.toSet()) }

            var showPicker by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showEdit = false },
                title = { Text("Edit Profile") },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Name") })

                        if (roleRaw == "teacher") {
                            Text("Subjects", fontWeight = FontWeight.Bold)
                            FlowRow {
                                selectedSubjects.forEach { InputChip(selected = true, onClick = { selectedSubjects -= it }, label = { Text(it) }) }
                            }
                            OutlinedButton(onClick = { showPicker = true }) { Text("Edit Subject") }

                            Text("Availability", fontWeight = FontWeight.Bold)
                            FlowRow {
                                AppData.daysOfWeek.forEach { day ->
                                    FilterChip(
                                        selected = selectedDays.contains(day),
                                        onClick = { selectedDays = if (selectedDays.contains(day)) selectedDays - day else selectedDays + day },
                                        label = { Text(day) }
                                    )
                                }
                            }
                        } else {
                            OutlinedTextField(value = newEducation, onValueChange = { newEducation = it }, label = { Text("Education Level") })
                            Text("Interests", fontWeight = FontWeight.Bold)
                            FlowRow {
                                selectedInterests.forEach { InputChip(selected = true, onClick = { selectedInterests -= it }, label = { Text(it) }) }
                            }
                            OutlinedButton(onClick = { showPicker = true }) { Text("Edit Interests") }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val uid = auth.currentUser?.uid ?: return@TextButton
                        val updateMap = mutableMapOf<String, Any>("name" to newName)
                        if (roleRaw == "teacher") {
                            updateMap["subjects"] = selectedSubjects.toList()
                            updateMap["availability"] = selectedDays.toList()
                        } else {
                            updateMap["grade"] = newEducation
                            updateMap["interests"] = selectedInterests.toList()
                        }
                        db.collection("users").document(uid).update(updateMap).addOnSuccessListener {
                            name = newName
                            education = newEducation
                            subjects = selectedSubjects.toList()
                            interests = selectedInterests.toList()
                            availability = selectedDays.toList()
                            showEdit = false
                        }
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Cancel") } }
            )

            // Dynamic Picker for both Subjects and Interests
            if (showPicker) {
                AlertDialog(
                    onDismissRequest = { showPicker = false },
                    title = { Text(if (roleRaw == "teacher") "Select Subjects" else "Select Interests") },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            AppData.allSubjects.forEach { item ->
                                val isSelected = if (roleRaw == "teacher") selectedSubjects.contains(item) else selectedInterests.contains(item)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (roleRaw == "teacher") {
                                            selectedSubjects = if (isSelected) selectedSubjects - item else selectedSubjects + item
                                        } else {
                                            selectedInterests = if (isSelected) selectedInterests - item else selectedInterests + item
                                        }
                                    },
                                    label = { Text(item) }
                                )
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showPicker = false }) { Text("Done") } }
                )
            }
        }
    }
}

@Composable
fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null)
        Column(Modifier.padding(start = 16.dp)) {
            Text(label, fontSize = 12.sp)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}