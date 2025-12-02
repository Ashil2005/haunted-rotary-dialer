# Requirements Document

## Introduction

This document outlines the requirements for a background toggle app that overlays a rotary dial interface on top of the system's dial pad when enabled. The app provides a simple on/off switch - when enabled, any dial pad in the phone system is replaced with a fun, nostalgic rotary phone interface. When disabled, everything returns to normal.

## Glossary

- **Toggle App**: The main Flutter application with a single enable/disable button
- **Rotary Overlay**: The visual rotary dial interface that appears over the system dial pad
- **System Dial Pad**: The default numeric keypad interface provided by Android for entering phone numbers
- **Overlay Service**: Android service that draws UI elements on top of other applications
- **Accessibility Service**: Android service that can monitor and interact with UI elements from other apps
- **Dial Intent**: Android system intent triggered when the user wants to dial a phone number

## Requirements

### Requirement 1: Simple Toggle Interface

**User Story:** As a user, I want a simple app with an enable/disable button, so that I can quickly turn the rotary dial feature on or off.

#### Acceptance Criteria

1. WHEN the user opens the app, THE Toggle App SHALL display a single prominent toggle button
2. THE Toggle App SHALL show the current status as either "Enabled" or "Disabled"
3. WHEN the user taps the toggle button, THE Toggle App SHALL switch between enabled and disabled states
4. THE Toggle App SHALL persist the toggle state across app restarts
5. THE Toggle App SHALL display a brief explanation of what the toggle does on first launch

### Requirement 2: Permission Requests

**User Story:** As a user, I want the app to request necessary permissions with clear explanations, so that I understand why they are needed.

#### Acceptance Criteria

1. WHEN the user enables the toggle for the first time, THE Toggle App SHALL request "Draw over other apps" permission
2. THE Toggle App SHALL display a clear explanation that this permission allows the rotary dial to appear over the dial pad
3. IF the user denies the permission, THEN THE Toggle App SHALL disable the toggle and show an explanation
4. WHEN the user enables the toggle, THE Toggle App SHALL request accessibility service permission if needed
5. THE Toggle App SHALL provide a button to open system settings if permissions are denied

### Requirement 3: Rotary Overlay Activation

**User Story:** As a user, I want the rotary dial to automatically appear when I open any dial pad, so that I can use it for dialing without extra steps.

#### Acceptance Criteria

1. WHEN the toggle is enabled and a dial pad appears, THE Toggle App SHALL display the rotary overlay within 500 milliseconds
2. THE Rotary Overlay SHALL fit perfectly over the dial pad area without obscuring other UI elements
3. THE Rotary Overlay SHALL match the screen dimensions and orientation of the device
4. WHEN the dial pad is closed, THE Toggle App SHALL remove the rotary overlay immediately
5. THE Rotary Overlay SHALL appear for dial pads in the phone app, contacts app, and any other system dialer

### Requirement 4: Rotary Dial Interaction

**User Story:** As a user, I want to dial numbers by rotating the dial, so that I can experience the nostalgic feel of old rotary phones.

#### Acceptance Criteria

1. WHEN the user drags a number hole clockwise to the finger stop, THE Rotary Overlay SHALL register that digit
2. THE Rotary Overlay SHALL display the dialed digits in a number display area
3. WHEN a digit is registered, THE Rotary Overlay SHALL send the digit to the underlying dial pad
4. THE Rotary Overlay SHALL provide realistic rotation physics with momentum and spring-back
5. THE Rotary Overlay SHALL play authentic rotary dial sounds during rotation and digit registration

### Requirement 5: Seamless Integration

**User Story:** As a user, I want the rotary dial to work with all phone functions, so that I don't lose any functionality.

#### Acceptance Criteria

1. WHEN digits are entered via the rotary dial, THE Toggle App SHALL pass them to the system dial pad
2. THE Toggle App SHALL allow the user to access call, delete, and other dial pad buttons
3. WHEN the user presses the system call button, THE Toggle App SHALL allow the call to proceed normally
4. THE Rotary Overlay SHALL not interfere with incoming calls or active call screens
5. THE Toggle App SHALL work with third-party dialer apps when they display a dial pad

### Requirement 6: Toggle Deactivation

**User Story:** As a user, I want to disable the rotary dial and return to normal, so that I can use the standard dial pad when needed.

#### Acceptance Criteria

1. WHEN the user disables the toggle, THE Toggle App SHALL immediately stop displaying the rotary overlay
2. THE Toggle App SHALL remove any active overlays within 100 milliseconds of disabling
3. WHEN disabled, THE Toggle App SHALL not monitor for dial pad appearances
4. THE Toggle App SHALL release all system resources when disabled
5. WHEN re-enabled, THE Toggle App SHALL resume overlay functionality without requiring app restart

### Requirement 7: Background Operation

**User Story:** As a user, I want the app to work in the background, so that I don't need to keep it open.

#### Acceptance Criteria

1. WHEN the toggle is enabled, THE Toggle App SHALL run a background service to monitor for dial pads
2. THE Toggle App SHALL continue to function when the app is closed or in the background
3. THE Toggle App SHALL survive device restarts if the toggle was enabled
4. THE Toggle App SHALL use minimal battery and memory resources while running in background
5. THE Toggle App SHALL display a persistent notification when the service is active

### Requirement 8: Visual Customization

**User Story:** As a user, I want the rotary dial to look realistic and authentic, so that it provides a genuine nostalgic experience.

#### Acceptance Criteria

1. THE Rotary Overlay SHALL display a realistic metallic rotary dial with proper shading and depth
2. THE Rotary Overlay SHALL show finger holes numbered 1-9 and 0 in the classic rotary layout
3. THE Rotary Overlay SHALL include a finger stop indicator at the correct position
4. THE Rotary Overlay SHALL use appropriate colors and materials to match vintage rotary phones
5. THE Rotary Overlay SHALL scale appropriately for different screen sizes and densities

### Requirement 9: Performance and Responsiveness

**User Story:** As a user, I want the rotary dial to respond smoothly to my touch, so that it feels natural and enjoyable to use.

#### Acceptance Criteria

1. THE Rotary Overlay SHALL maintain 60 frames per second during rotation animations
2. THE Rotary Overlay SHALL respond to touch input within 16 milliseconds
3. THE Rotary Overlay SHALL complete digit registration within 100 milliseconds of reaching the finger stop
4. THE Toggle App SHALL not cause lag or delays in the system dial pad functionality
5. THE Rotary Overlay SHALL handle rapid consecutive digit entries without dropping inputs

### Requirement 10: Error Handling and Edge Cases

**User Story:** As a user, I want the app to handle errors gracefully, so that it doesn't crash or interfere with important phone functions.

#### Acceptance Criteria

1. IF the overlay service crashes, THEN THE Toggle App SHALL restart it automatically within 2 seconds
2. IF permissions are revoked while enabled, THEN THE Toggle App SHALL disable the toggle and notify the user
3. THE Toggle App SHALL not block emergency calls under any circumstances
4. IF the system dial pad cannot be detected, THEN THE Toggle App SHALL log the error and continue monitoring
5. THE Toggle App SHALL provide a "Report Issue" button that captures logs for troubleshooting
