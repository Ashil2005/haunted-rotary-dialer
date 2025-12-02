# Rotary Dial Mechanics - Quick Reference

## How the Authentic Rotary Dial Works

### 1. Hole-First Selection

**When user touches the dial:**
```kotlin
MotionEvent.ACTION_DOWN -> {
    // Calculate which hole was touched
    val touchAngle = calculateAngle(x, y)
    selectedDigit = detectHoleAtAngle(touchAngle)
    
    if (selectedDigit != -1) {
        isDragging = true
        vibrateShort() // Haptic feedback
    }
}
```

**Detecting the hole:**
```kotlin
private fun detectHoleAtAngle(touchAngle: Float): Int {
    // Adjust for current rotation
    var adjustedAngle = touchAngle - currentRotation
    
    // Find closest digit hole (within 20 degrees)
    for (i in digits.indices) {
        var diff = abs(adjustedAngle - digitAngles[i])
        if (diff < minDiff && diff < 20f) {
            closestDigit = digits[i]
        }
    }
    
    return closestDigit
}
```

### 2. Rotation Tracking

**During drag:**
```kotlin
MotionEvent.ACTION_MOVE -> {
    if (isDragging && selectedDigit != -1) {
        val touchAngle = calculateAngle(x, y)
        var newRotation = touchAngle - initialAngle
        
        // Constrain to stop point (max 270°)
        if (newRotation > 180f) newRotation = 0f // No counter-clockwise
        currentRotation = minOf(newRotation, maxRotation)
        
        invalidate() // Redraw
    }
}
```

### 3. Release and Return

**When user releases:**
```kotlin
MotionEvent.ACTION_UP -> {
    if (isDragging && selectedDigit != -1) {
        // Check if rotated far enough (60° minimum)
        if (currentRotation >= 60f) {
            soundManager?.playClack()
            vibrateMedium()
            animateToRest(selectedDigit) // Return with ticks
        } else {
            animateToRest(-1) // Cancel, no digit
        }
    }
}
```

**Return animation:**
```kotlin
private fun animateToRest(digit: Int) {
    val animator = ValueAnimator.ofFloat(currentRotation, 0f)
    animator.duration = 400
    animator.interpolator = DecelerateInterpolator(2f)
    
    animator.addUpdateListener { animation ->
        val newRotation = animation.animatedValue as Float
        
        // Play tick every 15 degrees
        if (abs(newRotation - lastTickAngle) >= tickInterval) {
            soundManager?.playTick(0.2f)
            lastTickAngle = newRotation
        }
        
        currentRotation = newRotation
        invalidate()
    }
    
    animator.start()
    
    // Notify callback
    if (digit != -1) {
        postDelayed({ onDigitSelected?.invoke(digit) }, 100)
    }
}
```

## Key Differences from Previous Implementation

### Old Approach (Angle-Based)
```kotlin
// ❌ Selected digit based on WHERE user released
private fun detectSelectedDigit(): Int {
    val totalRotation = abs(currentRotation)
    val digitIndex = ((totalRotation / 36f) + 0.5f).toInt() % 10
    return digits[digitIndex]
}
```

### New Approach (Hole-First)
```kotlin
// ✅ Selected digit based on WHERE user started
MotionEvent.ACTION_DOWN -> {
    selectedDigit = detectHoleAtAngle(touchAngle)
    // Digit is locked in at touch, not at release
}
```

## Stop Point Mechanics

### Fixed Stop Position
```kotlin
private val stopAngle = 135f // 4-5 o'clock position
private val maxRotation = 270f // Maximum clockwise rotation
```

### Visual Indicator
```kotlin
private fun drawStopIndicator(canvas: Canvas) {
    val stopAngleRad = Math.toRadians(stopAngle.toDouble())
    val indicatorDistance = dialRadius * 1.1f
    val indicatorX = centerX + indicatorDistance * cos(stopAngleRad).toFloat()
    val indicatorY = centerY + indicatorDistance * sin(stopAngleRad).toFloat()
    
    // Draw triangle pointing inward
    val path = Path().apply {
        moveTo(indicatorX, indicatorY)
        // ... triangle points
    }
    canvas.drawPath(path, indicatorPaint)
}
```

### Rotation Constraint
```kotlin
// Prevent rotation beyond stop
if (newRotation > 180f) newRotation = 0f // No counter-clockwise
currentRotation = minOf(newRotation, maxRotation) // Cap at 270°
```

## 3D Metallic Rendering

### Dial Plate Gradient
```kotlin
val gradient = RadialGradient(
    centerX, centerY - dialRadius * 0.2f, dialRadius,
    intArrayOf(
        Color.parseColor("#4A4A4A"), // Highlight (top-left)
        Color.parseColor("#2D2D2D"), // Mid
        Color.parseColor("#1A1A1A")  // Shadow (edges)
    ),
    floatArrayOf(0f, 0.5f, 1f),
    Shader.TileMode.CLAMP
)
dialPaint.shader = gradient
```

### Finger Hole 3D Effect
```kotlin
private fun drawFingerHole3D(canvas: Canvas, digit: Int, angle: Float) {
    // Outer ring (brass)
    holePaint.color = Color.parseColor("#D4AF37")
    canvas.drawCircle(holeX, holeY, holeRadius, holePaint)
    
    // Inner shadow (depth)
    val shadowGradient = RadialGradient(
        holeX - holeRadius * 0.3f, holeY - holeRadius * 0.3f, holeRadius,
        intArrayOf(
            Color.parseColor("#1A1A1A"), // Dark center
            Color.parseColor("#3D3D3D")  // Lighter edge
        ),
        floatArrayOf(0f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawCircle(holeX, holeY, holeRadius * 0.7f, innerPaint)
    
    // Digit text
    canvas.drawText(digit.toString(), holeX, textY, textPaint)
}
```

