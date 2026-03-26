package com.example.teacherapp.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager

class RemoteControlService : AccessibilityService() {

    companion object {
        var instance: RemoteControlService? = null
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun injectTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, 100L))
                .build(),
            null, null
        )
    }

    fun injectSwipe(x1: Float, y1: Float, x2: Float, y2: Float) {
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, 400L))
                .build(),
            null, null
        )
    }

    private fun findFocusedNode(): AccessibilityNodeInfo? {
        rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { return it }
        rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)?.let { return it }
        // When IME opens it becomes the active window — search all windows
        for (window in windows) {
            val root = window.root ?: continue
            val found = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            if (found != null) return found
        }
        return null
    }

    fun clearText() {
        val focused = findFocusedNode() ?: return
        val bundle = Bundle()
        bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
        focused.recycle()
    }

    fun injectText(text: String) {
        val focused = findFocusedNode() ?: return
        val current = focused.text?.toString() ?: ""
        val bundle = Bundle()
        bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, current + text)
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
        focused.recycle()
    }

    fun injectGlobal(action: String) {
        when (action) {
            "back"     -> performGlobalAction(GLOBAL_ACTION_BACK)
            "home"     -> performGlobalAction(GLOBAL_ACTION_HOME)
            "recents"  -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "keyboard" -> {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                @Suppress("DEPRECATION")
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
            }
        }
    }

    fun injectKey(key: String) {
        val focused = findFocusedNode() ?: return
        val current = focused.text?.toString() ?: ""
        val next = when (key) {
            "Backspace" -> if (current.isNotEmpty()) current.dropLast(1) else { focused.recycle(); return }
            "Enter"     -> current + "\n"
            "Tab"       -> current + "\t"
            else        -> if (key.length == 1) current + key else { focused.recycle(); return }
        }
        val bundle = Bundle()
        bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, next)
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
        focused.recycle()
    }
}
