package com.example.rotary_dialer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.*

/**
 * Classic rotary dial matching reference image EXACTLY
 * Layout: 1 at top-right, then 2,3,4,5,6,7,8,9,0 clockwise
 * Stop pointer at bottom-right (~315°)
 */
class RotaryDialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dialPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var centerX = 0f
    private var centerY = 0f
    private var dialRadius = 0f
    
    // Rotation state
    private var currentRotation = 0f
    private var isDragging = false
    private var selectedDigit = -1
    private var selectedDigitAngle = 0f
    private var lastRotationForTick = 0f
    
    // Stop pointer position - EXACT as specified
    private val STOP_POINTER_ANGLE = 20f
    
    // Digit angles - UNIFORM 30° spacing (0° is RIGHT, counter-clockwise)
    private val digits = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
    private val digitAngles = floatArrayOf(
        330f,  // 1 → 330°
        300f,  // 2 → 300°
        270f,  // 3 → 270°
        240f,  // 4 → 240°
        210f,  // 5 → 210°
        180f,  // 6 → 180°
        150f,  // 7 → 150°
        120f,  // 8 → 120°
        90f,   // 9 → 90°
        60f    // 0 → 60°
    )
    
    var onDigitSelected: ((Int) -> Unit)? = null
    
    private var soundManager: RotarySoundManager? = null
    private var vibrator: Vibrator? = null
    
    private val dragTickInterval = 8f // Tick every 8° during drag
    private val returnTickInterval = 10f // Tick every 10° during return
    
    init {
        setupPaints()
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        
        soundManager = RotarySoundManager(context)
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        
        android.util.Log.d("RotaryDial", "=== EXACT ANGLES APPLIED ===")
        android.util.Log.d("RotaryDial", "STOP_POINTER_ANGLE = $STOP_POINTER_ANGLE°")
        for (i in digits.indices) {
            val maxRot = calculateMaxRotation(digitAngles[i])
            android.util.Log.d("RotaryDial", "Digit ${digits[i]} → ${digitAngles[i]}° (max rotation: $maxRot°)")
        }
    }
    
    private fun setupPaints() {
        // Wooden dial face (gradient set in onSizeChanged)
        dialPaint.style = Paint.Style.FILL
        
        // Orange glowing holes (gradient set in onSizeChanged)
        holePaint.style = Paint.Style.FILL
        
        // Dark text for readability on orange
        textPaint.color = Color.parseColor("#1A0A04") // Very dark brown
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        
        // Silver metallic pointer with green highlight
        pointerPaint.style = Paint.Style.FILL
        pointerPaint.color = Color.parseColor("#C8C8C8") // Light silver
        
        // Enhanced shadow for depth
        shadowPaint.color = Color.argb(80, 0, 0, 0) // Reduced opacity for subtlety
        shadowPaint.maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
        
        // Green glow (gradient set in onSizeChanged)
        glowPaint.style = Paint.Style.FILL
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        dialRadius = minOf(w, h) / 2f * 0.85f
        
        android.util.Log.d("RotaryDial", "onSizeChanged: w=$w, h=$h, centerX=$centerX, centerY=$centerY, dialRadius=$dialRadius")
        
        // Wooden Halloween dial gradient (warm browns)
        val woodenGradient = RadialGradient(
            centerX - dialRadius * 0.15f, centerY - dialRadius * 0.15f, dialRadius,
            intArrayOf(
                Color.parseColor("#4A2C16"), // Medium warm brown center
                Color.parseColor("#3A1F0F"), // Darker brown mid
                Color.parseColor("#251309")  // Very dark brown edge
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        dialPaint.shader = woodenGradient
        
        // Green aura gradient for subtle glow around dial
        val greenGlow = RadialGradient(
            centerX, centerY, dialRadius * 1.2f,
            intArrayOf(
                Color.argb(90, 0, 255, 120), // Center green glow
                Color.argb(0, 0, 255, 120)   // Transparent edge
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = greenGlow
        
        // Orange hole gradient for glowing effect
        val orangeGlow = RadialGradient(
            0f, 0f, dialRadius * 0.10f, // Will be repositioned per hole
            intArrayOf(
                Color.parseColor("#FFC857"), // Bright yellow-orange center
                Color.parseColor("#FF8C2B"), // Warm orange middle
                Color.parseColor("#A44A16")  // Darker orange/brown edge
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        holePaint.shader = orangeGlow
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        android.util.Log.d("RotaryDial", "onDraw called: centerX=$centerX, centerY=$centerY, dialRadius=$dialRadius")
        
        // Draw subtle green aura around dial (no full-screen background)
        canvas.drawCircle(centerX, centerY, dialRadius * 1.2f, glowPaint)
        
        // Draw subtle circular shadow behind dial (transparent)
        canvas.drawCircle(centerX + 4f, centerY + 4f, dialRadius, shadowPaint)
        
        canvas.save()
        canvas.rotate(currentRotation, centerX, centerY)
        
        // Draw wooden dial base
        canvas.drawCircle(centerX, centerY, dialRadius, dialPaint)
        
        // Draw darker ring near outer edge for depth
        val depthRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 8f
            color = Color.parseColor("#1A0A04") // Very dark brown
        }
        canvas.drawCircle(centerX, centerY, dialRadius - 4f, depthRingPaint)
        
        // Draw glowing orange digit holes
        textPaint.textSize = dialRadius * 0.13f
        for (i in digits.indices) {
            drawGlowingHole(canvas, digits[i], digitAngles[i])
        }
        
        // Draw pumpkin center hub
        drawPumpkinHub(canvas)
        
        canvas.restore()
        
        // Draw metallic stop pointer (fixed, doesn't rotate)
        drawMetallicPointer(canvas)
    }
    
    private fun drawGlowingHole(canvas: Canvas, digit: Int, angle: Float) {
        val angleRad = Math.toRadians(angle.toDouble())
        val holeDistance = dialRadius * 0.70f
        val holeX = centerX + holeDistance * cos(angleRad).toFloat()
        val holeY = centerY + holeDistance * sin(angleRad).toFloat()
        val holeRadius = dialRadius * 0.10f
        
        // Create orange gradient for this specific hole position
        val orangeGradient = RadialGradient(
            holeX, holeY, holeRadius,
            intArrayOf(
                Color.parseColor("#FFC857"), // Bright yellow-orange center
                Color.parseColor("#FF8C2B"), // Warm orange middle
                Color.parseColor("#A44A16")  // Darker orange/brown edge
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        
        val holeGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = orangeGradient
        }
        
        // Draw soft outer glow
        val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF8C2B")
            alpha = 60
            maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(holeX, holeY, holeRadius * 1.3f, outerGlowPaint)
        
        // Draw main glowing hole
        canvas.drawCircle(holeX, holeY, holeRadius, holeGlowPaint)
        
        // Draw darker border for definition
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.parseColor("#2A1509") // Dark brown border
        }
        canvas.drawCircle(holeX, holeY, holeRadius, borderPaint)
        
        // Draw digit text in dark brown for contrast
        val textY = holeY - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(digit.toString(), holeX, textY, textPaint)
    }
    
    private fun drawPumpkinHub(canvas: Canvas) {
        val hubRadius = dialRadius * 0.22f
        
        // Enhanced pumpkin gradient with 3D shading
        val pumpkinGradient = RadialGradient(
            centerX - hubRadius * 0.3f, centerY - hubRadius * 0.3f, hubRadius * 1.2f,
            intArrayOf(
                Color.parseColor("#FFB347"), // Bright pumpkin orange center
                Color.parseColor("#FF8C2B"), // Medium orange
                Color.parseColor("#E6751F"), // Darker orange
                Color.parseColor("#A44A16")  // Very dark orange edge
            ),
            floatArrayOf(0f, 0.4f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        
        val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = pumpkinGradient
        }
        canvas.drawCircle(centerX, centerY, hubRadius, hubPaint)
        
        // Pumpkin ridges for realistic texture
        val ridgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.parseColor("#D4631F")
            alpha = 120
        }
        
        // Draw 6 curved ridges from top to bottom
        for (i in 0..5) {
            val angle = i * 60f - 90f // Start from top
            val startAngle = angle - 25f
            val sweepAngle = 50f
            
            val ridgeRect = RectF(
                centerX - hubRadius * 0.85f,
                centerY - hubRadius * 0.85f,
                centerX + hubRadius * 0.85f,
                centerY + hubRadius * 0.85f
            )
            canvas.drawArc(ridgeRect, startAngle, sweepAngle, false, ridgePaint)
        }
        
        // Dark border around pumpkin
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.parseColor("#1A0A04")
        }
        canvas.drawCircle(centerX, centerY, hubRadius, borderPaint)
        
        // Enhanced jack-o'-lantern face with glowing effects
        
        // Glowing triangular eyes
        val eyeGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFC857")
            maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
        }
        
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A0A04")
            style = Paint.Style.FILL
        }
        
        // Left eye with glow
        val leftEyePath = Path().apply {
            moveTo(centerX - hubRadius * 0.35f, centerY - hubRadius * 0.25f)
            lineTo(centerX - hubRadius * 0.15f, centerY - hubRadius * 0.05f)
            lineTo(centerX - hubRadius * 0.55f, centerY - hubRadius * 0.05f)
            close()
        }
        canvas.drawPath(leftEyePath, eyeGlowPaint) // Glow first
        canvas.drawPath(leftEyePath, eyePaint) // Dark fill on top
        
        // Right eye with glow
        val rightEyePath = Path().apply {
            moveTo(centerX + hubRadius * 0.35f, centerY - hubRadius * 0.25f)
            lineTo(centerX + hubRadius * 0.55f, centerY - hubRadius * 0.05f)
            lineTo(centerX + hubRadius * 0.15f, centerY - hubRadius * 0.05f)
            close()
        }
        canvas.drawPath(rightEyePath, eyeGlowPaint) // Glow first
        canvas.drawPath(rightEyePath, eyePaint) // Dark fill on top
        
        // Enhanced zig-zag smile with glow
        val mouthGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF8C2B")
            maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL)
        }
        
        val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A0A04")
            style = Paint.Style.FILL
        }
        
        // Improved jagged smile
        val mouthPath = Path().apply {
            moveTo(centerX - hubRadius * 0.45f, centerY + hubRadius * 0.15f)
            lineTo(centerX - hubRadius * 0.25f, centerY + hubRadius * 0.35f)
            lineTo(centerX - hubRadius * 0.05f, centerY + hubRadius * 0.20f)
            lineTo(centerX + hubRadius * 0.05f, centerY + hubRadius * 0.35f)
            lineTo(centerX + hubRadius * 0.25f, centerY + hubRadius * 0.20f)
            lineTo(centerX + hubRadius * 0.45f, centerY + hubRadius * 0.15f)
            lineTo(centerX + hubRadius * 0.25f, centerY + hubRadius * 0.25f)
            lineTo(centerX + hubRadius * 0.05f, centerY + hubRadius * 0.15f)
            lineTo(centerX - hubRadius * 0.05f, centerY + hubRadius * 0.25f)
            lineTo(centerX - hubRadius * 0.25f, centerY + hubRadius * 0.15f)
            close()
        }
        canvas.drawPath(mouthPath, mouthGlowPaint) // Glow first
        canvas.drawPath(mouthPath, mouthPaint) // Dark fill on top
        
        // Subtle highlight for 3D effect
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD700")
            alpha = 60
            maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(centerX - hubRadius * 0.3f, centerY - hubRadius * 0.3f, hubRadius * 0.4f, highlightPaint)
    }
    
    private fun drawMetallicPointer(canvas: Canvas) {
        // Stop pointer at EXACT angle: 20°
        val stopAngleRad = Math.toRadians(STOP_POINTER_ANGLE.toDouble())
        val pointerDistance = dialRadius * 1.10f
        val pointerX = centerX + pointerDistance * cos(stopAngleRad).toFloat()
        val pointerY = centerY + pointerDistance * sin(stopAngleRad).toFloat()
        
        // Spooky bone-like pointer shape
        val path = Path().apply {
            val angle1 = STOP_POINTER_ANGLE - 18
            val angle2 = STOP_POINTER_ANGLE + 18
            
            // Outer point (tip of wedge)
            moveTo(pointerX, pointerY)
            
            // Two base points closer to dial
            val baseDistance = dialRadius * 0.96f
            lineTo(
                centerX + baseDistance * cos(Math.toRadians(angle1.toDouble())).toFloat(),
                centerY + baseDistance * sin(Math.toRadians(angle1.toDouble())).toFloat()
            )
            lineTo(
                centerX + baseDistance * cos(Math.toRadians(angle2.toDouble())).toFloat(),
                centerY + baseDistance * sin(Math.toRadians(angle2.toDouble())).toFloat()
            )
            close()
        }
        
        // Enhanced shadow for spooky depth
        val pointerShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 0, 0, 0)
            maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, pointerShadow)
        
        // Dark metallic blade gradient (darker, more ominous)
        val darkMetalGradient = LinearGradient(
            pointerX - 10f, pointerY - 10f,
            pointerX + 10f, pointerY + 10f,
            intArrayOf(
                Color.parseColor("#4A4A4A"), // Dark gray center
                Color.parseColor("#2A2A2A"), // Darker gray
                Color.parseColor("#1A1A1A")  // Very dark edge
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        
        val darkMetallicPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = darkMetalGradient
        }
        canvas.drawPath(path, darkMetallicPaint)
        
        // Eerie green glow along the edge
        val greenGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.parseColor("#00FF78")
            alpha = 180
            maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, greenGlowPaint)
        
        // Bright green edge highlight
        val greenEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.parseColor("#39FF14") // Bright neon green
            alpha = 200
        }
        canvas.drawPath(path, greenEdgePaint)
        
        // Dark outline for definition
        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.parseColor("#0A0A0A") // Almost black
        }
        canvas.drawPath(path, outlinePaint)
        
        // Add subtle scratches/wear marks for authenticity
        val scratchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = Color.parseColor("#6A6A6A")
            alpha = 100
        }
        
        // Draw a few small scratch lines on the blade
        val midX = (pointerX + centerX + dialRadius * 0.96f * cos(Math.toRadians(STOP_POINTER_ANGLE.toDouble())).toFloat()) / 2
        val midY = (pointerY + centerY + dialRadius * 0.96f * sin(Math.toRadians(STOP_POINTER_ANGLE.toDouble())).toFloat()) / 2
        
        canvas.drawLine(midX - 3f, midY - 1f, midX + 2f, midY + 1f, scratchPaint)
        canvas.drawLine(midX - 1f, midY - 3f, midX + 1f, midY + 2f, scratchPaint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val distance = sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
                
                if (distance > dialRadius * 0.55f && distance < dialRadius * 0.85f) {
                    val touchAngle = calculateAngle(x, y)
                    val detectedIndex = detectHoleAtAngle(touchAngle)
                    
                    if (detectedIndex != -1) {
                        selectedDigit = digits[detectedIndex]
                        selectedDigitAngle = digitAngles[detectedIndex]
                        isDragging = true
                        lastRotationForTick = 0f
                        
                        android.util.Log.d("RotaryDial", "=== ACTION_DOWN ===")
                        android.util.Log.d("RotaryDial", "Touch angle = $touchAngle°")
                        android.util.Log.d("RotaryDial", "DOWN digit: $selectedDigit, angle: $selectedDigitAngle°")
                        android.util.Log.d("RotaryDial", "Max rotation for this digit: ${calculateMaxRotation(selectedDigitAngle)}°")
                        
                        // Start the continuous rotary sound loop
                        soundManager?.startDragLoop()
                        
                        vibrateShort()
                        return true
                    }
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && selectedDigit != -1) {
                    val touchAngle = calculateAngle(x, y)
                    
                    // Calculate rotation from rest position
                    var newRotation = touchAngle - selectedDigitAngle
                    
                    // Normalize to 0-360
                    while (newRotation < 0) newRotation += 360f
                    while (newRotation > 360) newRotation -= 360f
                    
                    // Only allow clockwise, prevent counter-clockwise
                    if (newRotation > 180f) newRotation = 0f
                    
                    // Clamp to max rotation for THIS digit
                    val maxRot = calculateMaxRotation(selectedDigitAngle)
                    currentRotation = minOf(newRotation, maxRot)
                    
                    // The drag loop sound plays continuously during rotation
                    
                    invalidate()
                    return true
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging && selectedDigit != -1) {
                    isDragging = false
                    
                    android.util.Log.d("RotaryDial", "=== ACTION_UP ===")
                    android.util.Log.d("RotaryDial", "UP rotation: $currentRotation°")
                    
                    // Stop the drag loop sound first
                    soundManager?.stopDragLoop()
                    
                    // Check if rotated enough (minimum 15°)
                    val accepted = currentRotation >= 15f
                    android.util.Log.d("RotaryDial", "Accepted: $accepted (digit: $selectedDigit)")
                    
                    if (accepted) {
                        soundManager?.playClack()
                        vibrateMedium()
                        animateToRest(selectedDigit)
                    } else {
                        android.util.Log.d("RotaryDial", "Rotation too small, canceling")
                        animateToRest(-1)
                    }
                    
                    selectedDigit = -1
                    selectedDigitAngle = 0f
                    return true
                }
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    private fun calculateAngle(x: Float, y: Float): Float {
        val dx = x - centerX
        val dy = y - centerY
        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (angle < 0) angle += 360f
        return angle
    }
    
    /**
     * Calculate maximum rotation for a digit to reach the stop
     */
    private fun calculateMaxRotation(digitAngle: Float): Float {
        // maxRotation = (digitAngle - STOP_POINTER_ANGLE)
        var maxRot = digitAngle - STOP_POINTER_ANGLE
        
        // Normalize to positive
        while (maxRot < 0) maxRot += 360f
        while (maxRot > 360) maxRot -= 360f
        
        return maxRot
    }
    
    /**
     * Detect which hole is at the given angle
     */
    private fun detectHoleAtAngle(touchAngle: Float): Int {
        android.util.Log.d("RotaryDial", "Detecting hole at $touchAngle°")
        
        var closestIndex = -1
        var minDiff = Float.MAX_VALUE
        
        for (i in digitAngles.indices) {
            var diff = abs(touchAngle - digitAngles[i])
            if (diff > 180f) diff = 360f - diff
            
            android.util.Log.d("RotaryDial", "  Digit ${digits[i]} at ${digitAngles[i]}°, diff = $diff°")
            
            if (diff < minDiff && diff < 20f) {
                minDiff = diff
                closestIndex = i
            }
        }
        
        if (closestIndex != -1) {
            android.util.Log.d("RotaryDial", "✅ Detected: ${digits[closestIndex]} (diff = $minDiff°)")
        }
        
        return closestIndex
    }
    
    private fun animateToRest(digit: Int) {
        var lastTickAngle = currentRotation
        
        val animator = ValueAnimator.ofFloat(currentRotation, 0f)
        animator.duration = 500
        animator.interpolator = DecelerateInterpolator(2f)
        
        animator.addUpdateListener { animation ->
            val newRotation = animation.animatedValue as Float
            
            // Play return tick
            if (abs(newRotation - lastTickAngle) >= returnTickInterval) {
                soundManager?.playReturnTick()
                lastTickAngle = newRotation
            }
            
            currentRotation = newRotation
            invalidate()
        }
        
        animator.start()
        
        if (digit != -1) {
            postDelayed({
                android.util.Log.d("RotaryDial", "Calling onDigitSelected($digit)")
                onDigitSelected?.invoke(digit)
            }, 100)
        }
    }
    
    private fun vibrateShort() {
        vibrator?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(20)
            }
        }
    }
    
    private fun vibrateMedium() {
        vibrator?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(50)
            }
        }
    }
    
    fun cleanup() {
        soundManager?.release()
        soundManager = null
    }
}
