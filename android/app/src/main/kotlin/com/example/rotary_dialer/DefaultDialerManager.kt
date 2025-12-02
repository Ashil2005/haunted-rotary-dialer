package com.example.rotary_dialer

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log

class DefaultDialerManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DefaultDialerManager"
        const val REQUEST_CODE_SET_DEFAULT_DIALER = 1001
    }
    
    /**
     * Check if this app is currently the default dialer
     */
    fun isDefaultDialer(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ uses RoleManager
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
            } else {
                // Android 8-9 uses TelecomManager
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                telecomManager.defaultDialerPackage == context.packageName
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking default dialer status: ${e.message}", e)
            false
        }
    }
    
    /**
     * Request to set this app as the default dialer
     */
    fun requestDefaultDialerRole(activity: Activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ uses RoleManager
                val roleManager = activity.getSystemService(Context.ROLE_SERVICE) as RoleManager
                if (!roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    Log.e(TAG, "Dialer role is not available on this device")
                    return
                }
                
                if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                    activity.startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER)
                }
            } else {
                // Android 8-9 uses TelecomManager
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                if (telecomManager.defaultDialerPackage != context.packageName) {
                    val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                    activity.startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting default dialer role: ${e.message}", e)
        }
    }
    
    /**
     * Open system settings for default apps
     */
    fun openDefaultDialerSettings() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            } else {
                Intent(Settings.ACTION_SETTINGS)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening default dialer settings: ${e.message}", e)
        }
    }
    
    /**
     * Handle activity result from default dialer request
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == REQUEST_CODE_SET_DEFAULT_DIALER) {
            val isNowDefault = isDefaultDialer()
            Log.d(TAG, "Default dialer request result: $isNowDefault")
            
            // Notify Flutter about the status change
            TelephonyEventStreamHandler.instance.sendEvent(
                mapOf(
                    "event" to "onDefaultDialerStatusChanged",
                    "isDefault" to isNowDefault
                )
            )
        }
    }
}
