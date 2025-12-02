package com.example.rotary_dialer

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * CallScreeningService implementation for screening incoming calls
 * Available on Android 10+ (API 29+)
 */
@RequiresApi(Build.VERSION_CODES.Q)
class RotaryCallScreeningService : CallScreeningService() {
    
    companion object {
        private const val TAG = "RotaryCallScreeningService"
    }
    
    override fun onScreenCall(callDetails: Call.Details) {
        Log.d(TAG, "Screening call: ${callDetails.handle}")
        
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: "Unknown"
        val callId = callDetails.handle?.toString() ?: "unknown"
        
        // Notify Flutter about the incoming call
        TelephonyEventStreamHandler.instance.sendEvent(
            mapOf(
                "event" to "onIncomingCall",
                "callId" to callId,
                "phoneNumber" to phoneNumber,
                "callerName" to callDetails.callerDisplayName
            )
        )
        
        // Allow all calls by default (no screening/blocking)
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        
        respondToCall(callDetails, response)
    }
}
