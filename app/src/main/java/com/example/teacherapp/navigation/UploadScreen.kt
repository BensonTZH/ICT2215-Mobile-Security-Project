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
import androidx.compose.runtime.getValue
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
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(navController: NavHostController) {
    // Upload Dialog Variables
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("No file selected") }
    var showDialogUploading by remember { mutableStateOf(false) }

    // Function to handle form submission, uploading to database
    val handleSubmit = { fileName: String, description: String ->
        // TODO: Link sumbission to database
        Toast.makeText(navController.context, "File: $fileName\nDescription: $description", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                },
                // Actions are the icons on the right side
                actions = {
                    IconButton(onClick = { /* TODO: Navigate to Mail */ }) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = "Mail")
                    }
                    IconButton(onClick = { navController.navigate("profile_screen") }) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "Profile")
                    }
                    IconButton(onClick = { navController.navigate("settings_screen") }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            // Button to trigger dialog
            Button(
                onClick = { showDialogUploading = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Upload Resources")
            }

            // Dummy list of uploaded files for illustration
            val uploadedFiles = remember { mutableListOf("File1.pdf", "Image.png", "Document.docx") }
            // List of Uploaded Files (Simulated)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TODO: Extract list of uploaded files, Scroll if out of screen
                uploadedFiles.forEachIndexed { index, file ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // File name text
                            Text(
                                text = file,
                                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
                                modifier = Modifier.weight(1f)
                            )

                            // TODO: Edit Dialog, Delete button
                            // Edit button (icon)
                            IconButton(onClick = {
                                Toast.makeText(navController.context, "Edited: $file", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                            }
                            // Delete button (icon)
                            IconButton(onClick = {
                                uploadedFiles.removeAt(index)
                                Toast.makeText(navController.context, "Deleted: $file", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    // Show the dialog if showDialog is true
    UploadDialog(
        showDialog = showDialogUploading,
        onDismiss = { showDialogUploading = false },  // Close the dialog when dismissing
        onSubmit = handleSubmit,  // Submit form data
        onFileSelect = { uri, name ->
            // Handle file URI and name after file is selected
            fileUri = uri
            fileName = name
        }
    )
}

@Composable
fun UploadDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit,
    onFileSelect: (Uri?, String) -> Unit
) {
    // Dialog should be displayed when showDialog is true
    if (showDialog) {
        var fileName by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }

        // File picker launcher
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri: Uri? ->
                // Handle file selection
                if (uri != null) {
                    fileName = uri.lastPathSegment ?: "Unknown file"
                    onFileSelect(uri, fileName)  // Update the fileUri and fileName
                }
            }
        )

        // AlertDialog for form
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Fill in the details") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Button to select file
                    Button(onClick = { launcher.launch("application/*") }
                    ) {
                        Text("Click to upload")
                    }
                    // TODO: Name of file, Description, Group to tag to (dropdown)
                    TextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("File Name") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Handle form submission
                        onSubmit(fileName, description)
                        onDismiss()  // Close the dialog after submission
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}



