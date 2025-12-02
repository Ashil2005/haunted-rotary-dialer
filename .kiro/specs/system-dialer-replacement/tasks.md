# Implementation Plan

- [ ] 1. Set up Android native infrastructure
  - Create method channel bridge between Flutter and Android
  - Configure AndroidManifest.xml with required permissions and intent filters
  - Set up Kotlin source directory structure
  - _Requirements: 1.1, 1.2, 1.3_

- [ ] 1.1 Configure AndroidManifest.xml
  - Add all required permissions (READ_PHONE_STATE, CALL_PHONE, READ_CONTACTS, etc.)
  - Add intent filters for ACTION_DIAL and ACTION_CALL
  - Configure InCallService and CallScreeningService declarations
  - _Requirements: 1.1, 3.1, 5.1, 6.1_

- [ ] 1.2 Create method channel infrastructure
  - Implement MethodChannel in MainActivity with channel name 'com.rotarydialer/telephony'
  - Create MethodCallHandler to route calls to appropriate managers
  - Set up event sink for Android → Flutter communication
  - _Requirements: 1.1, 3.1, 5.1_

- [ ] 1.3 Create Kotlin manager classes
  - Implement TelephonyManager class for telecom operations
  - Implement PermissionsManager class for runtime permission handling
  - Implement DefaultDialerManager class for role management
  - _Requirements: 1.1, 1.2, 6.1, 6.2_

- [ ] 2. Implement default dialer role management
  - Create methods to check if app is default dialer
  - Implement request default dialer role functionality
  - Handle role request results and update Flutter
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2_

- [ ] 2.1 Implement isDefaultDialer method
  - Check RoleManager.ROLE_DIALER for Android 10+
  - Fall back to TelecomManager.getDefaultDialerPackage for older versions
  - Return boolean result to Flutter via method channel
  - _Requirements: 1.4, 2.3_

- [ ] 2.2 Implement requestDefaultDialerRole method
  - Create RoleManager intent for Android 10+
  - Create TelecomManager intent for older versions
  - Launch intent and handle result in onActivityResult
  - Send result back to Flutter
  - _Requirements: 1.2, 1.3_

- [ ] 2.3 Implement openDefaultDialerSettings method
  - Create intent to open system default apps settings
  - Navigate to dialer app selection screen
  - _Requirements: 2.1, 2.2_

- [ ] 3. Implement outgoing call functionality
  - Create DialerActivity to handle ACTION_DIAL and ACTION_CALL intents
  - Extract phone number from intent data
  - Implement placeCall method using TelecomManager
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 3.1 Create DialerActivity
  - Extend FlutterActivity for seamless Flutter integration
  - Override onCreate and onNewIntent to handle dialer intents
  - Parse tel: URIs and extract phone numbers
  - Send phone number to Flutter via method channel
  - _Requirements: 3.1, 3.2, 1.5_

- [ ] 3.2 Implement placeCall method
  - Create Uri from phone number
  - Use TelecomManager.placeCall() to initiate call
  - Handle call placement errors gracefully
  - _Requirements: 3.4, 3.5_

- [ ] 4. Implement InCallService for active call management
  - Create RotaryInCallService extending InCallService
  - Handle onCallAdded and onCallRemoved callbacks
  - Implement call control methods (answer, reject, end, mute, speaker, hold)
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 4.1 Create RotaryInCallService class
  - Extend InCallService
  - Register Call.Callback for state changes
  - Maintain reference to current active call
  - Send call state updates to Flutter
  - _Requirements: 4.1, 4.5_

- [ ] 4.2 Implement call control methods
  - Implement answerCall() using Call.answer()
  - Implement rejectCall() using Call.reject()
  - Implement endCall() using Call.disconnect()
  - Implement setMuted() using Call.setMuted()
  - Implement setSpeaker() using AudioManager
  - Implement setHold() using Call.hold() and Call.unhold()
  - _Requirements: 4.2, 4.3_

