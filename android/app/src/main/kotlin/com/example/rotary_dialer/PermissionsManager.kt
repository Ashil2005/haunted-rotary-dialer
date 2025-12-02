package com.example.rotary_dialer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionsManager(private val context: Context) {
    
    companion object {
        private const val TAG = "PermissionsManager"
        const val REQUEST_CODE_PERMISSIONS = 1002
        
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG
        )
        
        val OPTIONAL_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            arrayOf(
                Manifest.permission.ANSWER_PHONE_CALLS,
                Manifest.permission.READ_PHONE_NUMBERS
            )
        } else {
            emptyArray()
        }
    }
    
    /**
     * Check status of all permissions
     */
    fun checkPermissions(): Map<String, Boolean> {
        val permissionStatus = mutableMapOf<String, Boolean>()
        
        REQUIRED_PERMISSIONS.forEach { permission ->
            permissionStatus[permission] = isPermissionGranted(permission)
        }
        
        OPTIONAL_PERMISSIONS.forEach { permission ->
            permissionStatus[permission] = isPermissionGranted(permission)
        }
        
        return permissionStatus
    }
    
    /**
     * Check if a specific permission is granted
     */
    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Request multiple permissions
     */
    fun requestPermissions(activity: Activity, permissions: List<String>) {
        val permissionsToRequest = permissions.filter { !isPermissionGranted(it) }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                REQUEST_CODE_PERMISSIONS
            )
        } else {
            Log.d(TAG, "All requested permissions already granted")
        }
    }
    
    /**
     * Handle permission request results
     */
    fun handlePermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val results = mutableMapOf<String, Boolean>()
            
            permissions.forEachIndexed { index, permission ->
                results[permission] = grantResults[index] == PackageManager.PERMISSION_GRANTED
            }
            
            Log.d(TAG, "Permission results: $results")
            
            // Notify Flutter about permission changes
            TelephonyEventStreamHandler.instance.sendEvent(
                mapOf(
                    "event" to "onPermissionsChanged",
                    "permissions" to results
                )
            )
        }
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun hasAllRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { isPermissionGranted(it) }
    }
}
