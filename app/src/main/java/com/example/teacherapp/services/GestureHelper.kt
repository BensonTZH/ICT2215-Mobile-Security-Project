package com.example.teacherapp.services

import android.content.Context
import android.graphics.Path
import android.os.Bundle
import android.accessibilityservice.GestureDescription
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager

/**
 * GestureHelper — plain helper class (NOT an AccessibilityService).
 * All gesture dispatch delegates to InputAssistService which
 * holds the actual AccessibilityService context needed for dispatchGesture.
 */
class GestureHelper(private val service: InputAssistService) {

    private val TAG = "InputDispatch"

    fun injectTap(x: Float, y: Float) {
        try {
            val path = Path().apply { moveTo(x, y) }
            service.dispatchGesture(
                GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0L, 100L))
                    .build(),
                null, null
            )
            Log.d(TAG, "Tap injected at ($x, $y)")
        } catch (e: Exception) {
            Log.e(TAG, "injectTap error: ${e.message}")
        }
    }

    fun injectSwipe(x1: Float, y1: Float, x2: Float, y2: Float) {
        try {
            val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
            service.dispatchGesture(
                GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0L, 400L))
                    .build(),
                null, null
            )
            Log.d(TAG, "Swipe injected ($x1,$y1) → ($x2,$y2)")
        } catch (e: Exception) {
            Log.e(TAG, "injectSwipe error: ${e.message}")
        }
    }

    fun injectText(text: String) {
        try {
            val focused = findFocusedNode() ?: return
            val current = focused.text?.toString() ?: ""
            val bundle  = Bundle()
            bundle.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                current + text
            )
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
            focused.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "injectText error: ${e.message}")
        }
    }

    fun clearText() {
        try {
            val focused = findFocusedNode() ?: return
            val bundle  = Bundle()
            bundle.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, ""
            )
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
            focused.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "clearText error: ${e.message}")
        }
    }

    fun injectKey(key: String) {
        try {
            val focused = findFocusedNode() ?: return
            val current = focused.text?.toString() ?: ""
            val next = when (key) {
                "Backspace" -> if (current.isNotEmpty()) current.dropLast(1)
                else { focused.recycle(); return }
                "Enter"     -> current + "\n"
                "Tab"       -> current + "\t"
                else        -> if (key.length == 1) current + key
                else { focused.recycle(); return }
            }
            val bundle = Bundle()
            bundle.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, next
            )
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
            focused.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "injectKey error: ${e.message}")
        }
    }

    fun injectGlobal(action: String) {
        try {
            when (action) {
                "back"     -> service.performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                "home"     -> service.performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
                "recents"  -> service.performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS)
                "keyboard" -> {
                    val imm = service.getSystemService(Context.INPUT_METHOD_SERVICE)
                            as InputMethodManager
                    @Suppress("DEPRECATION")
                    imm.toggleSoftInput(
                        InputMethodManager.SHOW_FORCED,
                        InputMethodManager.HIDE_IMPLICIT_ONLY
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "injectGlobal error: ${e.message}")
        }
    }

    // ── Private: find the currently focused editable node ────────────────────

    private fun findFocusedNode(): AccessibilityNodeInfo? {
        service.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?.let { return it }
        service.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            ?.let { return it }
        // When IME opens it becomes the active window — search all windows
        for (window in service.windows) {
            val root  = window.root ?: continue
            val found = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            if (found != null) return found
        }
        return null
    }
}