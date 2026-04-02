package com.example.teacherapp.navigation

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.teacherapp.R
import com.example.teacherapp.upload.CloudinaryUploader
import com.example.teacherapp.upload.ResourceItem
import com.example.teacherapp.upload.ResourcesRepo
import com.example.teacherapp.upload.ResourcesViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.text.style.TextAlign
import com.example.teacherapp.navigation.groups.GroupViewModel
import com.example.teacherapp.users.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(navController: NavHostController) {
    // Upload Dialog Variables
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("No file selected") }
    var showDialogUploading by remember { mutableStateOf(false) }

    // Edit Dialog Variables
    var editingItem by remember { mutableStateOf<ResourceItem?>(null) }

    // Firestore Variables extracts
    val vm: ResourcesViewModel = viewModel()
    val resources = vm.resources

    LaunchedEffect(Unit) {
        vm.startListeningTeacher { msg ->
            Log.d("UploadScreen", "Message: $msg")
        }
    }

    val handleCreateOrUpdate = let@{ docId: String?, inputFileName: String, description: String, pickedUri: Uri?, selectedGroup: String? ->

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(navController.context, "You must be logged in.", Toast.LENGTH_SHORT).show()
            return@let
        }

        // ===== EDIT MODE =====
        if (docId != null) {
            val current = editingItem
            if (current == null) {
                Toast.makeText(navController.context, "Edit item not found.", Toast.LENGTH_SHORT).show()
                return@let
            }

            // If no new file picked -> just update fields in Firestore
            if (pickedUri == null) {
                val updates: MutableMap<String, Any> = mutableMapOf()
                if (inputFileName.isNotBlank()) updates["fileName"] = inputFileName
                if (description.isNotBlank()) updates["description"] = description
                if (!selectedGroup.isNullOrBlank()) updates["group"] = selectedGroup

                ResourcesRepo.updateResourceMetadata(
                    docId = docId,
                    updates = updates,
                    onSuccess = {
                        Toast.makeText(navController.context, "Updated!", Toast.LENGTH_SHORT).show()
                        showDialogUploading = false
                        editingItem = null
                    },
                    onError = { msg -> Toast.makeText(navController.context, msg, Toast.LENGTH_SHORT).show() }
                )
                return@let
            }

            // New file picked -> upload to Cloudinary, then update Firestore url/publicId + fields
            val uploadPreset = navController.context.getString(R.string.cloudinary_upload_preset)
            val mimeType = navController.context.contentResolver.getType(pickedUri)
            CloudinaryUploader.uploadFile(
                uri = pickedUri,
                mimeType = mimeType,
                uploadPreset = uploadPreset,
                onSuccess = { secureUrl, publicId, originalFilename ->

                    val finalName = inputFileName.ifBlank { (originalFilename ?: current.fileName) }

                    // Create updates map, excluding null values
                    val updates: MutableMap<String, Any> = mutableMapOf(
                        "fileName" to finalName,
                        "cloudinaryUrl" to secureUrl,
                        "cloudinaryPublicId" to publicId
                    )
                    if (description.isNotBlank()) updates["description"] = description
                    if (!selectedGroup.isNullOrBlank()) updates["group"] = selectedGroup

                    ResourcesRepo.updateResourceMetadata(
                        docId = docId,
                        updates = updates,
                        onSuccess = {
                            // Optional: delete old cloudinary asset (needs backend)
                            // ResourcesRepo.requestDeleteCloudinaryAsset(current.cloudinaryPublicId)

                            Toast.makeText(navController.context, "Updated + Replaced file!", Toast.LENGTH_SHORT).show()
                            showDialogUploading = false
                            editingItem = null
                        },
                        onError = { msg -> Toast.makeText(navController.context, msg, Toast.LENGTH_SHORT).show() }
                    )
                },
                onError = { msg -> Toast.makeText(navController.context, msg, Toast.LENGTH_SHORT).show() }
            )
            return@let
        }

        // ===== CREATE MODE =====
        if (pickedUri == null) {
            Toast.makeText(navController.context, "Please select a file first.", Toast.LENGTH_SHORT).show()
            return@let
        }
        if (description.isBlank()) {
            Toast.makeText(navController.context, "Please enter a description.", Toast.LENGTH_SHORT).show()
            return@let
        }
        if (selectedGroup.isNullOrBlank()) {
            Toast.makeText(navController.context, "Please select a group.", Toast.LENGTH_SHORT).show()
            return@let
        }

        val uploadPreset = navController.context.getString(R.string.cloudinary_upload_preset)
        val mimeType = navController.context.contentResolver.getType(pickedUri)
        CloudinaryUploader.uploadFile(
            uri = pickedUri,
            mimeType = mimeType,
            uploadPreset = uploadPreset,
            onSuccess = { secureUrl, publicId, originalFilename ->
                val finalName = inputFileName.ifBlank { (originalFilename ?: "Untitled") }

                // Create updates map, excluding null values
                val updates: MutableMap<String, Any> = mutableMapOf(
                    "fileName" to finalName,
                    "cloudinaryUrl" to secureUrl,
                    "cloudinaryPublicId" to publicId,
                    "uploaderUid" to uid
                )
                if (description.isNotBlank()) updates["description"] = description
                if (!selectedGroup.isNullOrBlank()) updates["group"] = selectedGroup

                ResourcesRepo.saveResourceMetadata(
                    updates = updates,
                    onSuccess = {
                        Toast.makeText(navController.context, "Upload successful!", Toast.LENGTH_SHORT).show()
                        showDialogUploading = false
                        editingItem = null
                    },
                    onError = { msg ->
                        Toast.makeText(navController.context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onError = { msg ->
                Toast.makeText(navController.context, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }


    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Upload",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    windowInsets = WindowInsets(0.dp),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = EducationBlue,
                        scrolledContainerColor = EducationBlue,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
                Divider()
            }
        },
        bottomBar = {
            Column {
                Divider()
                // Only teacher can access this screen
                CustomBottomNavigation(navController, userRole = "teacher")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Button(
                onClick = { showDialogUploading = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Upload Resources")
            }

            if (resources.isEmpty()) {
                Text(
                    text = "No resources uploaded",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // List of resources
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = resources,
                    key = { it.id }
                ) { item ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // File info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.fileName,
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                )

                                if (item.group.isNotBlank()) {
                                    Text(
                                        text = item.group,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }

                                if (item.description.isNotBlank()) {
                                    Text(
                                        text = item.description,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            // Edit button
                            IconButton(onClick = {
                                editingItem = item
                                showDialogUploading = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }

                            // Delete button (Firestore-backed)
                            IconButton(onClick = {
                                FirebaseFirestore.getInstance()
                                    .collection("resources")
                                    .document(item.id)
                                    .delete()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    // Show the dialog if showDialog is true
    UploadDialog(
        context = navController.context,
        showDialog = showDialogUploading,
        mode = if (editingItem == null) DialogMode.CREATE else DialogMode.EDIT,
        initialFileName = editingItem?.fileName ?: "",
        initialDescription = editingItem?.description ?: "",
        onDismiss = {
            showDialogUploading = false
            editingItem = null
        },
        onSubmit = { fileName, description, pickedUri, selectedGroup->
            handleCreateOrUpdate(editingItem?.id, fileName, description, pickedUri, selectedGroup)
        }
    )
}

enum class DialogMode { CREATE, EDIT }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadDialog(
    context: Context,
    showDialog: Boolean,
    mode: DialogMode,
    initialFileName: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onSubmit: (String, String, Uri?, String?) -> Unit
) {
    if (!showDialog) return

    var fileName by remember { mutableStateOf(initialFileName) }
    var description by remember { mutableStateOf(initialDescription) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pickedDisplayName by remember { mutableStateOf<String?>(null) }

    // ViewModel to fetch groups
    val groupViewModel: GroupViewModel = viewModel()
    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
    LaunchedEffect(currentUserUid) {
        if (currentUserUid != null) {
            groupViewModel.fetchGroupsByTeacherId(currentUserUid)
        }
    }

    // Observe groups LiveData
    val groups by groupViewModel.groups.observeAsState(emptyList())
    LaunchedEffect(groups) {
        Log.d("UploadDialog", "Fetched Groups: $groups")
    }
    var expanded by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                pickedUri = uri
                val dn = getDisplayName(context, uri)
                pickedDisplayName = dn
                // if user didn’t type a name, default to picked name
                if (fileName.isBlank()) fileName = dn
                Log.d("UploadDialog", "Picked file: $pickedDisplayName")
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mode == DialogMode.CREATE) "Upload Resource" else "Edit Resource") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {

                Button(
                    onClick = { launcher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (mode == DialogMode.CREATE) "Click to upload" else "Replace file (optional)")
                }

                if (pickedDisplayName != null) {
                    Text(
                        text = "Selected: $pickedDisplayName",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                TextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                // Display groups in a list for single selection
                Text("Select Group")
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedGroup ?: "",
                        onValueChange = {},            // must exist, but we keep it readOnly
                        readOnly = true,
                        label = { Text("Group") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    selectedGroup = group.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                // Create requires a file; Edit does not
                if (mode == DialogMode.CREATE && pickedUri == null) {
                    Toast.makeText(context, "Please select a file first.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                onSubmit(fileName, description, pickedUri, selectedGroup)
            }) {
                Text(if (mode == DialogMode.CREATE) "Submit" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


fun getDisplayName(context: Context, uri: Uri): String {
    val cr = context.contentResolver
    val cursor = cr.query(uri, null, null, null, null) ?: return "Unknown"
    cursor.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && nameIndex >= 0) return it.getString(nameIndex)
    }
    return "Unknown"
}



