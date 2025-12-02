package com.example.rotary_dialer

import android.os.Handler
import android.os.Looper
import io.flutter.plugin.common.EventChannel

/**
 * Singleton event stream handler for sending events from Android to Flutter
 */
class TelephonyEventStreamHandler private constructor() : EventChannel.StreamHandler {
    
    private var eventSink: EventChannel.EventSink? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    companion object {
        val instance = TelephonyEventStreamHandler()
    }
    
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
    }
    
    override fun onCancel(arguments: Any?) {
        eventSink = null
    }
    
    /**
     * Send an event to Flutter
     * Must be called on the main thread
     */
    fun sendEvent(event: Map<String, Any?>) {
        mainHandler.post {
            eventSink?.success(event)
        }
    }
    
    /**
     * Send an error to Flutter
     */
    fun sendError(errorCode: String, errorMessage: String?, errorDetails: Any?) {
        mainHandler.post {
            eventSink?.error(errorCode, errorMessage, errorDetails)
        }
    }
}
