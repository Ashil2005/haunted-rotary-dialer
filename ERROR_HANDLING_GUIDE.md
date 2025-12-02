# Error Handling Guide

## Overview

Comprehensive error handling has been implemented across all layers of the rotary dial overlay app to ensure robustness and provide clear feedback to users.

## Error Handling Strategy

### 1. Flutter Layer (Dart)

#### OverlayService
- **PlatformException handling**: Catches and rethrows platform-specific errors with error codes
- **Generic exception handling**: Catches unexpected errors and returns false
- **Error logging**: All errors are logged with descriptive messages
- **Return values**: Methods return boolean success indicators

**Error Codes:**
- `SERVICE_ERROR`: Service failed to start/stop
- `PERMISSION_ERROR`: Permission denied or check failed
- `INJECTION_ERROR`: Failed to inject digit
- `INVALID_ARGUMENT`: Invalid parameters passed
- `UNEXPECTED_ERROR`: Unexpected error occurred

#### Main App (main.dart)
- **Permission flow errors**: Handles permission denial gracefully
- **Service start/stop errors**: Provides user-friendly error messages
- **State recovery**: Ensures UI state is consistent even after errors
- **User feedback**: Shows SnackBar messages for all error conditions

**Error Messages:**
- "Please grant overlay and accessibility permissions in settings"
- "Permission error: [details]"
- "Service error: [details]"
- "Permission denied. Please check settings."
- "Failed to enable overlay. Check permissions."
- "Unexpected error: [details]"

### 2. Android Native Layer (Kotlin)

#### OverlayService
**Error Scenarios Handled:**
- Flutter engine initialization failure
- Overlay window creation failure (SecurityException, IllegalStateException)
- View attachment errors
- Service crash and restart
- Resource cleanup errors

**Error Handling:**
```kotlin
try {
    // Operation
} catch (e: SecurityException) {
    Log.e(TAG, "Permission denied", e)
    showErrorNotification("Overlay permission denied")
} catch (e: IllegalStateException) {
    Log.e(TAG, "Invalid state", e)
    showErrorNotification("Cannot show overlay: Invalid state")
} catch (e: Exception) {
    Log.e(TAG, "Unexpected error", e)
    showErrorNotification("Failed to show overlay")
}
```

**Features:**
- Error notifications shown to user
- Automatic cleanup on errors
- Graceful degradation
- START_STICKY for automatic restart

#### DialPadDetectorService
**Error Scenarios Handled:**
- Root node unavailable
- Emergency dialer detection (skips overlay)
- Node recycling errors
- Broadcast send failures
- Invalid accessibility node states

**Safety Features:**
- Emergency call bypass (never overlays emergency dialer)
- Node validation before use
- Proper node recycling in finally blocks
- Null checks throughout
- Try-catch on all node operations

#### InputInjector
**Error Scenarios Handled:**
- Invalid digit input
- Accessibility service unavailable
- Input field not found
- Node no longer valid
- Text injection failure

**Validation:**
- Digit format validation
- Node validity checks
- Service availability checks
- Graceful fallback mechanisms

#### MainActivity
**Error Scenarios Handled:**
- Method channel call failures
- Service start/stop errors
- Permission check failures
- Input injector initialization errors

**Error Responses:**
- Structured error codes returned to Flutter
- Detailed error messages
- Proper exception propagation
- Logging for debugging

### 3. Edge Cases Handled

#### Emergency Calls
- **Detection**: Checks for "emergency" in package name
- **Action**: Skips overlay entirely for emergency dialers
- **Priority**: Emergency calls always work normally

#### Permission Revocation
- **Detection**: Checks permissions before operations
- **Action**: Disables toggle and notifies user
- **Recovery**: User can re-enable after granting permissions

#### Service Crashes
- **Detection**: START_STICKY flag ensures restart
- **Action**: Service automatically restarts
- **Notification**: Error notification shown if restart fails
- **State**: Persisted state ensures correct behavior after restart

#### Node Lifecycle
- **Validation**: Checks if nodes are still valid before use
- **Recycling**: Proper recycling in finally blocks
- **Null checks**: All node operations check for null
- **Stale references**: Detects and handles stale node references

#### Overlay Window Issues
- **View already attached**: Checks before adding view
- **View not attached**: Handles removal of non-attached views
- **Window manager null**: Checks before operations
- **Screen rotation**: Handles configuration changes

