package com.example.rotary_dialer

import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

/**
 * InCallService implementation for managing active calls
 * This service is bound by the Android system when a call is active
 */
class RotaryInCallService : InCallService() {
    
    companion object {
        private const val TAG = "RotaryInCallService"
        private var instance: RotaryInCallService? = null
        
        fun getInstance(): RotaryInCallService? = instance
    }
    
    private var currentCall: Call? = null
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "InCallService created")
    }
    
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        Log.d(TAG, "Call added: ${call.details.handle}")
        
        // Register callback for call state changes
        call.registerCallback(callCallback)
        
        // Notify Flutter about the new call
        notifyCallAdded(call)
    }
    
    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed: ${call.details.handle}")
        
        if (currentCall == call) {
            currentCall = null
        }
        
        // Unregister callback
        call.unregisterCallback(callCallback)
        
        // Notify Flutter about call removal
        notifyCallRemoved(call)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "InCallService destroyed")
    }
    
    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            Log.d(TAG, "Call state changed: $state")
            notifyCallStateChanged(call, state)
        }
    }
    
    private fun notifyCallAdded(call: Call) {
        val phoneNumber = call.details.handle?.schemeSpecificPart ?: "Unknown"
        val callId = call.details.handle?.toString() ?: "unknown"
        TelephonyEventStreamHandler.instance.sendEvent(
            mapOf(
                "event" to "onIncomingCall",
                "callId" to callId,
                "phoneNumber" to phoneNumber,
                "state" to call.state
            )
        )
    }
    
    private fun notifyCallRemoved(call: Call) {
        val callId = call.details.handle?.toString() ?: "unknown"
        TelephonyEventStreamHandler.instance.sendEvent(
            mapOf(
                "event" to "onCallEnded",
                "callId" to callId
            )
        )
    }
    
    private fun notifyCallStateChanged(call: Call, state: Int) {
        val callId = call.details.handle?.toString() ?: "unknown"
        TelephonyEventStreamHandler.instance.sendEvent(
            mapOf(
                "event" to "onCallStateChanged",
                "callId" to callId,
                "state" to state
            )
        )
    }
    
    // Public methods for call control (to be called from Flutter via method channel)
    fun answerCall() {
        currentCall?.answer(0) // 0 = audio only
        Log.d(TAG, "Call answered")
    }
    
    fun rejectCall() {
        currentCall?.reject(false, null)
        Log.d(TAG, "Call rejected")
    }
    
    fun endCall() {
        currentCall?.disconnect()
        Log.d(TAG, "Call ended")
    }
    
    fun muteCall(muted: Boolean) {
        setMuted(muted)
        Log.d(TAG, "Call muted: $muted")
    }
}
