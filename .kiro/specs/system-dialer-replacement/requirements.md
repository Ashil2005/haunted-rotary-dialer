# Requirements Document

## Introduction

This document outlines the requirements for transforming the Rotary Dialer app into a system-level default dialer replacement for Android devices. The feature allows users to enable the rotary dial interface as their primary phone dialer, replacing the stock Android dialer, and disable it to return to their original dialer.

## Glossary

- **System Dialer**: The default phone application that handles dialing, incoming calls, and call management on Android devices
- **Rotary Dialer App**: The Flutter application providing a retro rotary phone interface
- **Intent Handler**: Android component that responds to system intents for dialing and calling
- **InCallService**: Android service that manages active phone calls
- **Default Dialer Prompt**: System dialog asking users to set an app as the default phone app
- **Dialer Role**: Android 10+ API for managing default dialer status
- **Call Screening**: Feature to identify and filter incoming calls

## Requirements

### Requirement 1: System Dialer Registration

**User Story:** As a user, I want to set the Rotary Dialer as my default phone app, so that all dialing actions use the rotary interface.

#### Acceptance Criteria

1. WHEN the user opens the app for the first time, THE Rotary Dialer App SHALL display a settings screen with an option to "Set as Default Dialer"
2. WHEN the user taps "Set as Default Dialer", THE Rotary Dialer App SHALL trigger the Android system dialog to request default dialer role
3. IF the user grants default dialer role, THEN THE Rotary Dialer App SHALL become the system default for all dialing intents
4. WHEN the Rotary Dialer App is set as default, THE Rotary Dialer App SHALL display a toggle showing "Enabled" status
5. THE Rotary Dialer App SHALL handle ACTION_DIAL intents with phone numbers pre-filled in the rotary interface

### Requirement 2: Default Dialer Deactivation

**User Story:** As a user, I want to disable the Rotary Dialer and return to my original phone app, so that I can switch back when needed.

#### Acceptance Criteria

1. WHEN the user toggles "Set as Default Dialer" to off, THE Rotary Dialer App SHALL open Android system settings for default apps
2. THE Rotary Dialer App SHALL provide clear instructions to select a different default dialer
3. WHEN another dialer is set as default, THE Rotary Dialer App SHALL update its toggle to show "Disabled" status
4. THE Rotary Dialer App SHALL continue to function as a standalone dialer even when not set as default
5. THE Rotary Dialer App SHALL persist user preferences for the toggle state across app restarts

### Requirement 3: Outgoing Call Handling

**User Story:** As a user, I want to dial phone numbers using the rotary interface when making outgoing calls, so that I have a nostalgic calling experience.

#### Acceptance Criteria

1. WHEN the user initiates a call from any app (contacts, browser, etc.), THE Rotary Dialer App SHALL open with the rotary dial interface
2. IF a phone number is provided in the intent, THEN THE Rotary Dialer App SHALL pre-fill the number display
3. WHEN the user rotates the dial, THE Rotary Dialer App SHALL append digits to the number display
4. WHEN the user presses the Call button, THE Rotary Dialer App SHALL initiate the phone call using Android Telecom APIs
5. THE Rotary Dialer App SHALL handle both ACTION_DIAL (pre-fill only) and ACTION_CALL (immediate call) intents

### Requirement 4: In-Call Interface

**User Story:** As a user, I want to see call controls during an active call, so that I can manage the call (mute, speaker, hang up).

#### Acceptance Criteria

1. WHEN a call is active, THE Rotary Dialer App SHALL display an in-call screen with call duration timer
2. THE Rotary Dialer App SHALL provide buttons for mute, speaker, hold, and end call
3. WHEN the user presses end call, THE Rotary Dialer App SHALL terminate the active call
4. THE Rotary Dialer App SHALL display caller information (name, number) during active calls
5. WHILE a call is active, THE Rotary Dialer App SHALL continue to function when the app is in the background

