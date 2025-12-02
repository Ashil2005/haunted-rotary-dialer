package com.example.rotary_dialer

import android.content.Intent
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.EventChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.rotarydialer/telephony"
    private val EVENT_CHANNEL = "com.rotarydialer/telephony_events"
    private val OVERLAY_CHANNEL = "com.example.rotary_dialer/overlay"
    
    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private lateinit var overlayMethodChannel: MethodChannel
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var defaultDialerManager: DefaultDialerManager
    private lateinit var overlayPermissionManager: OverlayPermissionManager
    private var inputInjector: InputInjector? = null
    
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        // Initialize managers
        telephonyManager = TelephonyManager(this)
        permissionsManager = PermissionsManager(this)
        defaultDialerManager = DefaultDialerManager(this)
        overlayPermissionManager = OverlayPermissionManager(this)
        
        // Initialize InputInjector when accessibility service is available
        DialPadDetectorService.instance?.let { service ->
            inputInjector = InputInjector(service)
        }
        
        // Set up method channel
        methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "requestDefaultDialerRole" -> {
                    defaultDialerManager.requestDefaultDialerRole(this)
                    result.success(true)
                }
                "isDefaultDialer" -> {
                    val isDefault = defaultDialerManager.isDefaultDialer()
                    result.success(isDefault)
                }
                "openDefaultDialerSettings" -> {
                    defaultDialerManager.openDefaultDialerSettings()
                    result.success(null)
                }
                "placeCall" -> {
                    val phoneNumber = call.argument<String>("phoneNumber")
                    if (phoneNumber != null) {
                        telephonyManager.placeCall(phoneNumber)
                        result.success(null)
                    } else {
                        result.error("INVALID_ARGUMENT", "Phone number is required", null)
                    }
                }
                "checkPermissions" -> {
                    val permissions = permissionsManager.checkPermissions()
                    result.success(permissions)
                }
                "requestPermissions" -> {
                    val permissionsList = call.argument<List<String>>("permissions")
                    if (permissionsList != null) {
                        permissionsManager.requestPermissions(this, permissionsList)
                        result.success(true)
                    } else {
                        result.error("INVALID_ARGUMENT", "Permissions list is required", null)
                    }
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
        
        // Set up overlay method channel
        overlayMethodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, OVERLAY_CHANNEL)
        overlayMethodChannel.setMethodCallHandler { call, result ->
            try {
                when (call.method) {
                    "enableOverlay" -> {
                        try {
                            val hasPermissions = overlayPermissionManager.checkAllPermissions()
                            if (hasPermissions) {
                                startOverlayService()
                                result.success(true)
                            } else {
                                result.success(false)
                            }
                        } catch (e: Exception) {
                            result.error("SERVICE_ERROR", "Failed to start overlay service: ${e.message}", null)
                        }
                    }
                    "disableOverlay" -> {
                        try {
                            stopOverlayService()
                            result.success(true)
                        } catch (e: Exception) {
                            result.error("SERVICE_ERROR", "Failed to stop overlay service: ${e.message}", null)
                        }
                    }
                    "checkPermissions" -> {
                        try {
                            val hasPermissions = overlayPermissionManager.checkAllPermissions()
                            result.success(hasPermissions)
                        } catch (e: Exception) {
                            result.error("PERMISSION_ERROR", "Failed to check permissions: ${e.message}", null)
                        }
                    }
                    "requestPermissions" -> {
                        try {
                            // Request overlay permission
                            if (!overlayPermissionManager.checkOverlayPermission()) {
                                overlayPermissionManager.requestOverlayPermission(this)
                            }
                            // Request accessibility permission
                            if (!overlayPermissionManager.checkAccessibilityPermission()) {
                                overlayPermissionManager.requestAccessibilityPermission(this)
                            }
                            result.success(null)
                        } catch (e: Exception) {
                            result.error("PERMISSION_ERROR", "Failed to request permissions: ${e.message}", null)
                        }
                    }
                    "sendDigit" -> {
                        try {
                            val digit = call.argument<String>("digit")
                            if (digit != null) {
                                // Initialize injector if not already done
                                if (inputInjector == null) {
                                    DialPadDetectorService.instance?.let { service ->
                                        inputInjector = InputInjector(service)
                                    }
                                }
                                
                                val success = inputInjector?.injectDigit(digit) ?: false
                                result.success(success)
                            } else {
                                result.error("INVALID_ARGUMENT", "Digit is required", null)
                            }
                        } catch (e: Exception) {
                            result.error("INJECTION_ERROR", "Failed to inject digit: ${e.message}", null)
                        }
                    }
                    else -> {
                        result.notImplemented()
                    }
                }
            } catch (e: Exception) {
                result.error("UNEXPECTED_ERROR", "Unexpected error: ${e.message}", null)
            }
        }
        
        // Set up event channel for Android → Flutter events
        eventChannel = EventChannel(flutterEngine.dartExecutor.binaryMessenger, EVENT_CHANNEL)
        eventChannel.setStreamHandler(TelephonyEventStreamHandler.instance)
    }
    
    private fun startOverlayService() {
        try {
            val intent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_START_SERVICE
            }
            startService(intent)
        } catch (e: SecurityException) {
            android.util.Log.e("MainActivity", "Security exception starting overlay service", e)
            throw e
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error starting overlay service", e)
            throw e
        }
    }
    
    private fun stopOverlayService() {
        try {
            val intent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_STOP_SERVICE
            }
            startService(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error stopping overlay service", e)
            throw e
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        
        // Test drag loop sound after a delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            testDragLoop()
        }, 2000)
    }
    
    private fun testDragLoop() {
        try {
            android.util.Log.d("MainActivity", "Testing drag loop sound...")
            val soundManager = RotarySoundManager(this)
            
            // Wait for sound to load, then test
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                android.util.Log.d("MainActivity", "Starting drag loop...")
                soundManager.startDragLoop()
                
                // Stop after 3 seconds
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    android.util.Log.d("MainActivity", "Stopping drag loop...")
                    soundManager.stopDragLoop()
                }, 3000)
            }, 3000) // Wait 3 seconds for sound to fully load
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error testing drag loop", e)
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        intent?.let {
            when (it.action) {
                Intent.ACTION_DIAL -> {
                    // Pre-fill number but don't call
                    val phoneNumber = it.data?.schemeSpecificPart
                    if (phoneNumber != null) {
                        sendToFlutter("prefillNumber", mapOf("phoneNumber" to phoneNumber))
                    }
                }
                Intent.ACTION_CALL -> {
                    // Immediate call (requires CALL_PHONE permission)
                    val phoneNumber = it.data?.schemeSpecificPart
                    if (phoneNumber != null) {
                        telephonyManager.placeCall(phoneNumber)
                    }
                }
            }
        }
    }
    
    private fun sendToFlutter(method: String, arguments: Any?) {
        methodChannel.invokeMethod(method, arguments)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        defaultDialerManager.handleActivityResult(requestCode, resultCode)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.handlePermissionsResult(requestCode, permissions, grantResults)
    }
}
