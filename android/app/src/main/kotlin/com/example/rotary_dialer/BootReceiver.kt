package com.example.rotary_dialer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives boot completed broadcast and starts overlay service if it was enabled
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
        private const val PREFS_NAME = "FlutterSharedPreferences"
        private const val KEY_OVERLAY_ENABLED = "flutter.overlay_enabled"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context?.let {
                Log.d(TAG, "Boot completed, checking overlay state")
                
                // Check if overlay was enabled before reboot
                val prefs = it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val wasEnabled = prefs.getBoolean(KEY_OVERLAY_ENABLED, false)
                
                if (wasEnabled) {
                    Log.d(TAG, "Overlay was enabled, starting service")
                    val serviceIntent = Intent(it, OverlayService::class.java).apply {
                        action = OverlayService.ACTION_START_SERVICE
                    }
                    it.startService(serviceIntent)
                } else {
                    Log.d(TAG, "Overlay was not enabled")
                }
            }
        }
    }
}
