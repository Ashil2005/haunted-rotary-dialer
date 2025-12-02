# Implementation Plan

- [x] 1. Create Flutter toggle UI and state management



  - Create main app screen with Material Switch toggle button
  - Implement state management using StatefulWidget with setState
  - Add SharedPreferences for toggle state persistence
  - Create status display showing "Enabled" / "Disabled"
  - Add first-time explanation card with dismissible feature


  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 2. Implement Method Channel bridge for Flutter-Native communication
  - Create OverlayService class in Flutter with MethodChannel
  - Define method channel name 'com.example.rotary_dialer/overlay'
  - Implement enableOverlay() method
  - Implement disableOverlay() method
  - Implement checkPermissions() method


  - Implement requestPermissions() method
  - Implement sendDigit(String digit) method
  - _Requirements: 1.1, 1.3, 2.1, 4.3_

- [ ] 3. Create Android Permission Manager
  - Create PermissionManager.kt in Android native code
  - Implement checkOverlayPermission() using Settings.canDrawOverlays()


  - Implement requestOverlayPermission() to open Settings.ACTION_MANAGE_OVERLAY_PERMISSION
  - Implement checkAccessibilityPermission() by querying enabled accessibility services
  - Implement requestAccessibilityPermission() to open Settings.ACTION_ACCESSIBILITY_SETTINGS
  - Add permission declarations to AndroidManifest.xml (SYSTEM_ALERT_WINDOW, BIND_ACCESSIBILITY_SERVICE)
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ] 4. Implement Accessibility Service for dial pad detection
  - Create DialPadDetectorService.kt extending AccessibilityService
  - Configure service in AndroidManifest.xml with intent filter
  - Create accessibility_service_config.xml with event types and package names


  - Implement onAccessibilityEvent() to listen for TYPE_WINDOW_STATE_CHANGED
  - Implement isDialPadVisible() to detect dial pad UI elements
  - Add logic to check for common dialer package names (com.android.dialer, com.google.android.dialer)
  - Implement findNodeByViewId() helper to locate number input field
  - Implement getDialPadBounds() to extract position and dimensions
  - Create broadcast mechanism to notify Overlay Service when dial pad detected
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 5. Create Overlay Service for managing overlay window
  - Create OverlayService.kt extending Service

  - Implement onStartCommand() with actions: SHOW_OVERLAY, HIDE_OVERLAY, STOP_SERVICE
  - Create foreground notification for service (required for Android 8.0+)
  - Implement showOverlay() to create WindowManager overlay window
  - Configure WindowManager.LayoutParams with TYPE_APPLICATION_OVERLAY
  - Set flags: FLAG_NOT_FOCUSABLE, FLAG_NOT_TOUCH_MODAL for touch pass-through
  - Implement hideOverlay() to remove overlay view from WindowManager
  - Implement stopService() to clean up resources and stop foreground service
  - Add START_STICKY return flag for automatic service restart
  - _Requirements: 3.1, 3.2, 3.4, 6.1, 6.2, 7.1, 7.2, 7.3_



- [ ] 6. Embed Flutter View in overlay window
  - Create FlutterEngine instance in OverlayService
  - Configure FlutterEngine with DartEntrypoint for overlay route
  - Create FlutterView and attach to FlutterEngine
  - Implement createFlutterView() method in OverlayService
  - Add overlay view to WindowManager with calculated layout params
  - Implement FlutterEngineCache for efficient engine reuse
  - Create separate Dart entrypoint for overlay UI (overlayMain())
  - _Requirements: 3.1, 3.2, 3.3, 8.5_



- [ ] 7. Adapt rotary dial widget for overlay usage
  - Extract existing RotaryDialWidget to separate file if not already done
  - Create RotaryOverlayWidget that wraps RotaryDialWidget
  - Add transparent background to overlay widget
  - Implement digit callback to send digits via Method Channel
  - Add touch event handling to prevent pass-through during active rotation
  - Scale rotary dial to fit dial pad area dimensions
  - Position number display to align with dial pad number field
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 8.1, 8.2, 8.3, 8.4, 8.5_



