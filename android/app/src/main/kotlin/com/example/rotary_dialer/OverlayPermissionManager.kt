package com.example.rotary_dialer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log

/**
 * Manages permissions required for the rotary dial overlay feature
 */
class OverlayPermissionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "OverlayPermissionMgr"
    }
    
    /**
     * Check if overlay permission (draw over other apps) is granted
     */
    fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            // Permission not required for API < 23
            true
        }
    }
    
    /**
     * Request overlay permission by opening system settings
     */
    fun requestOverlayPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            try {
                activity.startActivity(intent)
                Log.d(TAG, "Opened overlay permission settings")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open overlay permission settings", e)
            }
        }
    }
    
    /**
     * Check if accessibility service is enabled for this app
     */
    fun checkAccessibilityPermission(): Boolean {
        val serviceName = "${context.packageName}/${DialPadDetectorService::class.java.canonicalName}"
        
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        if (enabledServices.isNullOrEmpty()) {
            return false
        }
        
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(serviceName, ignoreCase = true)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Request accessibility permission by opening accessibility settings
     */
    fun requestAccessibilityPermission(activity: Activity) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        try {
            activity.startActivity(intent)
            Log.d(TAG, "Opened accessibility settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open accessibility settings", e)
        }
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun checkAllPermissions(): Boolean {
        val hasOverlay = checkOverlayPermission()
        val hasAccessibility = checkAccessibilityPermission()
        
        Log.d(TAG, "Overlay permission: $hasOverlay, Accessibility permission: $hasAccessibility")
        
        return hasOverlay && hasAccessibility
    }
}