- [ ] 4.3 Implement call state tracking
  - Override Call.Callback.onStateChanged()
  - Map Call.STATE_* constants to CallState enum
  - Send state changes to Flutter via event channel
  - Track call duration with timer
  - _Requirements: 4.1, 4.4_

- [ ] 5. Implement incoming call handling
  - Create CallScreeningService for call identification
  - Show full-screen incoming call notification
  - Implement answer and reject actions
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 5.1 Create RotaryCallScreeningService
  - Extend CallScreeningService
  - Override onScreenCall() to identify incoming calls
  - Extract caller information (number, name)
  - Send incoming call event to Flutter
  - _Requirements: 5.1, 5.2_

- [ ] 5.2 Implement incoming call notification
  - Create full-screen intent for incoming calls
  - Display caller information (name, number, photo)
  - Add answer and reject action buttons
  - Handle notification dismissal
  - _Requirements: 5.1, 5.3_

- [ ] 6. Implement Flutter TelephonyService
  - Create TelephonyService class with method channel
  - Implement all method channel calls
  - Set up event streams for incoming calls and state changes
  - _Requirements: 1.1, 3.1, 4.1, 5.1_

- [ ] 6.1 Create TelephonyService class
  - Initialize MethodChannel with 'com.rotarydialer/telephony'
  - Set up MethodCallHandler for Android → Flutter events
  - Create StreamControllers for incomingCalls and callStateChanges
  - _Requirements: 1.1, 5.1_

- [ ] 6.2 Implement method channel methods
  - Implement requestDefaultDialerRole()
  - Implement isDefaultDialer()
  - Implement openDefaultDialerSettings()
  - Implement placeCall(phoneNumber)
  - Implement answerCall(callId)
  - Implement rejectCall(callId)
  - Implement endCall(callId)
  - Implement setMuted(muted)
  - Implement setSpeaker(speaker)
  - Implement setHold(hold)
  - _Requirements: 1.1, 1.2, 2.1, 3.4, 4.2, 4.3, 5.3, 5.4_

- [ ] 6.3 Implement event handlers
  - Handle onIncomingCall events from Android
  - Handle onCallStateChanged events from Android
  - Handle onCallEnded events from Android
  - Handle onDefaultDialerStatusChanged events from Android
  - _Requirements: 5.1, 4.1_

- [ ] 7. Implement state management with Provider
  - Create CallState provider for active call state
  - Create DialerState provider for dialed number
  - Create SettingsState provider for app preferences
  - _Requirements: 1.4, 2.3, 3.3, 4.1, 10.5_

- [ ] 7.1 Create CallState provider
  - Track current call state (idle, ringing, active, etc.)
  - Track active call ID, phone number, caller name
  - Track call duration with timer
  - Track mute, speaker, hold states
  - Implement toggleMute(), toggleSpeaker(), toggleHold() methods
  - Implement endCall() method
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [ ] 7.2 Create DialerState provider
  - Track dialed number string
  - Implement addDigit(digit) method
  - Implement removeLastDigit() method
  - Implement clear() method
  - Validate phone numbers
  - _Requirements: 3.3_

- [ ] 7.3 Create SettingsState provider
  - Track isDefaultDialer status
  - Track sound effects enabled/disabled
  - Track haptic feedback enabled/disabled
  - Track dial sensitivity setting
  - Implement persistence with SharedPreferences
  - _Requirements: 1.4, 2.3, 10.2, 10.3, 10.4, 10.5_

- [ ] 8. Create SettingsScreen with default dialer toggle
  - Build settings UI with toggle switches
  - Implement default dialer toggle functionality
  - Add sound effects, haptic feedback, and sensitivity settings
  - _Requirements: 1.1, 2.1, 2.2, 10.1, 10.2, 10.3, 10.4_

