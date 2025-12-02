# Physics Tuning Guide

## Overview

The rotary dial now has **realistic spring-back physics** and **precise notch detection** with visual/audio/haptic feedback.

## Top-of-File Constants

All physics parameters are defined at the top of `lib/widgets/rotary_dial_widget.dart`:

```dart
const double NOTCH_ANGLE = math.pi / 5;        // 36° per notch
const double MIN_DIGIT_THRESHOLD = 0.35;       // 20° minimum
const double MAX_ROTATION = 2.0;               // 115° maximum
const int SPRING_DURATION_MS = 800;            // Spring-back time
const Curve SPRING_CURVE = Curves.elasticOut;  // Spring curve
```

## Tuning Parameters

### 1. NOTCH_ANGLE (Notch Frequency)

**What it does:** Controls how often you feel/hear clicks while rotating.

**Default:** `math.pi / 5` ≈ 0.628 rad ≈ 36° (10 notches for 10 digits)

**Tuning:**
```dart
// More frequent clicks (feels more mechanical)
const double NOTCH_ANGLE = math.pi / 6;  // 30° per notch (12 notches)

// Less frequent clicks (smoother feel)
const double NOTCH_ANGLE = math.pi / 4;  // 45° per notch (8 notches)

// Very frequent (ratchet-like)
const double NOTCH_ANGLE = math.pi / 10; // 18° per notch (20 notches)
```

**Effect:**
- Smaller angle = more clicks = more tactile feedback
- Larger angle = fewer clicks = smoother rotation

---

### 2. MIN_DIGIT_THRESHOLD (Dial Sensitivity)

**What it does:** Minimum rotation required to register a digit.

**Default:** `0.35` rad ≈ 20°

**Tuning:**
```dart
// Easier to dial (more sensitive)
const double MIN_DIGIT_THRESHOLD = 0.25;  // 14° minimum

// Harder to dial (less sensitive, prevents accidents)
const double MIN_DIGIT_THRESHOLD = 0.50;  // 29° minimum

// Very easy (almost any movement registers)
const double MIN_DIGIT_THRESHOLD = 0.15;  // 9° minimum
```

**Effect:**
- Lower value = easier to dial = more accidental dials
- Higher value = harder to dial = fewer accidents

---

### 3. MAX_ROTATION (Rotation Limit)

**What it does:** Maximum rotation allowed before clamping.

**Default:** `2.0` rad ≈ 115°

**Tuning:**
```dart
// Allow more rotation (feels looser)
const double MAX_ROTATION = 2.5;  // 143° maximum

// Limit rotation more (feels tighter)
const double MAX_ROTATION = 1.5;  // 86° maximum

// Very loose (almost full circle)
const double MAX_ROTATION = 3.0;  // 172° maximum
```

**Effect:**
- Higher value = can rotate further = feels looser
- Lower value = stops sooner = feels tighter/more controlled

---

### 4. SPRING_DURATION_MS (Spring-Back Speed)

**What it does:** How long the spring-back animation takes.

**Default:** `800` ms (feels heavy and mechanical)

**Tuning:**
```dart
// Faster spring-back (snappier feel)
const int SPRING_DURATION_MS = 500;  // Half second

// Slower spring-back (heavier feel)
const int SPRING_DURATION_MS = 1200; // Over a second

// Very fast (toy-like)
const int SPRING_DURATION_MS = 300;  // Quick snap
```

**Effect:**
- Lower value = faster return = lighter feel
- Higher value = slower return = heavier feel

---

### 5. SPRING_CURVE (Spring-Back Feel)

**What it does:** The animation curve for spring-back.

**Default:** `Curves.elasticOut` (bouncy spring effect)

**Tuning:**
```dart
// Less bounce (more damped)
const Curve SPRING_CURVE = Curves.easeOutBack;

// No bounce (smooth deceleration)
const Curve SPRING_CURVE = Curves.easeOut;

// More bounce (springy)
const Curve SPRING_CURVE = Curves.elasticOut;

// Very bouncy (toy-like)
const Curve SPRING_CURVE = Curves.bounceOut;

// Linear (mechanical, no easing)
const Curve SPRING_CURVE = Curves.linear;
```

**Effect:**
- `elasticOut`: Bouncy spring (default, realistic)
- `easeOutBack`: Slight overshoot then settle
- `easeOut`: Smooth deceleration, no bounce
- `bounceOut`: Multiple bounces (toy-like)
- `linear`: Constant speed (mechanical)

---

## Notch Detection Algorithm

### How It Works

1. **Absolute Tracking**: Tracks rotation from drag start, not current position
2. **Clockwise Only**: Only detects notches when rotating clockwise
3. **Fast Drag Support**: Registers all notches even in fast drags
4. **Per-Notch Events**: Each notch triggers:
   - Haptic feedback (`HapticFeedback.selectionClick()`)
   - Click sound (`click.wav`)
   - Visual flash (yellow radial highlight)

