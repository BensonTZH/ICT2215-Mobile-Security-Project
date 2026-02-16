package com.example.teacherapp.navigation.admin

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.teacherapp.admin.AdminRepo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(navController: NavController) {
    val context = LocalContext.current
    var users by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var deletingUid by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val reg = AdminRepo.listenManagedUsers(
            onUpdate = { users = it.sortedBy { u -> (u["name"] as? String ?: "").lowercase() } },
            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        )
        onDispose { reg.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Users") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("admin_user_edit/new") }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Create")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(users, key = { (it["uid"] as? String) ?: "" }) { user ->
                val uid = user["uid"] as? String ?: return@items
                val role = user["role"] as? String ?: ""
                ListItem(
                    headlineContent = {
                        Text((user["name"] as? String).orEmpty().ifBlank { "Unnamed" }, fontWeight = FontWeight.SemiBold)
                    },
                    supportingContent = {
                        Column {
                            Text((user["email"] as? String).orEmpty())
                            Text("Role: $role")
                        }
                    },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.clickable { navController.navigate("admin_user_edit/$uid") }
                            )
                            Spacer(modifier = Modifier.padding(2.dp))
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.clickable { deletingUid = uid }
                            )
                        }
                    }
                )
            }
        }
    }

    if (deletingUid != null) {
        val target = deletingUid!!
        AlertDialog(
            onDismissRequest = { deletingUid = null },
            title = { Text("Delete user?") },
            text = { Text("This deletes the user's profile document.") },
            confirmButton = {
                TextButton(onClick = {
                    AdminRepo.deleteManagedUser(
                        uid = target,
                        onSuccess = {
                            deletingUid = null
                            Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show()
                        },
                        onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                    )
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingUid = null }) { Text("Cancel") }
            }
        )
    }
}