- [ ] 8.1 Build SettingsScreen UI
  - Create Scaffold with AppBar
  - Add SwitchListTile for "Set as Default Dialer"
  - Add SwitchListTile for "Sound Effects"
  - Add SwitchListTile for "Haptic Feedback"
  - Add Slider for "Dial Sensitivity"
  - Display current default dialer status
  - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [ ] 8.2 Implement default dialer toggle logic
  - Call TelephonyService.requestDefaultDialerRole() when toggled on
  - Call TelephonyService.openDefaultDialerSettings() when toggled off
  - Update UI based on actual default dialer status
  - Show explanatory dialogs for permission requests
  - _Requirements: 1.1, 1.2, 2.1, 2.2_

- [ ] 8.3 Implement settings persistence
  - Save settings to SharedPreferences on change
  - Load settings on app startup
  - Apply settings to RotaryDialWidget behavior
  - _Requirements: 10.5_

- [ ] 9. Create InCallScreen for active call management
  - Build in-call UI with caller information
  - Add call control buttons (mute, speaker, hold, end)
  - Display call duration timer
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [ ] 9.1 Build InCallScreen UI
  - Create full-screen layout with dark retro theme
  - Display caller name and phone number
  - Add CircularProgressIndicator or animated element
  - Display call duration in MM:SS format
  - Create call control button row
  - _Requirements: 4.1, 4.4_

- [ ] 9.2 Implement call control buttons
  - Create CallControlButton widget for consistent styling
  - Add Mute button with mic icon (toggles on/off)
  - Add Speaker button with volume icon (toggles on/off)
  - Add Hold button with pause icon (toggles on/off)
  - Add End Call button with phone icon (red color)
  - Connect buttons to CallState provider methods
  - _Requirements: 4.2, 4.3_

- [ ] 9.3 Implement call duration timer
  - Start timer when call becomes active
  - Update UI every second
  - Format duration as MM:SS
  - Stop timer when call ends
  - _Requirements: 4.4_

- [ ] 10. Create IncomingCallScreen for call notifications
  - Build full-screen incoming call UI
  - Display caller information with photo
  - Add swipe or button actions for answer/reject
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 10.1 Build IncomingCallScreen UI
  - Create full-screen overlay with dark background
  - Display caller photo (circular avatar)
  - Display caller name in large text
  - Display phone number below name
  - Add "Incoming Call" label
  - _Requirements: 5.1, 5.2_

- [ ] 10.2 Implement answer/reject actions
  - Add green "Answer" button with phone icon
  - Add red "Reject" button with phone disabled icon
  - Implement swipe gestures (swipe up to answer, down to reject)
  - Call TelephonyService.answerCall() on answer
  - Call TelephonyService.rejectCall() on reject
  - Transition to InCallScreen on answer
  - Dismiss screen on reject
  - _Requirements: 5.3, 5.4, 5.5_

- [ ] 11. Implement call history integration
  - Create method to fetch call log from Android
  - Build CallHistoryScreen UI
  - Display call type icons and timestamps
  - Implement tap to redial functionality
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 11.1 Implement getCallHistory method (Android)
  - Query CallLog.Calls content provider
  - Retrieve last N calls (configurable limit)
  - Extract call type, number, name, duration, timestamp
  - Return List<CallLogEntry> to Flutter
  - _Requirements: 7.1_

- [ ] 11.2 Build CallHistoryScreen UI
  - Create ListView with call log entries
  - Display call type icon (incoming, outgoing, missed)
  - Display contact name or phone number
  - Display call timestamp (relative or absolute)
  - Display call duration for completed calls
  - Add pull-to-refresh functionality
  - _Requirements: 7.1, 7.2, 7.4_

- [ ] 11.3 Implement tap to redial
  - Add onTap handler to call history items
  - Pre-fill phone number in rotary dial
  - Navigate to RotaryDialScreen
  - _Requirements: 7.3_

- [ ] 11.4 Implement real-time call history updates
  - Listen to call state changes
  - Refresh call history after each call ends
  - Update UI automatically
  - _Requirements: 7.5_

- [ ] 12. Implement contacts integration
  - Create method to fetch contacts from Android
  - Build ContactsScreen UI with search
  - Display contact photos and multiple numbers
  - Implement tap to dial functionality
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 12.1 Implement getContacts method (Android)
  - Query ContactsContract.Contacts content provider
  - Retrieve contact display name, phone numbers, photo URI
  - Handle contacts with multiple phone numbers
  - Return List<Contact> to Flutter
  - _Requirements: 8.1_

