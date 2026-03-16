package com.example.teacherapp.navigation.admin

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.teacherapp.admin.AdminRepo
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserFormScreen(navController: NavController, uidArg: String) {
    val context = LocalContext.current
    val isCreate = uidArg == "new"

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("student") }
    var roleMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uidArg) {
        if (!isCreate) {
            FirebaseFirestore.getInstance().collection("users").document(uidArg).get()
                .addOnSuccessListener { doc ->
                    name = doc.getString("name") ?: ""
                    email = doc.getString("email") ?: ""
                    role = doc.getString("role") ?: "student"
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCreate) "Create User" else "Edit User") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                enabled = isCreate,
                modifier = Modifier.fillMaxWidth()
            )

            if (isCreate) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            ExposedDropdownMenuBox(expanded = roleMenu, onExpandedChange = { roleMenu = !roleMenu }) {
                OutlinedTextField(
                    value = role,
                    onValueChange = {},
                    label = { Text("Role") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleMenu) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(expanded = roleMenu, onDismissRequest = { roleMenu = false }) {
                    listOf("student", "teacher").forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = {
                                role = opt
                                roleMenu = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (isCreate) {
                        AdminRepo.createManagedUserWithAuth(
                            name = name,
                            email = email,
                            password = password,
                            role = role,
                            onSuccess = {
                                Toast.makeText(context, "User created", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    } else {
                        AdminRepo.upsertManagedUser(
                            uid = uidArg,
                            name = name,
                            email = email,
                            role = role,
                            onSuccess = {
                                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    }
                },
                enabled = if (isCreate) {
                    name.isNotBlank() && email.isNotBlank() && password.length >= 6
                } else {
                    name.isNotBlank() && email.isNotBlank()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isCreate) "Create User" else "Save User")
            }
        }
    }
}
