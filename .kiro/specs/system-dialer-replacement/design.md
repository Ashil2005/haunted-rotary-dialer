# Design Document: System Dialer Replacement

## Overview

This design document outlines the architecture and implementation approach for transforming the Rotary Dialer Flutter app into a full system dialer replacement for Android. The solution uses Flutter for the UI layer and Android native code (Kotlin) for system-level telephony integration.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Flutter UI Layer                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Rotary Dial  │  │  In-Call UI  │  │   Settings   │  │
│  │   Screen     │  │    Screen    │  │    Screen    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│         │                  │                  │          │
│         └──────────────────┴──────────────────┘          │
│                           │                               │
│                  ┌────────▼────────┐                     │
│                  │ Method Channel  │                     │
│                  └────────┬────────┘                     │
└───────────────────────────┼──────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────┐
│              Android Native Layer (Kotlin)               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Dialer     │  │  InCallUI    │  │   Telecom    │  │
│  │   Activity   │  │   Service    │  │   Manager    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│         │                  │                  │          │
│         └──────────────────┴──────────────────┘          │
│                           │                               │
└───────────────────────────┼──────────────────────────────┘
                            │
                   ┌────────▼────────┐
                   │  Android System │
                   │   Telephony     │
                   └─────────────────┘
```

### Component Architecture

#### Flutter Layer Components

1. **RotaryDialScreen** - Main dialing interface with rotary widget
2. **InCallScreen** - Active call management UI
3. **IncomingCallScreen** - Full-screen incoming call notification
4. **CallHistoryScreen** - Recent calls list
5. **ContactsScreen** - Device contacts browser
6. **SettingsScreen** - App configuration and default dialer toggle
7. **TelephonyService** - Flutter service for method channel communication

#### Android Native Components

1. **DialerActivity** - Handles ACTION_DIAL and ACTION_CALL intents
2. **InCallService** - Manages active call state and controls
3. **CallScreeningService** - Identifies incoming calls
4. **TelecomManager** - Interface to Android telephony system
5. **PermissionsManager** - Handles runtime permission requests
6. **DefaultDialerManager** - Manages default dialer role

## Components and Interfaces

### Method Channel Interface

**Channel Name:** `com.rotarydialer/telephony`

#### Methods (Flutter → Android)

```dart
// Request default dialer role
Future<bool> requestDefaultDialerRole()

// Check if app is default dialer
Future<bool> isDefaultDialer()

// Open default apps settings
Future<void> openDefaultDialerSettings()

// Place outgoing call
Future<void> placeCall(String phoneNumber)

// Answer incoming call
Future<void> answerCall(String callId)

// Reject incoming call
Future<void> rejectCall(String callId)

// End active call
Future<void> endCall(String callId)

// Toggle mute
Future<void> setMuted(bool muted)

// Toggle speaker
Future<void> setSpeaker(bool speaker)

// Toggle hold
Future<void> setHold(bool hold)

// Get call history
Future<List<CallLogEntry>> getCallHistory(int limit)

// Get contacts
Future<List<Contact>> getContacts()

// Check permissions
Future<Map<String, bool>> checkPermissions()

// Request permissions
Future<bool> requestPermissions(List<String> permissions)
```

#### Events (Android → Flutter)

```dart
// Incoming call event
onIncomingCall(String callId, String phoneNumber, String callerName)

// Call state changed
onCallStateChanged(String callId, String state)

// Call ended
onCallEnded(String callId, int duration)

// Default dialer status changed
onDefaultDialerStatusChanged(bool isDefault)
```

### Data Models

#### CallLogEntry

```dart
class CallLogEntry {
  final String id;
  final String phoneNumber;
  final String? contactName;
  final CallType type; // incoming, outgoing, missed
  final DateTime timestamp;
  final int duration; // seconds
  final String? photoUri;
}
```

#### Contact

```dart
class Contact {
  final String id;
  final String displayName;
  final List<PhoneNumber> phoneNumbers;
  final String? photoUri;
}

class PhoneNumber {
  final String number;
  final String? label; // mobile, home, work, etc.
}
```

#### CallState

```dart
enum CallState {
  idle,
  ringing,
  dialing,
  active,
  held,
  disconnected
}
```

## Android Native Implementation

### AndroidManifest.xml Configuration

```xml
<!-- Permissions -->
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.WRITE_CALL_LOG" />
<uses-permission android:name="android.permission.ANSWER_PHONE_CALLS" />
<uses-permission android:name="android.permission.MANAGE_OWN_CALLS" />