- [ ] 12.2 Build ContactsScreen UI
  - Create ListView with contact entries
  - Display contact photo (circular avatar with placeholder)
  - Display contact name
  - Add search bar at top
  - Implement alphabetical section headers
  - Add fast scroll functionality
  - _Requirements: 8.1, 8.2, 8.5_

- [ ] 12.3 Implement contact search
  - Filter contacts by name or phone number
  - Update ListView in real-time as user types
  - Highlight matching text
  - _Requirements: 8.2_

- [ ] 12.4 Implement tap to dial
  - Add onTap handler to contact items
  - If contact has single number, pre-fill and navigate to dial screen
  - If contact has multiple numbers, show bottom sheet to select number
  - Display number labels (mobile, home, work)
  - _Requirements: 8.3, 8.4_

- [ ] 13. Implement emergency calling support
  - Detect emergency numbers based on locale
  - Bypass confirmations for emergency calls
  - Display emergency call indicator
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ] 13.1 Implement emergency number detection
  - Create list of emergency numbers by country code
  - Check dialed number against emergency list
  - Use TelecomManager.isEmergencyNumber() for validation
  - _Requirements: 9.1_

- [ ] 13.2 Implement emergency call handling
  - Skip confirmation dialogs for emergency numbers
  - Place call immediately when emergency number is dialed
  - Show prominent red "EMERGENCY CALL" indicator
  - Allow emergency calls even without SIM card
  - _Requirements: 9.2, 9.3, 9.4_

- [ ] 14. Implement permissions management
  - Create permission request flow with rationale
  - Handle permission denied scenarios
  - Provide settings shortcut for manual permission grant
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 14.1 Implement checkPermissions method (Android)
  - Check READ_PHONE_STATE permission
  - Check CALL_PHONE permission
  - Check READ_CONTACTS permission
  - Check READ_CALL_LOG permission
  - Return Map<String, bool> with permission statuses
  - _Requirements: 6.4_

- [ ] 14.2 Implement requestPermissions method (Android)
  - Request specified permissions using ActivityCompat.requestPermissions()
  - Handle onRequestPermissionsResult callback
  - Return boolean indicating if all permissions granted
  - _Requirements: 6.1, 6.2_

- [ ] 14.3 Implement permission rationale dialogs
  - Show explanation dialog before requesting permissions
  - Explain why each permission is needed
  - Provide "Grant" and "Deny" options
  - _Requirements: 6.1_

- [ ] 14.4 Handle permission denied scenarios
  - Disable affected features when permissions denied
  - Show informative message explaining limitation
  - Provide button to open app settings for manual grant
  - Allow app to function in standalone mode without default dialer permissions
  - _Requirements: 6.3, 6.5_

- [ ] 15. Update main RotaryDialScreen to integrate with telephony
  - Modify RotaryDialScreen to use DialerState provider
  - Connect Call button to TelephonyService.placeCall()
  - Handle intent-provided phone numbers
  - Add navigation to settings, call history, and contacts
  - _Requirements: 1.5, 3.1, 3.2, 3.3, 3.4_

- [ ] 15.1 Integrate DialerState provider
  - Wrap RotaryDialScreen with Consumer<DialerState>
  - Update number display from DialerState
  - Call DialerState.addDigit() when number dialed on rotary
  - Call DialerState.removeLastDigit() on backspace
  - _Requirements: 3.3_

- [ ] 15.2 Connect Call button to telephony
  - Replace url_launcher with TelephonyService.placeCall()
  - Validate phone number before calling
  - Show loading indicator during call placement
  - Handle call placement errors
  - _Requirements: 3.4, 3.5_

- [ ] 15.3 Handle intent-provided phone numbers
  - Listen to method channel for prefillNumber events
  - Update DialerState with provided number
  - Distinguish between ACTION_DIAL (prefill) and ACTION_CALL (immediate)
  - _Requirements: 1.5, 3.1, 3.2_

