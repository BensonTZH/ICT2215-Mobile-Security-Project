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

                    subjects = doc.get("subjects") as? List<String> ?: emptyList()
                    availability = doc.get("availability") as? List<String> ?: emptyList()

                    isLoading = false
                }
        } else isLoading = false
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("My Profile", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showEdit = true }) {
                        Icon(Icons.Default.Edit, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ProfilePurple),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            Column {
                Divider()
                CustomBottomNavigation(navController)
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
                            Divider()
                            ProfileInfoRow(Icons.Default.Book, "Subjects", subjects.joinToString(", "))
                        }

                        if (availability.isNotEmpty()) {
                            Divider()
                            ProfileInfoRow(Icons.Default.CalendarToday, "Availability", availability.joinToString(", "))
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
            var selectedSubjects by remember { mutableStateOf(subjects.toSet()) }
            var selectedDays by remember { mutableStateOf(availability.toSet()) }

            var showSubjectPicker by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showEdit = false },
                title = { Text("Edit Profile") },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 500.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        // ✅ BOTH STUDENT + TEACHER CAN EDIT NAME
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Name") }
                        )

                        // ✅ ONLY TEACHER SEES THESE
                        if (roleRaw == "teacher") {

                            Text("Subjects", fontWeight = FontWeight.Bold)

                            FlowRow {
                                selectedSubjects.forEach {
                                    InputChip(
                                        selected = true,
                                        onClick = { selectedSubjects -= it },
                                        label = { Text(it) }
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { showSubjectPicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add Subject")
                            }

                            Text("Availability", fontWeight = FontWeight.Bold)

                            FlowRow {
                                AppData.daysOfWeek.forEach { day ->
                                    val selected = selectedDays.contains(day)

                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            selectedDays =
                                                if (selected) selectedDays - day
                                                else selectedDays + day
                                        },
                                        label = { Text(day) }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val uid = auth.currentUser?.uid ?: return@TextButton

                        val updateMap = mutableMapOf<String, Any>(
                            "name" to newName
                        )

                        // ✅ ONLY TEACHER DATA UPDATED
                        if (roleRaw == "teacher") {
                            updateMap["subjects"] = selectedSubjects.toList()
                            updateMap["availability"] = selectedDays.toList()
                        }

                        db.collection("users").document(uid)
                            .update(updateMap)
                            .addOnSuccessListener {
                                name = newName
                                subjects = selectedSubjects.toList()
                                availability = selectedDays.toList()
                                showEdit = false
                            }
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEdit = false }) {
                        Text("Cancel")
                    }
                }
            )

            // SUBJECT PICKER (TEACHER ONLY)
            if (showSubjectPicker && roleRaw == "teacher") {
                AlertDialog(
                    onDismissRequest = { showSubjectPicker = false },
                    title = { Text("Select Subjects") },
                    text = {
                        Column {
                            AppData.allSubjects.forEach { subject ->
                                val selected = selectedSubjects.contains(subject)

                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedSubjects =
                                            if (selected) selectedSubjects - subject
                                            else selectedSubjects + subject
                                    },
                                    label = { Text(subject) }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSubjectPicker = false }) {
                            Text("Done")
                        }
                    }
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