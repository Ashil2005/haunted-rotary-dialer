# Hole-First Detection Algorithm

## Problem Solved

The previous implementation had **number mis-registration** - when you touched hole "1" and rotated, it might register as "4" or another wrong number. This was because it tried to infer the number from rotation angle alone.

## Solution: Hole-First Detection

The new algorithm detects **which hole you touched first**, then tracks rotation relative to that hole.

## Algorithm Steps

### 1. Touch Detection (onPanStart)

```dart
activeHoleIndex = _detectTouchedHole(details.localPosition);
```

- Checks if touch position is within any hole's radius (24px + 50% tolerance)
- If YES: Sets `activeHoleIndex` (0-9) and starts tracking
- If NO: Ignores the touch completely

**Key Variables:**
- `activeHoleIndex`: Which hole (0-9) was touched
- `dragStartAngle`: Initial angle when touch began
- `maxDragAngle`: Maximum rotation reached (starts at 0)

### 2. Drag Tracking (onPanUpdate)

```dart
if (deltaRotation > 0) {
  _currentRotation += deltaRotation;
  _currentRotation = math.min(_currentRotation, maxRotationForHole);
  maxDragAngle = math.max(maxDragAngle, _currentRotation);
}
```

- Only tracks if `activeHoleIndex` is set
- Only allows clockwise rotation (positive delta)
- Clamps to `maxRotationForHole` (2.0 radians ~115°)
- Records maximum angle reached

**Key Variables:**
- `_currentRotation`: Current rotation angle
- `maxDragAngle`: Peak rotation (never decreases during drag)

### 3. Release Decision (onPanEnd)

#### A. Tap Detection
```dart
if (tapDuration < 200ms && maxDragAngle < 0.1 rad) {
  // Quick tap - immediately dial the digit
  widget.onNumberDialed(digit);
}
```

#### B. Drag Validation
```dart
else if (maxDragAngle >= minDigitThreshold) {
  // Dragged far enough - dial the digit
  widget.onNumberDialed(digit);
  // Spring back animation
}
```

#### C. Drag Cancelled
```dart
else {
  // Didn't drag far enough - cancel
  // Quick snap back, no digit dialed
}
```

**Key Variables:**
- `minDigitThreshold`: 0.35 radians (~20°) - minimum drag to register
- `maxRotationForHole`: 2.0 radians (~115°) - maximum allowed rotation

### 4. Digit Mapping

```dart
int _getDigitForHole(int holeIndex) {
  return numbers[holeIndex]; // [1,2,3,4,5,6,7,8,9,0]
}
```

- Hole 0 → Digit 1
- Hole 1 → Digit 2
- ...
- Hole 8 → Digit 9
- Hole 9 → Digit 0

## Key Features

### ✅ Precise Detection
- Always dials the hole you touched, not an inferred number
- No more mis-registration

### ✅ Tap Support
- Quick tap on a hole → immediately dials that digit
- No need to drag for quick input

### ✅ Drag Threshold
- Must drag at least 0.35 radians (~20°) to register
- Prevents accidental dials from small movements

### ✅ Visual Feedback
- Active hole highlights in yellow/gold
- Thicker border on active hole
- Brighter glow effect

### ✅ Debug Overlay
- Double-tap dial to toggle debug mode
- Shows:
  - Active hole index
  - Current digit
  - Start angle
  - Current angle
  - Max angle reached
  - Threshold status (will dial / too short)

## Configuration

### Adjust Sensitivity

**Make it easier to dial (lower threshold):**
```dart
static const double minDigitThreshold = 0.25; // Was 0.35
```

**Make it harder to dial (higher threshold):**
```dart
static const double minDigitThreshold = 0.50; // Was 0.35
```

### Adjust Maximum Rotation

**Allow more rotation:**
```dart
static const double maxRotationForHole = 2.5; // Was 2.0
```

**Limit rotation more:**
```dart
static const double maxRotationForHole = 1.5; // Was 2.0
```

### Adjust Tap Detection

**More sensitive tap (longer time, more movement allowed):**
```dart
if (tapDuration.inMilliseconds < 300 && tapMovement < 0.15) {
```

**Less sensitive tap (shorter time, less movement allowed):**
```dart
if (tapDuration.inMilliseconds < 150 && tapMovement < 0.05) {
```

## Debug Mode

### Enable Debug Overlay

**Double-tap the dial** to toggle debug mode.

### Debug Information Shown

1. **Active Hole**: Which hole (0-9) you're touching
2. **Digit**: The actual digit that will be dialed
3. **Start Angle**: Angle when you started dragging
4. **Current Angle**: Current rotation angle
5. **Max Angle**: Maximum angle reached during drag
6. **Threshold**: Minimum required (0.35 rad)
7. **Status**: ✅ WILL DIAL or ❌ TOO SHORT

### Console Logging

The widget logs to console:

```
🎯 Touched hole: 0 (digit: 1)
📐 Rotation: 0.45 rad, Max: 0.45 rad
✅ DRAG complete: hole 0 -> digit 1 (0.45 rad)
```

Or:

```
❌ Touch outside holes - ignored
```

Or:

```
❌ DRAG cancelled: only 0.20 rad (need 0.35)
```

Or:

```
👆 TAP detected on hole 0 -> digit 1
```

## Testing Checklist

- [x] Touch hole 1, drag → dials 1
- [x] Touch hole 5, drag → dials 5
- [x] Touch hole 9 (0), drag → dials 0
- [x] Touch outside holes → ignored
- [x] Touch hole but don't drag far enough → cancelled
- [x] Quick tap on hole → immediately dials
- [x] Active hole highlights during drag
- [x] Debug overlay shows correct info
- [x] Spring-back animation works
- [x] Haptic feedback on notches
- [x] Audio plays (if files present)

## Advantages Over Previous Algorithm

| Feature | Old Algorithm | New Algorithm |
|---------|--------------|---------------|
| **Accuracy** | ❌ Inferred from rotation | ✅ Based on touched hole |
| **Mis-registration** | ❌ Common | ✅ Eliminated |
| **Tap Support** | ❌ No | ✅ Yes |
| **Threshold** | ❌ No minimum | ✅ 0.35 rad minimum |
| **Debug Mode** | ❌ No | ✅ Yes |
| **Visual Feedback** | ⚠️ Basic | ✅ Active hole highlight |
| **Ignore Invalid Touches** | ❌ No | ✅ Yes |

## Technical Details

### Hole Detection Radius
- Hole visual radius: 24px
- Detection radius: 36px (24px × 1.5)
- Provides comfortable touch target

### Angle Calculations
- Uses `atan2(dy, dx)` for precise angles
- Handles angle wraparound (-π to π)
- Normalizes angles to 0-2π when needed

### Rotation Clamping
- Minimum: 0 radians (no backwards rotation)
- Maximum: 2.0 radians (~115°)
- Prevents over-rotation

### Animation
- Spring-back: 800ms with `Curves.easeOutBack`
- Cancel snap: 300ms with `Curves.easeOut`
- Smooth and realistic

## Future Enhancements

1. **Adjustable sensitivity** - UI slider to change `minDigitThreshold`
2. **Rotation sound** - Continuous sound while dragging
3. **Vibration patterns** - Different patterns for tap vs drag
4. **Visual trail** - Show rotation path during drag
5. **Undo last digit** - Shake to undo or dedicated button
