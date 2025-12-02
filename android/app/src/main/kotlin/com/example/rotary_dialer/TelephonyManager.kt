package com.example.rotary_dialer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager as AndroidTelecomManager
import android.util.Log

class TelephonyManager(private val context: Context) {
    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as AndroidTelecomManager
    
    companion object {
        private const val TAG = "TelephonyManager"
    }
    
    /**
     * Place an outgoing call using the Android Telecom API
     */
    fun placeCall(phoneNumber: String) {
        try {
            val uri = Uri.fromParts("tel", phoneNumber, null)
            val extras = Bundle()
            
            // Check if we have CALL_PHONE permission
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) 
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    telecomManager.placeCall(uri, extras)
                } else {
                    Log.e(TAG, "CALL_PHONE permission not granted")
                    // Fall back to ACTION_DIAL intent
                    val dialIntent = Intent(Intent.ACTION_DIAL, uri)
                    dialIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(dialIntent)
                }
            } else {
                telecomManager.placeCall(uri, extras)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error placing call: ${e.message}", e)
        }
    }
    
    /**
     * Check if a phone number is an emergency number
     */
    fun isEmergencyNumber(phoneNumber: String): Boolean {
        return try {
            // Check common emergency numbers
            phoneNumber in listOf("911", "112", "999", "000", "110", "119", "108")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking emergency number: ${e.message}", e)
            false
        }
    }
}
