# Haunted Rotary Dialer 👻📞

A Halloween-themed Android app that overlays a nostalgic rotary phone dial on your system dial pad.  
Built with Flutter + Kotlin + Kiro for the Kiroween Hackathon.

## 🎯 What It Does

This app provides a simple toggle that, when enabled, displays a realistic rotary dial interface over your phone's dial pad. Rotate the dial to enter numbers just like the old days, then make your call normally. Disable the toggle to return to your standard dial pad.

## ✨ Features

- **Simple Toggle**: One button to enable/disable the rotary overlay
- **Automatic Detection**: Overlay appears automatically when you open any dial pad
- **Realistic Experience**: Authentic rotary dial with physics and animations
- **Full Compatibility**: Works with all phone functions - calls proceed normally
- **Background Operation**: Runs in the background, survives app closure and reboots
- **Customizable Settings**: Adjust sound effects, haptic feedback, and dial sensitivity
- **Safe & Secure**: Never overlays on emergency calls, no data collection

## 📱 Requirements

- Android 8.0 (API 26) or higher
- Permissions:
  - Draw over other apps (for overlay)
  - Accessibility service (for dial pad detection)

## 🚀 Installation

1. Download and install the APK
2. Open the app
3. Tap the toggle to enable
4. Grant required permissions:
   - Allow "Draw over other apps"
   - Enable "Rotary Dial Overlay" in Accessibility settings
5. Open your phone app and dial!

## 🎮 How to Use

1. **Enable**: Tap the toggle in the app to turn on the overlay
2. **Dial**: Open your phone's dial pad - the rotary dial appears automatically
3. **Rotate**: Drag a number hole clockwise to the finger stop to dial that digit
4. **Call**: Press the call button to make your call normally
5. **Disable**: Return to the app and toggle off to use normal dial pad
 ## How We Used Kiro

- Vibe coding sessions to design the rotary dial physics and math.
- Spec-driven development using the `/.kiro/specs` folder to guide overlay behavior.
- Hooks in `/.kiro/hooks` to automate testing and iteration on dial mechanics and accessibility.


## ⚙️ Settings

Access settings via the gear icon in the app:

- **Sound Effects**: Toggle authentic rotary dial sounds
- **Haptic Feedback**: Enable/disable vibrations when dialing
- **Dial Sensitivity**: Adjust rotation speed (0.5x - 2.0x)

## 🏗️ Technical Details

### Architecture
- **Flutter**: Cross-platform UI framework
- **Kotlin**: Android native implementation
- **Accessibility Service**: Detects dial pad appearance
- **Overlay Service**: Manages the rotary dial window
- **Method Channels**: Flutter-Native communication

### Key Components
- `OverlayService`: Manages overlay window with Flutter View
- `DialPadDetectorService`: Detects when dial pads appear
- `InputInjector`: Sends digits to system dial pad
- `RotaryDialWidget`: Renders the rotary dial interface

## 🛡️ Privacy & Safety

- **No Data Collection**: App doesn't collect or store any personal information
- **Emergency Call Protection**: Never overlays on emergency dialers
- **Minimal Permissions**: Only requests necessary permissions with clear explanations
- **Open Source**: Code is transparent and auditable

## 🐛 Troubleshooting

### Overlay doesn't appear
- Verify both permissions are granted
- Check that the service is running (notification should be visible)
- Try disabling and re-enabling the toggle

### Digits don't register
- Ensure accessibility service is enabled
- Try restarting the accessibility service
- Test with the default phone app first

### Service stops after reboot
- Check that toggle was enabled before reboot
- Verify RECEIVE_BOOT_COMPLETED permission is granted
- Disable battery optimization for the app

## 📚 Documentation

- **OVERLAY_IMPLEMENTATION.md**: Technical implementation details
- **ERROR_HANDLING_GUIDE.md**: Comprehensive error handling documentation
- **IMPLEMENTATION_COMPLETE.md**: Project completion summary

## 🔧 Development

### Build from Source

```bash
# Clone the repository
git clone <repository-url>
cd rotary_dialer

# Install dependencies
flutter pub get

# Build debug APK
flutter build apk --debug

# Build release APK
flutter build apk --release

# Run on connected device
flutter run
```

### Run Tests

```bash
# Run all tests
flutter test

# Run specific test file
flutter test test/widget_test.dart
```

## 📋 Project Status

✅ **All features implemented** (16/16 tasks complete)
✅ **Comprehensive error handling**
✅ **Settings and customization**
✅ **Documentation complete**
✅ **Ready for testing**

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

## 📜 License

This project is licensed under the [MIT License](LICENSE).


## 🙏 Acknowledgments

Inspired by the nostalgic experience of rotary phones, this app brings that tactile satisfaction to modern smartphones while maintaining full compatibility with today's phone systems.

---

**Made with ❤️ using Flutter and Kotlin**

For support or questions, please open an issue on GitHub.
