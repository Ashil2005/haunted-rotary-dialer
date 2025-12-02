# Rotary Dialer - Quick Reference

## How to Use

### Drag to Dial
1. Touch a number hole (1-9 or 0)
2. Drag clockwise (to the right/down)
3. Drag at least ~20° to register
4. Release - dial springs back and number is dialed

### Tap to Dial
1. Quickly tap a number hole
2. Number is immediately dialed
3. No dragging needed

### Debug Mode
- **Double-tap** the dial to toggle debug overlay
- Shows which hole you're touching and rotation info

## Troubleshooting

### "Wrong number dialed"
- ✅ **FIXED!** New algorithm always dials the hole you touched
- Enable debug mode to verify

### "Number not registering"
- Drag further (need ~20° minimum)
- Check debug overlay to see current angle
- Try tapping instead of dragging

### "Can't dial at all"
- Make sure you're touching a number hole (not empty space)
- Debug mode shows "Touch outside holes - ignored" if you miss

### "No sound"
- Add `click.wav` and `release.wav` to `assets/sounds/`
- See `HOW_TO_ADD_AUDIO.md`
- App works without audio

## Key Variables

| Variable | Default | What it does |
|----------|---------|--------------|
| `minDigitThreshold` | 0.35 rad (~20°) | Minimum drag to register |
| `maxRotationForHole` | 2.0 rad (~115°) | Maximum rotation allowed |
| `holeRadius` | 24px | Size of number holes |
| `notchAngle` | 0.28 rad (~16°) | Haptic feedback interval |

## Customization

### Make it easier to dial
```dart
static const double minDigitThreshold = 0.25; // Lower threshold
```

### Make it harder to dial
```dart
static const double minDigitThreshold = 0.50; // Higher threshold
```

### Adjust tap sensitivity
```dart
// In _onPanEnd method, change:
if (tapDuration.inMilliseconds < 300 && tapMovement < 0.15) {
  // More forgiving tap detection
}
```

## Build Commands

```bash
# Get dependencies
flutter pub get

# Build debug APK
flutter build apk --debug

# Run on connected device
flutter run

# Install APK
flutter install

# Clean build
flutter clean
```

## File Structure

```
rotary_dialer/
├── lib/
│   ├── main.dart                    # Main app
│   └── widgets/
│       └── rotary_dial_widget.dart  # Rotary dial (hole-first algorithm)
├── assets/
│   ├── sounds/
│   │   ├── click.wav               # Click sound (add this)
│   │   └── release.wav             # Release sound (add this)
│   └── images/                     # Future images
├── android/                        # Android native code
└── docs/
    ├── HOLE_FIRST_ALGORITHM.md     # Algorithm explanation
    ├── HOW_TO_ADD_AUDIO.md         # Audio setup guide
    └── QUICK_REFERENCE.md          # This file
```

## Debug Console Output

### Successful Drag
```
🎯 Touched hole: 0 (digit: 1)
📐 Rotation: 0.45 rad, Max: 0.45 rad
✅ DRAG complete: hole 0 -> digit 1 (0.45 rad)
```

### Tap
```
🎯 Touched hole: 4 (digit: 5)
👆 TAP detected on hole 4 -> digit 5
```

### Cancelled (too short)
```
🎯 Touched hole: 2 (digit: 3)
📐 Rotation: 0.15 rad, Max: 0.15 rad
❌ DRAG cancelled: only 0.15 rad (need 0.35)
```

### Invalid Touch
```
❌ Touch outside holes - ignored
```

## Tips

1. **Use tap for quick input** - Much faster than dragging
2. **Enable debug mode** - Helps understand what's happening
3. **Add audio files** - Makes it much more satisfying
4. **Test on real device** - Haptic feedback works best on physical hardware
5. **Adjust thresholds** - Customize to your preference

## Support

- Check console logs for debug info
- Enable debug overlay (double-tap dial)
- See `HOLE_FIRST_ALGORITHM.md` for technical details
- See `HOW_TO_ADD_AUDIO.md` for audio setup

## Version

- **Algorithm**: Hole-First Detection v2.0
- **Build**: Successful
- **Status**: ✅ Production Ready