- [ ] 8. Implement input injection to send digits to dial pad
  - Create InputInjector.kt class with AccessibilityService reference
  - Implement injectDigit(String digit) method
  - Implement findNumberInputField() to locate EditText in dial pad
  - Use AccessibilityNodeInfo.ACTION_SET_TEXT to inject digit



  - Implement fallback using ACTION_PASTE if SET_TEXT fails
  - Add getCurrentText() helper to preserve existing digits
  - Implement getKeyCodeForDigit() for keypress simulation fallback
  - Handle edge cases: empty field, cursor position, selection
  - _Requirements: 4.3, 5.1, 5.2, 5.3_

- [x] 9. Wire Method Channel handlers in MainActivity



  - Update MainActivity.kt to handle Method Channel calls
  - Implement 'enableOverlay' handler to start OverlayService
  - Implement 'disableOverlay' handler to stop OverlayService
  - Implement 'checkPermissions' handler using PermissionManager
  - Implement 'requestPermissions' handler to open settings
  - Implement 'sendDigit' handler to call InputInjector
  - Add error handling and return appropriate responses to Flutter
  - Store InputInjector instance in MainActivity for digit injection


  - _Requirements: 1.3, 2.1, 4.3, 6.1, 6.2_

- [ ] 10. Implement service lifecycle and persistence
  - Add logic to start OverlayService on device boot if toggle was enabled
  - Create BootReceiver.kt to listen for BOOT_COMPLETED broadcast
  - Check SharedPreferences for toggle state in BootReceiver
  - Implement service restart logic with exponential backoff on crashes
  - Add notification actions for quick disable from notification shade

  - Implement onDestroy() cleanup in OverlayService
  - Handle service unbind and rebind scenarios
  - _Requirements: 6.3, 7.2, 7.3, 10.1, 10.2_

- [ ] 11. Add error handling and edge cases
  - Implement try-catch blocks in all native methods
  - Add null checks for AccessibilityNodeInfo operations

  - Handle permission revocation while service is running
  - Implement emergency call detection to bypass overlay
  - Add logging for debugging dial pad detection failures
  - Create error notification when service cannot start
  - Implement graceful degradation if input injection fails
  - Add "Report Issue" button in Flutter UI to export logs
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_



- [ ] 12. Implement settings and customization
  - Create SettingsScreen widget in Flutter
  - Add navigation from main screen to settings
  - Implement sound effects toggle with SharedPreferences persistence
  - Implement haptic feedback toggle with SharedPreferences persistence
  - Implement dial sensitivity slider (0.5x to 2.0x)
  - Pass settings to overlay via Method Channel


  - Update RotaryDialWidget to respect settings
  - _Requirements: 1.5, 4.5, 8.4, 9.3_

- [ ] 13. Create unit tests for core functionality
  - Write tests for toggle state management and persistence
  - Write tests for permission checking logic
  - Write tests for Method Channel communication
  - Write tests for rotary math calculations
  - Write tests for digit registration logic
  - _Requirements: 4.1, 4.2, 9.1, 9.2, 9.3, 9.4_

- [ ] 14. Create integration tests for service interaction
  - Write test for enabling toggle and starting services
  - Write test for disabling toggle and stopping services
  - Write test for permission request flows
  - Write test for digit injection into mock dial pad
  - Write test for service restart after crash
  - _Requirements: 1.3, 2.1, 6.1, 6.2, 10.1_

- [ ] 15. Polish UI and add visual feedback
  - Add loading indicator when starting/stopping services
  - Implement smooth toggle animation
  - Add permission status indicators (checkmarks/warnings)
  - Create onboarding flow for first-time users
  - Add app icon and splash screen
  - Implement Material Design 3 theming
  - Add haptic feedback to toggle button
  - _Requirements: 1.1, 1.2, 1.5, 2.2, 2.3_

- [ ] 16. Optimize performance and resource usage
  - Implement FlutterEngine caching to reduce memory
  - Add debouncing to dial pad detection to prevent flicker
  - Optimize overlay rendering to maintain 60 FPS
  - Reduce background service memory footprint
  - Implement lazy loading for Flutter engine
  - Add battery optimization exemption request if needed
  - Profile and optimize accessibility event processing
  - _Requirements: 7.4, 9.1, 9.2, 9.3, 9.4_
