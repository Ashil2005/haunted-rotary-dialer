# Design Document: Rotary Overlay Toggle

## Overview

The Rotary Overlay Toggle app is a lightweight Flutter application that provides a simple enable/disable switch to overlay a nostalgic rotary dial interface on top of the Android system dial pad. When enabled, the app runs a background service that detects when a dial pad appears and displays a realistic rotary dial overlay. User interactions with the rotary dial are translated into digit inputs that are sent to the underlying system dial pad, maintaining full compatibility with all phone functions.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Flutter UI Layer                      │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Main App Screen (Toggle Button + Status)          │ │
│  └────────────────────────────────────────────────────┘ │
│                         │                                │
│                         ▼                                │
│  ┌────────────────────────────────────────────────────┐ │
│  │         Method Channel Bridge                       │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│              Android Native Layer (Kotlin)               │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Overlay Service (Manages Rotary Overlay Window)   │ │
│  └────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Accessibility Service (Detects Dial Pad)          │ │
│  └────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Input Injection (Sends Digits to Dial Pad)        │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

1. **User enables toggle** → Flutter UI calls native method via Method Channel
2. **Native layer starts services** → Accessibility Service begins monitoring UI
3. **Dial pad detected** → Accessibility Service notifies Overlay Service
4. **Overlay appears** → Flutter View embedded in Android Window overlays dial pad
5. **User rotates dial** → Flutter captures gesture, calculates digit
6. **Digit registered** → Native layer injects input into system dial pad
7. **User disables toggle** → Services stop, overlay removed

## Components and Interfaces

### 1. Flutter UI Layer

#### Main App Screen (`lib/main.dart`)
- **Purpose**: Provides the primary user interface with toggle button
- **State Management**: Uses `StatefulWidget` with `SharedPreferences` for persistence
- **Key Elements**:
  - Large toggle switch (Material Switch widget)
  - Status text ("Enabled" / "Disabled")
  - First-time explanation card
  - Settings button for future customization

#### Rotary Dial Widget (`lib/widgets/rotary_overlay_widget.dart`)
- **Purpose**: Renders the rotary dial interface for the overlay
- **Rendering**: Uses `CustomPaint` for high-performance graphics
- **Features**:
  - Touch gesture detection (`GestureDetector`)
  - Rotation physics simulation
  - Digit registration logic (hole-first algorithm)
  - Visual feedback (shadows, highlights, animations)

#### Method Channel Bridge (`lib/services/overlay_service.dart`)
```dart
class OverlayService {
  static const platform = MethodChannel('com.example.rotary_dialer/overlay');
  
  Future<bool> enableOverlay() async {
    return await platform.invokeMethod('enableOverlay');
  }
  
  Future<bool> disableOverlay() async {
    return await platform.invokeMethod('disableOverlay');
  }
  
  Future<bool> checkPermissions() async {
    return await platform.invokeMethod('checkPermissions');
  }
  
  Future<void> requestPermissions() async {
    await platform.invokeMethod('requestPermissions');
  }
  
  void sendDigit(String digit) {
    platform.invokeMethod('sendDigit', {'digit': digit});
  }
}
```

### 2. Android Native Layer

#### Overlay Service (`OverlayService.kt`)
- **Type**: Android Foreground Service
- **Purpose**: Manages the overlay window lifecycle
- **Key Responsibilities**:
  - Create and manage `WindowManager` overlay window
  - Embed Flutter View into overlay window
  - Position overlay over dial pad coordinates
  - Handle overlay visibility and removal
  - Maintain foreground notification

```kotlin
class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var flutterEngine: FlutterEngine? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> showOverlay()
            ACTION_HIDE_OVERLAY -> hideOverlay()
            ACTION_STOP_SERVICE -> stopService()
        }
        return START_STICKY
    }
    
    private fun showOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        
        // Create Flutter view for overlay
        overlayView = createFlutterView()
        windowManager?.addView(overlayView, params)
    }
}
```

#### Accessibility Service (`DialPadDetectorService.kt`)
- **Type**: Android AccessibilityService
- **Purpose**: Monitors UI hierarchy to detect dial pad appearance
- **Detection Strategy**:
  - Listen for `TYPE_WINDOW_STATE_CHANGED` events
  - Check for dial pad package names (com.android.dialer, com.google.android.dialer)
  - Verify presence of dial pad UI elements (EditText for number, digit buttons)
  - Extract dial pad position and dimensions

```kotlin
class DialPadDetectorService : AccessibilityService() {
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val rootNode = rootInActiveWindow ?: return
            
            if (isDialPadVisible(rootNode)) {
                val bounds = getDialPadBounds(rootNode)
                notifyOverlayService(bounds)
            } else {
                hideOverlayIfVisible()
            }
        }
    }
    
    private fun isDialPadVisible(node: AccessibilityNodeInfo): Boolean {
        // Check for dial pad indicators
        val hasDialPadPackage = node.packageName in DIALER_PACKAGES
        val hasNumberField = findNodeByViewId(node, "digits") != null
        return hasDialPadPackage && hasNumberField
    }
}
```

#### Input Injection Manager (`InputInjector.kt`)
- **Purpose**: Sends digit inputs to the system dial pad
- **Approach**: Uses Accessibility Service's `performAction()` to inject text
- **Fallback**: If direct injection fails, uses `InputConnection` or `Instrumentation`

```kotlin
class InputInjector(private val accessibilityService: AccessibilityService) {
    
    fun injectDigit(digit: String) {
        val rootNode = accessibilityService.rootInActiveWindow ?: return
        val numberField = findNumberInputField(rootNode) ?: return
        
        // Method 1: Direct text injection
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, 
                getCurrentText(numberField) + digit)
        }
        
        if (numberField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
            return // Success
        }
        
        // Method 2: Simulate key press
        simulateKeyPress(getKeyCodeForDigit(digit))
    }
    
    private fun findNumberInputField(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Find EditText with resource ID "digits" or similar
        return findNodeByViewId(node, "digits") 
            ?: findNodeByClassName(node, "android.widget.EditText")
    }
}
```

