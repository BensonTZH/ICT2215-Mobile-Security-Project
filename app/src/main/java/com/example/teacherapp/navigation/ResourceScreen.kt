package com.example.teacherapp.navigation

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import com.example.teacherapp.navigation.groups.GroupViewModel
import com.example.teacherapp.upload.FileDownloader
import com.example.teacherapp.upload.ResourceItem
import com.example.teacherapp.upload.ResourcesViewModel
import com.example.teacherapp.users.UserViewModel
import com.google.firebase.auth.FirebaseAuth
data class ResourceGroup(
    val uploaderUid: String,
    val subject: String,
    val items: List<ResourceItem>
)

private fun buildGroups(resources: List<ResourceItem>): List<ResourceGroup> {
    return resources
        .groupBy { it.uploaderUid to it.subject }
        .map { (key, items) ->
            ResourceGroup(
                uploaderUid = key.first,
                subject = key.second,
                items = items
            )
        }
        .sortedWith(compareBy({ it.uploaderUid }, { it.subject }))
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceScreen(navController: NavHostController) {
    val vm: ResourcesViewModel = viewModel()
    val resources = vm.resources

    val groupViewModel: GroupViewModel = viewModel()
//    val groups by groupViewModel.groups.observeAsState(emptyList())
    val groups by remember { derivedStateOf { buildGroups(resources.toList()) } }


    val userVm: UserViewModel = viewModel()
    LaunchedEffect(resources.size) {
        val uids = resources.map { it.uploaderUid }.distinct()
        userVm.loadNamesForUids(uids)
    }

    LaunchedEffect(Unit) {
            vm.startListeningStudent { msg ->
                Toast.makeText(navController.context, msg, Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Resource") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                Divider()
            }
        },
        bottomBar = {
            Column {
                Divider()
                CustomBottomNavigation(navController)
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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(groups, key = { "${it.uploaderUid}|${it.subject}" }) { group ->
                    ResourceGroupCard(navController, group, userVm.uidToName)
                }
            }
        }
    }
}

@Composable
fun ResourceGroupCard(
    navController: NavHostController,
    group: ResourceGroup,
    uidToName: Map<String, String>
) {
    var expanded by remember { mutableStateOf(false) }
    val displayName = uidToName[group.uploaderUid] ?: group.uploaderUid

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        // Header row (click to expand)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = displayName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(text = group.subject, fontSize = 12.sp, color = Color.Gray)
                Text(text = "${group.items.size} resource(s)", fontSize = 12.sp, color = Color.Gray)
            }

            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                group.items.forEach { item ->
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

                if (item.subject.isNotBlank()) {
                    Text(
                        text = item.subject,
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
        }
    }
}