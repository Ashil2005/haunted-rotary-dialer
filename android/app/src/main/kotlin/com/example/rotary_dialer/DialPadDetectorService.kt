package com.example.rotary_dialer

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Accessibility Service that detects when a dial pad appears on screen
 * and notifies the Overlay Service to show the rotary dial overlay
 */
class DialPadDetectorService : AccessibilityService() {
    
    companion object {
        private const val TAG = "DialPadDetector"
        const val ACTION_DIAL_PAD_DETECTED = "com.example.rotary_dialer.DIAL_PAD_DETECTED"
        const val ACTION_DIAL_PAD_HIDDEN = "com.example.rotary_dialer.DIAL_PAD_HIDDEN"
        const val EXTRA_BOUNDS = "bounds"
        const val EXTRA_PACKAGE_NAME = "packageName"
        
        // Common dialer package names
        private val DIALER_PACKAGES = setOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.contacts",
            "com.google.android.contacts"
        )
        
        // Resource IDs commonly used for dial pad number field
        private val DIAL_PAD_FIELD_IDS = setOf(
            "digits",
            "digit",
            "phone_number",
            "phoneNumber",
            "number_field"
        )
        
        var instance: DialPadDetectorService? = null
    }
    
    private var isDialPadVisible = false
    private var currentInputField: AccessibilityNodeInfo? = null
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility Service connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                checkForDialPad()
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            currentInputField?.recycle()
            currentInputField = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up input field", e)
        }
        instance = null
        Log.d(TAG, "Accessibility Service destroyed")
    }
    
    /**
     * Check if a dial pad is currently visible
     */
    private fun checkForDialPad() {
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Log.w(TAG, "Root node not available")
            return
        }
        
        try {
            val packageName = rootNode.packageName?.toString() ?: ""
            
            // Check for emergency dialer - don't overlay on emergency calls
            if (isEmergencyDialer(packageName)) {
                Log.d(TAG, "Emergency dialer detected, skipping overlay")
                if (isDialPadVisible) {
                    hideDialPad()
                }
                return
            }
            
            // Check if this is a dialer app
            if (!isDialerPackage(packageName)) {
                if (isDialPadVisible) {
                    hideDialPad()
                }
                return
            }
            
            // Look for dial pad indicators
            val numberField = findNumberInputField(rootNode)
            if (numberField != null) {
                val bounds = getNodeBounds(numberField)
                if (!isDialPadVisible) {
                    showDialPad(bounds, packageName)
                }
                currentInputField = numberField
            } else {
                if (isDialPadVisible) {
                    hideDialPad()
                }
                currentInputField?.recycle()
                currentInputField = null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception checking for dial pad", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Invalid state when checking for dial pad", e)
            if (isDialPadVisible) {
                hideDialPad()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error checking for dial pad", e)
        } finally {
            try {
                rootNode.recycle()
            } catch (e: Exception) {
                Log.e(TAG, "Error recycling root node", e)
            }
        }
    }
    
    /**
     * Check if this is an emergency dialer
     */
    private fun isEmergencyDialer(packageName: String): Boolean {
        return packageName.contains("emergency", ignoreCase = true)
    }
    
    /**
     * Check if package name is a known dialer app
     */
    private fun isDialerPackage(packageName: String): Boolean {
        return DIALER_PACKAGES.any { packageName.contains(it, ignoreCase = true) }
    }
    
    /**
     * Find the dial pad button area (not just the number input field)
     */
    private fun findNumberInputField(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // First, try to find the dial pad button container
        // Look for ViewGroup that contains multiple buttons (0-9, *, #)
        val dialPadContainer = findDialPadContainer(node)
        if (dialPadContainer != null) {
            return dialPadContainer
        }
        
        // Fallback: Try to find by resource ID
        for (fieldId in DIAL_PAD_FIELD_IDS) {
            val field = findNodeByViewId(node, fieldId)
            if (field != null) {
                return field
            }
        }
        
        // Last resort: Try to find by class name (EditText)
        val editText = findNodeByClassName(node, "android.widget.EditText")
        if (editText != null && editText.isEditable) {
            return editText
        }
        
        return null
    }
    
    /**
     * Find the dial pad button container (the grid of number buttons)
     */
    private fun findDialPadContainer(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Look for a ViewGroup that contains multiple button children
        if (node.childCount >= 12) { // Dial pad typically has 12 buttons (0-9, *, #)
            var buttonCount = 0
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                if (child.className?.contains("Button") == true) {
                    buttonCount++
                }
                child.recycle()
            }
            
            if (buttonCount >= 10) { // At least 10 number buttons
                return node
            }
        }
        
        // Recursively search children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findDialPadContainer(child)
            if (result != null) {
                return result
            }
            child.recycle()
        }
        
        return null
    }
    
    /**
     * Find node by view resource ID
     */
    private fun findNodeByViewId(node: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? {
        try {
            if (node.viewIdResourceName?.contains(viewId, ignoreCase = true) == true) {
                return node
            }
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    val result = findNodeByViewId(child, viewId)
                    if (result != null) {
                        return result
                    }
                } finally {
                    if (child != node) {
                        child.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding node by view ID: $viewId", e)
        }
        
        return null
    }
    
    /**
     * Find node by class name
     */
    private fun findNodeByClassName(node: AccessibilityNodeInfo, className: String): AccessibilityNodeInfo? {
        try {
            if (node.className?.toString() == className) {
                return node
            }
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    val result = findNodeByClassName(child, className)
                    if (result != null) {
                        return result
                    }
                } finally {
                    if (child != node) {
                        child.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding node by class name: $className", e)
        }
        
        return null
    }
    
    /**
     * Get bounds of a node
     */
    private fun getNodeBounds(node: AccessibilityNodeInfo): Rect {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect
    }
    
    /**
     * Notify that dial pad is visible
     */
    private fun showDialPad(bounds: Rect, packageName: String) {
        try {
            isDialPadVisible = true
            Log.d(TAG, "Dial pad detected in $packageName at $bounds")
            
            val intent = Intent(ACTION_DIAL_PAD_DETECTED).apply {
                putExtra(EXTRA_BOUNDS, intArrayOf(bounds.left, bounds.top, bounds.right, bounds.bottom))
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                setPackage(applicationContext.packageName)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error broadcasting dial pad detected", e)
        }
    }
    
    /**
     * Notify that dial pad is hidden
     */
    private fun hideDialPad() {
        try {
            isDialPadVisible = false
            Log.d(TAG, "Dial pad hidden")
            
            val intent = Intent(ACTION_DIAL_PAD_HIDDEN).apply {
                setPackage(applicationContext.packageName)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error broadcasting dial pad hidden", e)
        }
    }
    
    /**
     * Get the current input field for digit injection
     */
    fun getCurrentInputField(): AccessibilityNodeInfo? {
        return currentInputField
    }
}
