package com.example.teacherapp.navigation

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.teacherapp.upload.FileDownloader
import com.example.teacherapp.upload.ResourceItem
import com.example.teacherapp.upload.ResourcesViewModel

/**
 * Read-only Resources screen:
 * - Listens to Firestore via ResourcesViewModel (same as UploadScreen)
 * - Displays Resource cards (same Card/Row/Column layout)
 * - No Edit/Delete actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceScreen(navController: NavHostController) {
    val vm: ResourcesViewModel = viewModel()
    val resources = vm.resources

    LaunchedEffect(Unit) {
        vm.startListening { msg ->
            Toast.makeText(navController.context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
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
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items = resources, key = { it.id }) { item ->
                    ResourceCardDownloadConfirm(navController = navController, item = item)
                }
            }
        }
    }
}

@Composable
fun ResourceCardDownloadConfirm(
    navController: NavHostController,
    item: ResourceItem
) {
    var showConfirm by remember { mutableStateOf(false) }

    // Confirmation dialog
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Download file?") },
            text = { Text("Download “${item.fileName}”?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        FileDownloader.downloadToDownloads(
                            context = navController.context,
                            url = item.cloudinaryUrl,
                            fileName = item.fileName
                        )
                    }
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clickable card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showConfirm = true },
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                )

                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}