### Drop Shadow
```kotlin
// Draw shadow before dial
shadowPaint.color = Color.argb(100, 0, 0, 0)
shadowPaint.maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
canvas.drawCircle(centerX + 5f, centerY + 5f, dialRadius, shadowPaint)
```

## Sound & Haptic Integration

### Sound Manager Usage
```kotlin
// Initialize
soundManager = RotarySoundManager(context)

// Play sounds
soundManager?.playClack() // On release
soundManager?.playTick(0.2f) // During return

// Cleanup
soundManager?.release()
```

### Haptic Feedback
```kotlin
private fun vibrateShort() {
    vibrator?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            it.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            it.vibrate(20)
        }
    }
}

private fun vibrateMedium() {
    vibrator?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            it.vibrate(50)
        }
    }
}
```

## Number Display Integration

### In OverlayService
```kotlin
// Track dialed number
private var dialedNumber = StringBuilder()
private var numberDisplay: TextView? = null

// On digit selected
rotaryDialView?.onDigitSelected = { digit ->
    // Add to our number
    dialedNumber.append(digit)
    updateNumberDisplay()
    
    // Inject to system dialer
    inputInjector?.injectDigit(digit.toString())
}

// Update display
private fun updateNumberDisplay() {
    numberDisplay?.text = if (dialedNumber.isEmpty()) {
        ""
    } else {
        dialedNumber.toString()
    }
}
```

### Delete Handler
```kotlin
deleteButton?.setOnClickListener {
    if (dialedNumber.isNotEmpty()) {
        // Remove from our number
        dialedNumber.deleteCharAt(dialedNumber.length - 1)
        updateNumberDisplay()
        
        // Delete from system dialer
        inputInjector?.deleteLastDigit()
    }
}
```

### Call Handler
```kotlin
callButton?.setOnClickListener {
    if (dialedNumber.isNotEmpty()) {
        // Click system call button
        val success = inputInjector?.clickCallButton() ?: false
        
        if (success) {
            // Clear after call
            dialedNumber.clear()
            updateNumberDisplay()
        }
    }
}
```

## Accessibility Actions

### Delete via Accessibility
```kotlin
fun deleteLastDigit(): Boolean {
    // Method 1: Click backspace button
    if (clickBackspaceButton()) {
        return true
    }
    
    // Method 2: Text manipulation
    val numberField = detectorService.getCurrentInputField()
    val currentText = getCurrentText(numberField)
    val newText = currentText.dropLast(1)
    
    val args = Bundle().apply {
        putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            newText
        )
    }
    return numberField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
}
```

### Call via Accessibility
```kotlin
fun clickCallButton(): Boolean {
    val rootNode = accessibilityService.rootInActiveWindow
    
    // Find call button
    val callPatterns = listOf(
        "call", "dial", "dialpad_voice_call",
        "floating_action_button", "fab"
    )
    
    val button = findButtonByPatterns(rootNode, callPatterns)
    if (button != null && isNodeValid(button)) {
        return button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
    
    return false
}
```

## Configuration Constants

### Timing
```kotlin
animator.duration = 400 // Return animation duration (ms)
postDelayed({ ... }, 100) // Callback delay (ms)
```

### Angles
```kotlin
private val stopAngle = 135f // Stop position (degrees)
private val maxRotation = 270f // Max rotation (degrees)
private val tickInterval = 15f // Tick sound interval (degrees)
```

### Thresholds
```kotlin
if (currentRotation >= 60f) // Minimum rotation to register
if (diff < 20f) // Hole detection tolerance (degrees)
```

### Sizes
```kotlin
dialRadius = minOf(w, h) / 2f * 0.85f // 85% of available space
holeRadius = dialRadius * 0.12f // 12% of dial radius
holeDistance = dialRadius * 0.65f // 65% from center
hubRadius = dialRadius * 0.18f // 18% for center hub
```

### Vibration
```kotlin
vibrateShort: 20ms // On touch
vibrateMedium: 50ms // On release
```

## Testing the Mechanics

### Test Hole Selection
1. Touch different holes
2. Check logs: "Selected digit hole: X"
3. Verify correct digit is selected

### Test Rotation Constraint
1. Try rotating counter-clockwise → Should not move
2. Try rotating past stop → Should stop at 270°
3. Release before 60° → Should cancel

### Test Return Animation
1. Rotate and release
2. Watch dial return smoothly
3. Listen for tick sounds (if enabled)
4. Feel vibration on release

### Test Number Display
1. Dial several digits
2. Verify display updates
3. Verify system dialer updates
4. Test delete button
5. Test call button

## Troubleshooting

### Digit Not Detected
- Check hole detection tolerance (20°)
- Verify touch is within dial radius
- Check logs for "Selected digit hole"

### Rotation Feels Wrong
- Adjust maxRotation (270°)
- Adjust minimum rotation (60°)
- Check interpolator (DecelerateInterpolator)

### No Sounds
- Add audio files to res/raw/
- Uncomment sound loading code
- Check SoundPool initialization

### No Haptics
- Verify VIBRATE permission
- Check vibrator initialization
- Test on different device

## Summary

The authentic rotary dial mechanics provide:
- **Hole-first selection** - Digit chosen at touch, not release
- **Fixed stop point** - Realistic mechanical constraint
- **Smooth return** - Deceleration with tick sounds
- **3D metallic UI** - Gradients and shadows for depth
- **Haptic feedback** - Touch and release vibrations
- **Number tracking** - Real-time display and sync

This creates a faithful recreation of vintage rotary phone behavior while seamlessly integrating with modern Android dialers.