<!-- Intent filters for dialer -->
<activity android:name=".DialerActivity">
    <intent-filter>
        <action android:name="android.intent.action.DIAL" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:scheme="tel" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.DIAL" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="tel" />
    </intent-filter>
</activity>

<!-- InCallService -->
<service
    android:name=".RotaryInCallService"
    android:permission="android.permission.BIND_INCALL_SERVICE">
    <intent-filter>
        <action android:name="android.telecom.InCallService" />
    </intent-filter>
</service>

<!-- Call Screening Service (Android 10+) -->
<service
    android:name=".RotaryCallScreeningService"
    android:permission="android.permission.BIND_SCREENING_SERVICE">
    <intent-filter>
        <action android:name="android.telecom.CallScreeningService" />
    </intent-filter>
</service>
```

### DialerActivity (Kotlin)

```kotlin
class DialerActivity : FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle incoming intent
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }
    
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_DIAL -> {
                // Pre-fill number but don't call
                val phoneNumber = intent.data?.schemeSpecificPart
                sendToFlutter("prefilNumber", phoneNumber)
            }
            Intent.ACTION_CALL -> {
                // Immediate call
                val phoneNumber = intent.data?.schemeSpecificPart
                if (phoneNumber != null) {
                    placeCall(phoneNumber)
                }
            }
        }
    }
}
```

### InCallService Implementation

```kotlin
class RotaryInCallService : InCallService() {
    private var currentCall: Call? = null
    
    override fun onCallAdded(call: Call) {
        currentCall = call
        
        // Register callback
        call.registerCallback(callCallback)
        
        // Notify Flutter
        sendCallStateToFlutter(call)
        
        // Show in-call UI
        showInCallUI(call)
    }
    
    override fun onCallRemoved(call: Call) {
        currentCall = null
        sendCallEndedToFlutter(call)
    }
    
    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            sendCallStateToFlutter(call)
        }
    }
    
    fun answerCall() {
        currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }
    
    fun rejectCall() {
        currentCall?.reject(false, null)
    }
    
    fun endCall() {
        currentCall?.disconnect()
    }
    
    fun setMuted(muted: Boolean) {
        currentCall?.let {
            it.setMuted(muted)
        }
    }
}
```

### TelecomManager Integration

```kotlin
class TelephonyManager(private val context: Context) {
    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    
    fun placeCall(phoneNumber: String) {
        val uri = Uri.fromParts("tel", phoneNumber, null)
        val extras = Bundle()
        
        telecomManager.placeCall(uri, extras)
    }
    
    fun isDefaultDialer(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        } else {
            telecomManager.defaultDialerPackage == context.packageName
        }
    }
    
    fun requestDefaultDialerRole(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(Context.ROLE_SERVICE) as RoleManager
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            activity.startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER)
        } else {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
            intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            activity.startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER)
        }
    }
}
```

## Flutter Implementation

### TelephonyService (Flutter)

```dart
class TelephonyService {
  static const platform = MethodChannel('com.rotarydialer/telephony');
  
  // Stream controllers for events
  final _incomingCallController = StreamController<IncomingCall>.broadcast();
  final _callStateController = StreamController<CallStateEvent>.broadcast();
  
  Stream<IncomingCall> get incomingCalls => _incomingCallController.stream;
  Stream<CallStateEvent> get callStateChanges => _callStateController.stream;
  
  TelephonyService() {
    platform.setMethodCallHandler(_handleMethodCall);
  }
  
  Future<void> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onIncomingCall':
        _incomingCallController.add(IncomingCall.fromMap(call.arguments));
        break;
      case 'onCallStateChanged':
        _callStateController.add(CallStateEvent.fromMap(call.arguments));
        break;
    }
  }
  
  Future<bool> requestDefaultDialerRole() async {
    return await platform.invokeMethod('requestDefaultDialerRole');
  }
  
  Future<bool> isDefaultDialer() async {
    return await platform.invokeMethod('isDefaultDialer');
  }
  
  Future<void> placeCall(String phoneNumber) async {
    await platform.invokeMethod('placeCall', {'phoneNumber': phoneNumber});
  }
  
  Future<void> answerCall(String callId) async {
    await platform.invokeMethod('answerCall', {'callId': callId});
  }
  
  Future<void> endCall(String callId) async {
    await platform.invokeMethod('endCall', {'callId': callId});
  }
}
```

### State Management

Use **Provider** for state management:

```dart
class CallState extends ChangeNotifier {
  CallStateEnum _state = CallStateEnum.idle;
  String? _activeCallId;
  String? _phoneNumber;
  String? _callerName;
  Duration _callDuration = Duration.zero;
  bool _isMuted = false;
  bool _isSpeaker = false;
  