### Requirement 5: Incoming Call Handling

**User Story:** As a user, I want to answer or reject incoming calls through the rotary interface, so that all phone interactions use the retro design.

#### Acceptance Criteria

1. WHEN an incoming call is received, THE Rotary Dialer App SHALL display a full-screen incoming call notification
2. THE Rotary Dialer App SHALL show caller information (name, number, photo if available)
3. THE Rotary Dialer App SHALL provide swipe or button actions to answer or reject the call
4. WHEN the user answers the call, THE Rotary Dialer App SHALL transition to the in-call interface
5. WHEN the user rejects the call, THE Rotary Dialer App SHALL dismiss the incoming call screen

### Requirement 6: Permissions Management

**User Story:** As a user, I want the app to request only necessary permissions, so that I understand why each permission is needed.

#### Acceptance Criteria

1. WHEN the app is first launched, THE Rotary Dialer App SHALL request READ_PHONE_STATE permission with explanation
2. WHEN the user attempts to set as default dialer, THE Rotary Dialer App SHALL request CALL_PHONE permission
3. IF the user denies required permissions, THEN THE Rotary Dialer App SHALL display an explanation and disable affected features
4. THE Rotary Dialer App SHALL request READ_CONTACTS permission only when user attempts to access contacts
5. THE Rotary Dialer App SHALL function in standalone mode without default dialer permissions

### Requirement 7: Call History Integration

**User Story:** As a user, I want to see my recent calls in the app, so that I can redial numbers easily.

#### Acceptance Criteria

1. WHEN the user opens the call history tab, THE Rotary Dialer App SHALL display recent calls from the system call log
2. THE Rotary Dialer App SHALL show call type (incoming, outgoing, missed) with appropriate icons
3. WHEN the user taps a call history entry, THE Rotary Dialer App SHALL pre-fill the number in the rotary dial
4. THE Rotary Dialer App SHALL display call duration and timestamp for each entry
5. THE Rotary Dialer App SHALL update call history in real-time after each call

### Requirement 8: Contacts Integration

**User Story:** As a user, I want to access my contacts from the app, so that I can dial saved numbers without typing.

#### Acceptance Criteria

1. WHEN the user opens the contacts tab, THE Rotary Dialer App SHALL display the device contact list
2. THE Rotary Dialer App SHALL provide search functionality to filter contacts by name or number
3. WHEN the user selects a contact, THE Rotary Dialer App SHALL pre-fill the contact's phone number in the rotary dial
4. IF a contact has multiple numbers, THEN THE Rotary Dialer App SHALL display all numbers with labels
5. THE Rotary Dialer App SHALL display contact photos when available

### Requirement 9: Emergency Calling

**User Story:** As a user, I want to dial emergency numbers quickly, so that I can get help in urgent situations.

#### Acceptance Criteria

1. THE Rotary Dialer App SHALL recognize emergency numbers (911, 112, etc.) based on device locale
2. WHEN an emergency number is dialed, THE Rotary Dialer App SHALL bypass any confirmation dialogs
3. THE Rotary Dialer App SHALL display a prominent emergency call indicator when dialing emergency numbers
4. THE Rotary Dialer App SHALL place emergency calls even if the device has no SIM card
5. THE Rotary Dialer App SHALL comply with all regional emergency calling regulations

### Requirement 10: Settings and Preferences

**User Story:** As a user, I want to customize the app behavior, so that it works according to my preferences.

#### Acceptance Criteria

1. THE Rotary Dialer App SHALL provide a settings screen accessible from the main interface
2. THE Rotary Dialer App SHALL allow users to toggle sound effects on/off
3. THE Rotary Dialer App SHALL allow users to toggle haptic feedback on/off
4. THE Rotary Dialer App SHALL allow users to adjust dial sensitivity (rotation speed)
5. THE Rotary Dialer App SHALL persist all settings across app restarts and system reboots
