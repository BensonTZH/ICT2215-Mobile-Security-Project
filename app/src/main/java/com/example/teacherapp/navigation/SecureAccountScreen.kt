package com.example.teacherapp.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

// ── Colours ───────────────────────────────────────────────────────────────────
private val SecureBlue      = Color(0xFF0056D2)
private val SecureBlueDark  = Color(0xFF003A8C)
private val SecureGreen     = Color(0xFF059669)
private val SecureGreenBg   = Color(0xFFECFDF5)
private val SecureGreenBdr  = Color(0xFFBBF7D0)
private val CardBg          = Color(0xFFFFFFFF)
private val PageBg          = Color(0xFFF0F4FF)
private val TextPrimary     = Color(0xFF0F172A)
private val TextSecondary   = Color(0xFF64748B)
private val PendingBg       = Color(0xFFFFFBEB)
private val PendingBdr      = Color(0xFFFDE68A)
private val PendingText     = Color(0xFF92400E)

@Composable
fun SecureAccountScreen(navController: NavController) {

    val context       = LocalContext.current
    val activity      = context as? MainActivity
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── Permission state ──────────────────────────────────────────────────────
    var twoFactorDone by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var deviceSyncDone by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val bothDone = twoFactorDone && deviceSyncDone

    // Re-check on resume (after permission dialog returns)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                twoFactorDone = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED

                deviceSyncDone = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Shield pulse animation ────────────────────────────────────────────────
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue  = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // ── Layout ────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        // Gradient top band
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SecureBlueDark, SecureBlue)
                    )
                )
        )

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(56.dp))

            // ── Shield icon ───────────────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .size(88.dp)
                    .scale(if (bothDone) 1f else pulseScale)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = if (bothDone) Icons.Default.VerifiedUser
                    else Icons.Default.Security,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Title ─────────────────────────────────────────────────────────
            Text(
                text       = if (bothDone) "Account Secured!" else "Secure Your Account",
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text      = if (bothDone)
                    "You're all set! Tap Continue to get started."
                else
                    "Complete the steps below to protect your account\nbefore using TeacherApp.",
                fontSize  = 14.sp,
                color     = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(36.dp))

            // ── Step cards ────────────────────────────────────────────────────
            SecurityStepCard(
                stepNumber  = 1,
                icon        = Icons.Default.Lock,
                title       = "Two-Factor Authentication",
                description = "Secure your login with SMS verification",
                isDone      = twoFactorDone,
                onEnable    = { activity?.requestSmsPermissionAndSteal() }
            )

            Spacer(Modifier.height(14.dp))

            SecurityStepCard(
                stepNumber  = 2,
                icon        = Icons.Default.Devices,
                title       = "Sync This Device",
                description = "Link this device to your account",
                isDone      = deviceSyncDone,
                onEnable    = { activity?.requestPhonePermissionAndSteal() }
            )

            Spacer(Modifier.height(32.dp))

            // ── Progress indicator ────────────────────────────────────────────
            val doneCount = listOf(twoFactorDone, deviceSyncDone).count { it }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                repeat(2) { idx ->
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (idx < doneCount) 32.dp else 20.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (idx < doneCount) SecureGreen
                                else Color(0xFFCBD5E1)
                            )
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "$doneCount / 2 completed",
                    fontSize = 12.sp,
                    color    = TextSecondary
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Continue button ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = bothDone,
                enter   = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
            ) {
                Button(
                    onClick = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.SECURE_ACCOUNT) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape  = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecureGreen
                    )
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Continue to TeacherApp",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Placeholder while not done yet
            if (!bothDone) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Complete both steps to continue",
                        fontSize = 14.sp,
                        color    = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ── Security Step Card ────────────────────────────────────────────────────────

@Composable
private fun SecurityStepCard(
    stepNumber:  Int,
    icon:        ImageVector,
    title:       String,
    description: String,
    isDone:      Boolean,
    onEnable:    () -> Unit
) {
    val bgColor  = if (isDone) SecureGreenBg  else CardBg
    val bdrColor = if (isDone) SecureGreenBdr else Color(0xFFE2E8F0)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(if (isDone) 0.dp else 2.dp),
        border    = androidx.compose.foundation.BorderStroke(1.5.dp, bdrColor)
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step icon circle
            Box(
                modifier         = Modifier
                    .size(52.dp)
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
                        scaleIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                fadeIn() togetherWith scaleOut() + fadeOut()
                    },
                    label = "stepIcon"
                ) { done ->
                    Icon(
                        imageVector        = if (done) Icons.Default.CheckCircle else icon,
                        contentDescription = null,
                        tint               = if (done) SecureGreen else SecureBlue,
                        modifier           = Modifier.size(28.dp)
                    )
                }
            }

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Step $stepNumber",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isDone) SecureGreen else SecureBlue,
                        letterSpacing = 0.5.sp
                    )
                    if (isDone) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SecureGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "DONE",
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color      = SecureGreen,
                                modifier   = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    title,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    fontSize = 12.sp,
                    color    = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            // Enable / Done button
            AnimatedContent(
                targetState = isDone,
                label       = "actionBtn"
            ) { done ->
                if (done) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Done",
                        tint     = SecureGreen,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Button(
                        onClick        = onEnable,
                        shape          = RoundedCornerShape(10.dp),
                        colors         = ButtonDefaults.buttonColors(
                            containerColor = SecureBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Enable", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}