### Key Code

```dart
void _checkNotchCrossing() {
  // Calculate absolute rotation from drag start
  final absoluteRotation = _currentRotation - _dragStartRotation;
  
  // Calculate which notch we're at
  final currentNotchIndex = (absoluteRotation / NOTCH_ANGLE).floor();
  
  // Emit events for each new notch crossed
  if (currentNotchIndex > _lastNotchIndex && currentNotchIndex >= 0) {
    for (int i = _lastNotchIndex + 1; i <= currentNotchIndex; i++) {
      _emitNotchEvent(i);
    }
    _lastNotchIndex = currentNotchIndex;
  }
}
```

### Visual Flash

- **Duration**: 150ms
- **Color**: Yellow (#ffff00) with 60% opacity
- **Effect**: Radial blur at notch position
- **Multiple**: Can have multiple flashes active simultaneously

---

## Preset Configurations

### Heavy Mechanical (Default)
```dart
const double NOTCH_ANGLE = math.pi / 5;
const double MIN_DIGIT_THRESHOLD = 0.35;
const double MAX_ROTATION = 2.0;
const int SPRING_DURATION_MS = 800;
const Curve SPRING_CURVE = Curves.elasticOut;
```
**Feel:** Heavy, mechanical, realistic rotary phone

---

### Light & Snappy
```dart
const double NOTCH_ANGLE = math.pi / 6;
const double MIN_DIGIT_THRESHOLD = 0.25;
const double MAX_ROTATION = 2.5;
const int SPRING_DURATION_MS = 500;
const Curve SPRING_CURVE = Curves.easeOutBack;
```
**Feel:** Lighter, faster, more responsive

---

### Tight & Precise
```dart
const double NOTCH_ANGLE = math.pi / 4;
const double MIN_DIGIT_THRESHOLD = 0.50;
const double MAX_ROTATION = 1.5;
const int SPRING_DURATION_MS = 600;
const Curve SPRING_CURVE = Curves.easeOut;
```
**Feel:** Controlled, precise, no accidents

---

### Ratchet-Like
```dart
const double NOTCH_ANGLE = math.pi / 10;
const double MIN_DIGIT_THRESHOLD = 0.30;
const double MAX_ROTATION = 2.0;
const int SPRING_DURATION_MS = 700;
const Curve SPRING_CURVE = Curves.elasticOut;
```
**Feel:** Very tactile, lots of clicks, mechanical

---

## Testing Your Changes

1. **Edit** `lib/widgets/rotary_dial_widget.dart`
2. **Change** constants at top of file
3. **Hot reload** (press `r` in terminal) or rebuild:
   ```bash
   flutter run
   ```
4. **Test** by dragging the dial
5. **Observe**:
   - How many clicks you feel/hear
   - How far you need to drag
   - How the spring-back feels
6. **Iterate** until it feels right

## Debug Mode

**Enable:** Double-tap the dial

**Shows:**
- Current rotation angle
- Maximum angle reached
- Current notch index
- Whether drag will register

**Use for:**
- Understanding threshold behavior
- Tuning MIN_DIGIT_THRESHOLD
- Verifying notch detection

## Audio Integration

Notch clicks use `click.wav` at 60% volume:

```dart
await _audioPlayer.play(AssetSource('sounds/click.wav'), volume: 0.6);
```

**Adjust volume:**
```dart
volume: 0.4  // Quieter
volume: 0.8  // Louder
```

## Performance Notes

- Notch detection runs on every `onPanUpdate` (60fps)
- Visual flashes are lightweight (150ms duration)
- Audio playback is async (non-blocking)
- Haptic feedback is instant

## Troubleshooting

### Too many/few clicks
→ Adjust `NOTCH_ANGLE`

### Too easy/hard to dial
→ Adjust `MIN_DIGIT_THRESHOLD`

### Spring-back too fast/slow
→ Adjust `SPRING_DURATION_MS`

### Spring-back too bouncy
→ Change `SPRING_CURVE` to `Curves.easeOutBack` or `Curves.easeOut`

### Can rotate too far
→ Decrease `MAX_ROTATION`

### Notches not registering in fast drags
→ Already fixed! Uses absolute rotation tracking

## Advanced: Custom Curves

Create your own spring curve:

```dart
// Custom cubic curve
const Curve SPRING_CURVE = Cubic(0.25, 0.1, 0.25, 1.0);

// Custom elastic with parameters
final Curve SPRING_CURVE = ElasticOutCurve(period: 0.4);
```

See: https://api.flutter.dev/flutter/animation/Curves-class.html
