package com.example.rotary_dialer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

/**
 * Foreground service that manages the rotary dial overlay window
 */
class OverlayService : Service() {
    
    companion object {
        private const val TAG = "OverlayService"
        const val ACTION_START_SERVICE = "com.example.rotary_dialer.START_OVERLAY_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.rotary_dialer.STOP_OVERLAY_SERVICE"
        const val ACTION_SHOW_OVERLAY = "com.example.rotary_dialer.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.example.rotary_dialer.HIDE_OVERLAY"
        
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "rotary_overlay_channel"
    }
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayVisible = false
    private var dialPadBounds: Rect? = null
    private var inputInjector: InputInjector? = null
    
    // Number tracking
    private var dialedNumber = StringBuilder()
    private var numberDisplay: android.widget.TextView? = null
    
    private val dialPadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                DialPadDetectorService.ACTION_DIAL_PAD_DETECTED -> {
                    val boundsArray = intent.getIntArrayExtra(DialPadDetectorService.EXTRA_BOUNDS)
                    val packageName = intent.getStringExtra(DialPadDetectorService.EXTRA_PACKAGE_NAME)
                    
                    if (boundsArray != null && boundsArray.size >= 4) {
                        dialPadBounds = Rect(boundsArray[0], boundsArray[1], boundsArray[2], boundsArray[3])
                        Log.d(TAG, "Dial pad detected in $packageName at $dialPadBounds")
                        showOverlay()
                    }
                }
                DialPadDetectorService.ACTION_DIAL_PAD_HIDDEN -> {
                    Log.d(TAG, "Dial pad hidden")
                    dialPadBounds = null
                    hideOverlay()
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Overlay Service created")
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Register broadcast receiver for dial pad detection
        val filter = IntentFilter().apply {
            addAction(DialPadDetectorService.ACTION_DIAL_PAD_DETECTED)
            addAction(DialPadDetectorService.ACTION_DIAL_PAD_HIDDEN)
        }
        registerReceiver(dialPadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        
        // Initialize input injector
        DialPadDetectorService.instance?.let { service ->
            inputInjector = InputInjector(service)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startForegroundService()
                // DON'T show overlay immediately - wait for dial pad detection
                Log.d(TAG, "Service started, waiting for dial pad detection")
            }
            ACTION_STOP_SERVICE -> {
                stopService()
            }
            ACTION_SHOW_OVERLAY -> {
                showOverlay()
            }
            ACTION_HIDE_OVERLAY -> {
                hideOverlay()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Overlay Service destroyed")
        
        try {
            unregisterReceiver(dialPadReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        
        hideOverlay()
        inputInjector = null
    }
    
    /**
     * Start service in foreground with notification
     */
    private fun startForegroundService() {
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "Overlay Service started in foreground")
    }
    
    /**
     * Stop the service
     */
    private fun stopService() {
        hideOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rotary Dial Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when rotary dial overlay is active"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create foreground service notification
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_dialer)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    

    
    /**
     * Show the overlay window
     */
    private fun showOverlay() {
        if (isOverlayVisible || overlayView != null) {
            Log.d(TAG, "Overlay already visible")
            return
        }
        
        // Check if we have dial pad bounds
        val bounds = dialPadBounds
        if (bounds == null) {
            Log.e(TAG, "Cannot show overlay: No dial pad bounds available")
            return
        }
        
        try {
            // Inflate native Android layout
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as android.view.LayoutInflater
            overlayView = inflater.inflate(R.layout.overlay_rotary_dial, null)
            
            // Get number display
            numberDisplay = overlayView?.findViewById(R.id.number_display)
            
            // Setup rotary dial view
            val rotaryDialView = overlayView?.findViewById<RotaryDialView>(R.id.rotary_dial_view)
            rotaryDialView?.onDigitSelected = { digit ->
                Log.d(TAG, "Digit selected: $digit")
                
                // Ensure InputInjector is initialized
                if (inputInjector == null) {
                    DialPadDetectorService.instance?.let { service ->
                        inputInjector = InputInjector(service)
                    }
                }
                
                // Add digit to our number
                dialedNumber.append(digit)
                updateNumberDisplay()
                
                // Inject digit into system dialer
                val success = inputInjector?.injectDigit(digit.toString()) ?: false
                Log.d(TAG, "Digit injection result: $success")
            }
            
            // Setup delete button
            val deleteButton = overlayView?.findViewById<android.widget.Button>(R.id.delete_button)
            deleteButton?.setOnClickListener {
                Log.d(TAG, "Delete button clicked")
                
                if (dialedNumber.isNotEmpty()) {
                    // Remove last digit from our number
                    dialedNumber.deleteCharAt(dialedNumber.length - 1)
                    updateNumberDisplay()
                    
                    // Delete from system dialer
                    inputInjector?.deleteLastDigit()
                }
            }
            
            // Setup call button
            val callButton = overlayView?.findViewById<android.widget.Button>(R.id.call_button)
            callButton?.setOnClickListener {
                Log.d(TAG, "Call button clicked")
                
                // Ensure InputInjector is initialized
                if (inputInjector == null) {
                    DialPadDetectorService.instance?.let { service ->
                        inputInjector = InputInjector(service)
                    }
                }
                
                // Click the system dialer's call button using accessibility
                // This will use whatever number is already in the system dialer
                val success = inputInjector?.clickCallButton() ?: false
                
                if (success) {
                    Log.d(TAG, "✅ Call button clicked successfully via accessibility")
                    // Clear our overlay number display after successful call
                    dialedNumber.clear()
                    updateNumberDisplay()
                } else {
                    Log.e(TAG, "❌ Failed to click system call button via accessibility")
                }
            }
            
            // Setup close button
            val closeButton = overlayView?.findViewById<android.widget.ImageButton>(R.id.close_button)
            closeButton?.setOnClickListener {
                Log.d(TAG, "Close button clicked")
                hideOverlay()
            }
            
            // Window parameters - full screen with transparent background
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }
            
            // Add view to window manager
            windowManager?.addView(overlayView, params)
            isOverlayVisible = true
            
            Log.d(TAG, "Native rotary dial overlay shown")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied to show overlay", e)
            showErrorNotification("Overlay permission denied")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Invalid state when showing overlay", e)
            showErrorNotification("Cannot show overlay: Invalid state")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error showing overlay", e)
            showErrorNotification("Failed to show overlay")
        }
    }
    
    /**
     * Hide the overlay window
     */
    private fun hideOverlay() {
        if (!isOverlayVisible || overlayView == null) {
            return
        }
        
        try {
            // Cleanup rotary dial view
            val rotaryDialView = overlayView?.findViewById<RotaryDialView>(R.id.rotary_dial_view)
            rotaryDialView?.cleanup()
            
            windowManager?.removeView(overlayView)
            overlayView = null
            numberDisplay = null
            isOverlayVisible = false
            
            // Clear dialed number
            dialedNumber.clear()
            
            Log.d(TAG, "Overlay hidden")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "View not attached to window manager", e)
            overlayView = null
            numberDisplay = null
            isOverlayVisible = false
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding overlay", e)
            overlayView = null
            numberDisplay = null
            isOverlayVisible = false
        }
    }
    
    /**
     * Update the number display TextView
     */
    private fun updateNumberDisplay() {
        numberDisplay?.text = if (dialedNumber.isEmpty()) {
            ""
        } else {
            dialedNumber.toString()
        }
    }
    

    
    /**
     * Show error notification to user
     */
    private fun showErrorNotification(message: String) {
        try {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Rotary Dial Error")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(NOTIFICATION_ID + 1, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show error notification", e)
        }
    }
}