#### Input Injection Failures
- **Primary method**: Direct text injection via accessibility
- **Fallback**: Key press simulation (placeholder)
- **Validation**: Verifies digit format and field availability
- **Logging**: Detailed logs for debugging injection issues

## Error Recovery Mechanisms

### Automatic Recovery
1. **Service restart**: START_STICKY ensures service restarts after crash
2. **Engine caching**: Flutter engine cached and reused
3. **State persistence**: Toggle state persists across restarts
4. **Permission re-check**: Permissions checked before each operation

### Manual Recovery
1. **Toggle disable/enable**: User can reset by toggling
2. **Permission re-grant**: User can grant permissions in settings
3. **App restart**: Full reset by restarting app
4. **Service restart**: System restarts service automatically

### User Guidance
1. **Clear error messages**: User-friendly error descriptions
2. **Action suggestions**: "Please check settings", "Grant permissions"
3. **Status indicators**: Visual feedback on toggle state
4. **Notifications**: Error notifications with details

## Logging Strategy

### Log Levels
- **DEBUG**: Normal operations, state changes
- **INFO**: Important events (service start/stop, overlay show/hide)
- **WARN**: Recoverable issues (key press simulation not supported)
- **ERROR**: Failures requiring attention

### Log Tags
- `OverlayService`: Overlay window management
- `DialPadDetector`: Dial pad detection
- `InputInjector`: Digit injection
- `OverlayPermissionMgr`: Permission management
- `MainActivity`: Method channel operations

### What's Logged
- All errors with stack traces
- Permission checks and results
- Service lifecycle events
- Overlay show/hide operations
- Dial pad detection events
- Input injection attempts
- Node operations and failures

## Testing Error Scenarios

### Manual Testing
1. **Deny permissions**: Verify graceful handling
2. **Revoke permissions while running**: Check auto-disable
3. **Kill service**: Verify automatic restart
4. **Emergency call**: Confirm no overlay appears
5. **Unsupported dialer**: Check detection failure handling
6. **Rapid toggle**: Test race conditions
7. **Low memory**: Verify service behavior under pressure

### Expected Behaviors
- No crashes under any scenario
- Clear error messages to user
- Automatic recovery where possible
- Consistent UI state
- Proper resource cleanup

## Troubleshooting

### Overlay doesn't appear
**Possible causes:**
- Permissions not granted
- Service not running
- Dial pad not detected
- Flutter engine failed to initialize

**Check:**
1. Verify both permissions granted
2. Check notification - service should be running
3. Check logs for detection failures
4. Try disabling and re-enabling toggle

### Digits don't register
**Possible causes:**
- Accessibility service not available
- Input field not found
- Node became invalid
- Injection method not supported

**Check:**
1. Verify accessibility service enabled
2. Check logs for injection errors
3. Try different dialer app
4. Restart accessibility service

### Service keeps stopping
**Possible causes:**
- Repeated crashes
- Battery optimization killing service
- Insufficient permissions
- System resource constraints

**Check:**
1. Check logs for crash reasons
2. Disable battery optimization for app
3. Verify all permissions granted
4. Check available system memory

### Error notifications keep appearing
**Possible causes:**
- Permissions revoked
- Service initialization failing
- Flutter engine issues
- System compatibility problems

**Check:**
1. Re-grant all permissions
2. Restart app completely
3. Check Android version compatibility (API 26+)
4. Review error notification details

## Best Practices

### For Developers
1. Always check return values
2. Use try-catch for all native operations
3. Recycle accessibility nodes in finally blocks
4. Validate inputs before processing
5. Log errors with context
6. Provide user-friendly error messages
7. Test error scenarios thoroughly

### For Users
1. Grant all required permissions
2. Keep app updated
3. Report issues with log details
4. Disable battery optimization if needed
5. Use supported Android version (8.0+)
6. Test with default dialer first

## Future Improvements

1. **Retry mechanisms**: Automatic retry for transient failures
2. **Error analytics**: Track error patterns for improvements
3. **Self-healing**: Automatic permission re-request
4. **Compatibility detection**: Warn about unsupported devices
5. **Diagnostic mode**: Enhanced logging for troubleshooting
6. **Error reporting**: Built-in bug report generation