  // Getters
  CallStateEnum get state => _state;
  String? get activeCallId => _activeCallId;
  // ... other getters
  
  // Methods
  void updateCallState(CallStateEvent event) {
    _state = event.state;
    _activeCallId = event.callId;
    notifyListeners();
  }
  
  void toggleMute() {
    _isMuted = !_isMuted;
    TelephonyService().setMuted(_isMuted);
    notifyListeners();
  }
}
```

### UI Screens

#### SettingsScreen with Default Dialer Toggle

```dart
class SettingsScreen extends StatefulWidget {
  @override
  _SettingsScreenState createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool _isDefaultDialer = false;
  final _telephonyService = TelephonyService();
  
  @override
  void initState() {
    super.initState();
    _checkDefaultDialerStatus();
  }
  
  Future<void> _checkDefaultDialerStatus() async {
    final isDefault = await _telephonyService.isDefaultDialer();
    setState(() {
      _isDefaultDialer = isDefault;
    });
  }
  
  Future<void> _toggleDefaultDialer(bool value) async {
    if (value) {
      final granted = await _telephonyService.requestDefaultDialerRole();
      if (granted) {
        setState(() {
          _isDefaultDialer = true;
        });
      }
    } else {
      await _telephonyService.openDefaultDialerSettings();
    }
  }
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Settings')),
      body: ListView(
        children: [
          SwitchListTile(
            title: Text('Set as Default Dialer'),
            subtitle: Text(_isDefaultDialer 
              ? 'Rotary Dialer is your default phone app'
              : 'Use system default dialer'),
            value: _isDefaultDialer,
            onChanged: _toggleDefaultDialer,
          ),
          // Other settings...
        ],
      ),
    );
  }
}
```

#### InCallScreen

```dart
class InCallScreen extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Consumer<CallState>(
      builder: (context, callState, child) {
        return Scaffold(
          backgroundColor: Color(0xFF1a1a1a),
          body: Column(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              // Caller info
              CallerInfoWidget(
                name: callState.callerName,
                number: callState.phoneNumber,
              ),
              
              // Call duration
              CallDurationWidget(duration: callState.callDuration),
              
              // Call controls
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  CallControlButton(
                    icon: callState.isMuted ? Icons.mic_off : Icons.mic,
                    label: 'Mute',
                    onPressed: () => callState.toggleMute(),
                  ),
                  CallControlButton(
                    icon: Icons.phone_disabled,
                    label: 'End',
                    color: Colors.red,
                    onPressed: () => callState.endCall(),
                  ),
                  CallControlButton(
                    icon: callState.isSpeaker ? Icons.volume_up : Icons.volume_down,
                    label: 'Speaker',
                    onPressed: () => callState.toggleSpeaker(),
                  ),
                ],
              ),
            ],
          ),
        );
      },
    );
  }
}
```

## Error Handling

### Permission Denied

- Show explanatory dialog with rationale
- Provide button to open app settings
- Gracefully degrade functionality

### Call Failures

- Display error message to user
- Log error details for debugging
- Fall back to system dialer if available

### Default Dialer Role Denied

- Update toggle to reflect actual state
- Show message explaining how to set manually
- Continue functioning as standalone app

## Testing Strategy

### Unit Tests

- TelephonyService method channel communication
- CallState state management logic
- Data model serialization/deserialization
- Permission checking logic

### Widget Tests

- RotaryDialWidget gesture detection
- InCallScreen button interactions
- SettingsScreen toggle behavior
- Navigation flows

### Integration Tests

- End-to-end call placement
- Incoming call handling
- Default dialer role request flow
- Call history retrieval

### Manual Testing

- Test on multiple Android versions (8.0 - 14)
- Test with different device manufacturers
- Test emergency calling
- Test with no SIM card
- Test call interruptions (incoming call during active call)

## Performance Considerations

- Use RepaintBoundary for rotary dial widget
- Lazy load call history and contacts
- Cache contact photos
- Minimize method channel calls
- Use isolates for heavy data processing

## Security Considerations

- Validate all phone numbers before calling
- Sanitize contact data display
- Secure storage for user preferences
- Comply with GDPR for call log access
- Handle sensitive data appropriately

## Deployment Considerations

- Minimum Android SDK: 26 (Android 8.0)
- Target Android SDK: 34 (Android 14)
- Requires Google Play Services for some features
- App size: ~15-20 MB
- Requires testing on physical devices (emulator limitations for telephony)