- [ ] 15.4 Add navigation to other screens
  - Add bottom navigation bar or drawer
  - Add navigation to SettingsScreen
  - Add navigation to CallHistoryScreen
  - Add navigation to ContactsScreen
  - _Requirements: 10.1_

- [ ] 16. Implement app-wide navigation and routing
  - Set up named routes for all screens
  - Implement navigation based on call state
  - Handle incoming call screen overlay
  - _Requirements: 4.5, 5.1_

- [ ] 16.1 Configure named routes
  - Define routes for RotaryDialScreen, InCallScreen, IncomingCallScreen, SettingsScreen, CallHistoryScreen, ContactsScreen
  - Set RotaryDialScreen as home route
  - Implement onGenerateRoute for dynamic routing
  - _Requirements: 10.1_

- [ ] 16.2 Implement call state navigation
  - Listen to CallState changes in main app
  - Navigate to InCallScreen when call becomes active
  - Navigate back to RotaryDialScreen when call ends
  - Show IncomingCallScreen as full-screen overlay on incoming call
  - _Requirements: 4.5, 5.1_

- [ ] 17. Add audio integration for dial sounds
  - Update RotaryDialWidget to use settings for sound effects
  - Implement conditional audio playback based on settings
  - Add actual click.wav and release.wav audio files
  - _Requirements: 10.2_

- [ ] 17.1 Update RotaryDialWidget audio methods
  - Check SettingsState.soundEffectsEnabled before playing sounds
  - Implement proper audio player disposal
  - Handle audio playback errors gracefully
  - _Requirements: 10.2_

- [ ] 17.2 Add audio assets
  - Create or source click.wav sound effect (short mechanical click)
  - Create or source release.wav sound effect (spring release sound)
  - Add audio files to assets/sounds/ directory
  - Update pubspec.yaml assets section
  - _Requirements: 10.2_

- [ ] 18. Update RotaryDialWidget to respect settings
  - Integrate haptic feedback toggle from settings
  - Integrate dial sensitivity setting
  - Apply settings in real-time
  - _Requirements: 10.3, 10.4_

- [ ] 18.1 Implement haptic feedback toggle
  - Check SettingsState.hapticFeedbackEnabled before triggering haptics
  - Update _checkNotchCrossing() method
  - _Requirements: 10.3_

- [ ] 18.2 Implement dial sensitivity
  - Use SettingsState.dialSensitivity to adjust rotation speed
  - Apply sensitivity multiplier to deltaRotation in _onPanUpdate
  - Allow users to customize dial feel
  - _Requirements: 10.4_

- [ ]* 19. Write unit tests for core functionality
  - Test TelephonyService method channel communication
  - Test CallState, DialerState, SettingsState providers
  - Test data model serialization
  - Test permission checking logic
  - _Requirements: All_

- [ ]* 20. Write widget tests for UI components
  - Test RotaryDialWidget gesture detection
  - Test InCallScreen button interactions
  - Test SettingsScreen toggle behavior
  - Test navigation flows
  - _Requirements: All_

- [ ] 21. Update documentation and README
  - Document Android-only limitation
  - Add setup instructions for default dialer
  - Document required permissions
  - Add screenshots of all screens
  - _Requirements: All_

- [ ] 21.1 Update README.md
  - Add "System Dialer Replacement" section
  - Document Android version requirements (8.0+)
  - List all required permissions with explanations
  - Add setup instructions for setting as default dialer
  - Add troubleshooting section
  - _Requirements: All_

- [ ] 21.2 Create user guide
  - Document how to enable/disable default dialer
  - Explain all features (call history, contacts, settings)
  - Add FAQ section
  - Include screenshots of key screens
  - _Requirements: All_

- [ ] 22. Final integration and testing
  - Test complete flow from installation to making calls
  - Test on multiple Android devices and versions
  - Verify emergency calling works correctly
  - Test permission flows thoroughly
  - Verify app works in standalone mode when not default dialer
  - _Requirements: All_