#### Permission Manager (`PermissionManager.kt`)
- **Purpose**: Handles permission requests and status checks
- **Permissions Required**:
  - `SYSTEM_ALERT_WINDOW` (Draw over other apps)
  - `BIND_ACCESSIBILITY_SERVICE` (Accessibility service)
- **Methods**:
  - `checkOverlayPermission()`: Check if overlay permission granted
  - `requestOverlayPermission()`: Open settings to grant permission
  - `checkAccessibilityPermission()`: Check if accessibility service enabled
  - `requestAccessibilityPermission()`: Open accessibility settings

## Data Models

### Toggle State
```dart
class ToggleState {
  final bool isEnabled;
  final bool hasOverlayPermission;
  final bool hasAccessibilityPermission;
  final DateTime? lastToggleTime;
  
  bool get canEnable => hasOverlayPermission && hasAccessibilityPermission;
}
```

### Overlay Configuration
```dart
class OverlayConfig {
  final double dialRadius;
  final Color dialColor;
  final bool soundEnabled;
  final bool hapticEnabled;
  final double sensitivity;
  
  // Saved to SharedPreferences
}
```

### Dial Pad Bounds
```kotlin
data class DialPadBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val packageName: String
)
```

## Error Handling

### Permission Errors
- **Scenario**: User denies overlay or accessibility permission
- **Handling**: 
  - Disable toggle automatically
  - Show explanation dialog with "Open Settings" button
  - Persist disabled state to prevent auto-enable on restart

### Service Crashes
- **Scenario**: Overlay Service or Accessibility Service crashes
- **Handling**:
  - Use `START_STICKY` to auto-restart service
  - Implement exponential backoff for repeated crashes
  - Log crash details for debugging
  - Show notification to user if service cannot restart

### Dial Pad Detection Failures
- **Scenario**: Cannot detect dial pad or inject input
- **Handling**:
  - Log detection failure with package name and UI hierarchy
  - Continue monitoring for dial pad (don't crash)
  - Provide "Report Issue" feature to send logs to developer

### Overlay Rendering Issues
- **Scenario**: Overlay doesn't fit properly or blocks UI elements
- **Handling**:
  - Implement adaptive sizing based on screen dimensions
  - Provide manual adjustment controls in settings
  - Allow user to temporarily hide overlay with swipe gesture

## Testing Strategy

### Unit Tests
- **Rotary Math**: Test digit calculation from rotation angles
- **State Management**: Test toggle state persistence and transitions
- **Permission Logic**: Test permission checking and request flows

### Integration Tests
- **Method Channel**: Test Flutter-Native communication
- **Service Lifecycle**: Test service start/stop/restart scenarios
- **Input Injection**: Test digit injection into mock dial pad

### Manual Testing Checklist
1. Enable toggle → Verify services start and notification appears
2. Open phone app → Verify overlay appears over dial pad
3. Rotate dial → Verify digits appear in dial pad number field
4. Make call → Verify call proceeds normally
5. Disable toggle → Verify overlay disappears and services stop
6. Restart device → Verify toggle state persists
7. Test with third-party dialers (WhatsApp, Skype, etc.)
8. Test on different Android versions (8.0, 10.0, 12.0, 14.0)
9. Test on different screen sizes (phone, tablet, foldable)
10. Test permission denial and re-granting flows

### Performance Testing
- **Overlay Latency**: Measure time from dial pad appearance to overlay display (target: <500ms)
- **Frame Rate**: Monitor FPS during rotation (target: 60 FPS)
- **Memory Usage**: Monitor service memory footprint (target: <50MB)
- **Battery Impact**: Measure battery drain over 24 hours (target: <2%)

## Technical Considerations

### Android Version Compatibility
- **Minimum SDK**: Android 8.0 (API 26) - Required for `TYPE_APPLICATION_OVERLAY`
- **Target SDK**: Android 14 (API 34)
- **Accessibility API**: Available since API 14, stable since API 16
- **Foreground Service**: Notification required since API 26

### Overlay Window Type
- Use `TYPE_APPLICATION_OVERLAY` (API 26+) instead of deprecated `TYPE_SYSTEM_ALERT`
- Requires `SYSTEM_ALERT_WINDOW` permission
- Cannot be used for secure windows (lock screen, password fields)

### Flutter Engine Management
- Create single `FlutterEngine` instance for overlay to reduce memory
- Cache engine when overlay is hidden (don't destroy)
- Use `FlutterEngineCache` for efficient reuse

### Input Injection Limitations
- Accessibility Service can only inject into apps that allow it
- Some secure fields (passwords) block accessibility input
- Emergency dialer may have restrictions

### Third-Party Dialer Support
- Detection logic must handle various package names and UI structures
- Some dialers may use custom views that are harder to detect
- Provide whitelist/blacklist feature for app compatibility

## Future Enhancements

1. **Customization Options**:
   - Multiple rotary dial themes (black, red, brass)
   - Adjustable dial size and position
   - Custom sound effects

2. **Smart Detection**:
   - Machine learning to improve dial pad detection
   - Adaptive positioning based on dial pad location

3. **Gesture Shortcuts**:
   - Swipe to temporarily hide overlay
   - Double-tap to disable toggle quickly

4. **Analytics**:
   - Track usage patterns (anonymized)
   - Identify compatibility issues with specific devices/apps

5. **Widget Support**:
   - Home screen widget for quick toggle
   - Quick settings tile for Android notification shade
