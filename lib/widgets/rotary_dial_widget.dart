import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:audioplayers/audioplayers.dart';
import 'dart:math' as math;
import 'dart:io';

// ============================================================================
// PHYSICS TUNING PARAMETERS
// ============================================================================
const double NOTCH_ANGLE = math.pi / 5;
const double MIN_DIGIT_THRESHOLD = 0.35;
const double MAX_ROTATION = 2.0;
const int SPRING_DURATION_MS = 800;
const Curve SPRING_CURVE = Curves.elasticOut;

/// A rotary dial widget with realistic metal materials and motion
class RotaryDialWidget extends StatefulWidget {
  final Function(int) onNumberDialed;

  const RotaryDialWidget({super.key, required this.onNumberDialed});

  @override
  State<RotaryDialWidget> createState() => _RotaryDialWidgetState();
}

class _RotaryDialWidgetState extends State<RotaryDialWidget>
    with SingleTickerProviderStateMixin {
  late AnimationController _animationController;
  late Animation<double> _rotationAnimation;
  final AudioPlayer _audioPlayer = AudioPlayer();

  // Rotation state
  double _currentRotation = 0.0;
  Offset _center = Offset.zero;
  double _dialRadius = 0.0;
  
  // Hole-first detection state
  int? activeHoleIndex;
  double dragStartAngle = 0.0;
  double maxDragAngle = 0.0;
  bool _isDragging = false;
  
  // Notch detection state
  int _lastNotchIndex = -1;
  double _dragStartRotation = 0.0;
  List<int> _flashingNotches = [];
  
  // Tap detection
  Offset? _tapDownPosition;
  DateTime? _tapDownTime;
  
  // Finger stop animation
  double _fingerStopBounce = 0.0;
  
  // Dial configuration
  static const double fingerStopAngle = math.pi / 3;
  static const int numberOfDigits = 10;
  static const List<int> numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 0];
  static const double numberRadius = 0.68;
  static const double holeRadius = 24.0;
  
  bool _showDebug = false;
  bool _hasDialTexture = false;

  @override
  void initState() {
    super.initState();
    
    _checkAssets();
    
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: SPRING_DURATION_MS),
    );

    _animationController.addListener(() {
      setState(() {
        _currentRotation = _rotationAnimation.value;
        
        // Animate finger stop bounce during spring-back
        final progress = _animationController.value;
        if (progress > 0.7) {
          _fingerStopBounce = math.sin((progress - 0.7) * math.pi * 4) * 3.0;
        }
      });
    });

    _animationController.addStatusListener((status) {
      if (status == AnimationStatus.completed) {
        _playReleaseSound();
        setState(() {
          _fingerStopBounce = 0.0;
        });
      }
    });
  }

  void _checkAssets() async {
    // Check if dial texture exists
    try {
      await rootBundle.load('assets/images/dial_texture.png');
      setState(() {
        _hasDialTexture = true;
      });
    } catch (e) {
      _hasDialTexture = false;
    }
  }

  @override
  void dispose() {
    _animationController.dispose();
    _audioPlayer.dispose();
    super.dispose();
  }

  double _calculateAngle(Offset position) {
    final dx = position.dx - _center.dx;
    final dy = position.dy - _center.dy;
    return math.atan2(dy, dx);
  }

  Offset _getHolePosition(int holeIndex) {
    final angle = -math.pi / 2 + (holeIndex * 2 * math.pi / numberOfDigits);
    final x = _center.dx + _dialRadius * numberRadius * math.cos(angle);
    final y = _center.dy + _dialRadius * numberRadius * math.sin(angle);
    return Offset(x, y);
  }

  int? _detectTouchedHole(Offset touchPosition) {
    for (int i = 0; i < numberOfDigits; i++) {
      final holePos = _getHolePosition(i);
      final distance = (touchPosition - holePos).distance;
      if (distance <= holeRadius * 1.5) return i;
    }
    return null;
  }

  int _getDigitForHole(int holeIndex) => numbers[holeIndex];

  double _calculateRotationDelta(double currentAngle, double previousAngle) {
    double delta = currentAngle - previousAngle;
    if (delta > math.pi) delta -= 2 * math.pi;
    else if (delta < -math.pi) delta += 2 * math.pi;
    return delta;
  }

  void _checkNotchCrossing() {
    final absoluteRotation = _currentRotation - _dragStartRotation;
    final currentNotchIndex = (absoluteRotation / NOTCH_ANGLE).floor();
    
    if (currentNotchIndex > _lastNotchIndex && currentNotchIndex >= 0) {
      for (int i = _lastNotchIndex + 1; i <= currentNotchIndex; i++) {
        if (i >= 0) _emitNotchEvent(i);
      }
      _lastNotchIndex = currentNotchIndex;
    }
  }

  void _emitNotchEvent(int notchIndex) {
    HapticFeedback.selectionClick();
    _playClickSound();
    
    setState(() {
      if (!_flashingNotches.contains(notchIndex)) {
        _flashingNotches.add(notchIndex);
      }
    });
    
    Future.delayed(const Duration(milliseconds: 150), () {
      if (mounted) {
        setState(() {
          _flashingNotches.remove(notchIndex);
        });
      }
    });
  }

  void _playClickSound() async {
    try {
      await _audioPlayer.play(AssetSource('sounds/click.wav'), volume: 0.6);
    } catch (e) {}
  }

  void _playReleaseSound() async {
    try {
      await _audioPlayer.play(AssetSource('sounds/release.wav'), volume: 0.8);
    } catch (e) {}
  }

  void _onPanStart(DragStartDetails details) {
    _animationController.stop();
    
    final RenderBox box = context.findRenderObject() as RenderBox;
    _center = box.size.center(Offset.zero);
    _dialRadius = box.size.width / 2;
    
    activeHoleIndex = _detectTouchedHole(details.localPosition);
    
    if (activeHoleIndex != null) {
      _isDragging = true;
      dragStartAngle = _calculateAngle(details.localPosition);
      maxDragAngle = 0.0;
      _dragStartRotation = _currentRotation;
      _lastNotchIndex = -1;
      _flashingNotches.clear();
    } else {
      _isDragging = false;
    }
    
    _tapDownPosition = details.localPosition;
    _tapDownTime = DateTime.now();
  }

  void _onPanUpdate(DragUpdateDetails details) {
    if (!_isDragging || activeHoleIndex == null) return;

    final currentAngle = _calculateAngle(details.localPosition);
    final previousAngle = _calculateAngle(details.localPosition - details.delta);
    double deltaRotation = _calculateRotationDelta(currentAngle, previousAngle);

    setState(() {
      if (deltaRotation > 0) {
        _currentRotation += deltaRotation;
        _currentRotation = math.min(_currentRotation, MAX_ROTATION);
        
        if (_currentRotation > maxDragAngle) {
          maxDragAngle = _currentRotation;
        }
        
        _checkNotchCrossing();
      }
    });
  }

  void _onPanEnd(DragEndDetails details) {
    if (!_isDragging || activeHoleIndex == null) return;
    
    final digit = _getDigitForHole(activeHoleIndex!);
    
    bool wasTap = false;
    if (_tapDownTime != null && _tapDownPosition != null) {
      final tapDuration = DateTime.now().difference(_tapDownTime!);
      final tapMovement = maxDragAngle;
      if (tapDuration.inMilliseconds < 200 && tapMovement < 0.1) {
        wasTap = true;
      }
    }
    
    if (wasTap) {
      widget.onNumberDialed(digit);
      _isDragging = false;
      activeHoleIndex = null;
      _currentRotation = 0.0;
      maxDragAngle = 0.0;
      _flashingNotches.clear();
    } else if (maxDragAngle >= MIN_DIGIT_THRESHOLD) {
      widget.onNumberDialed(digit);
      
      _rotationAnimation = Tween<double>(
        begin: _currentRotation,
        end: 0.0,
      ).animate(CurvedAnimation(
        parent: _animationController,
        curve: SPRING_CURVE,
      ));

      _animationController.forward(from: 0.0);
    } else {
      _rotationAnimation = Tween<double>(
        begin: _currentRotation,
        end: 0.0,
      ).animate(CurvedAnimation(
        parent: _animationController,
        curve: Curves.easeOut,
      ));

      _animationController.duration = const Duration(milliseconds: 300);
      _animationController.forward(from: 0.0).then((_) {
        _animationController.duration = const Duration(milliseconds: SPRING_DURATION_MS);
      });
    }
    
    _isDragging = false;
    activeHoleIndex = null;
    _lastNotchIndex = -1;
    _tapDownPosition = null;
    _tapDownTime = null;
    
    Future.delayed(const Duration(milliseconds: 300), () {
      if (mounted) {
        setState(() {
          _flashingNotches.clear();
        });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onPanStart: _onPanStart,
      onPanUpdate: _onPanUpdate,
      onPanEnd: _onPanEnd,
      onDoubleTap: () => setState(() => _showDebug = !_showDebug),
      child: Stack(
        children: [
          // Main dial with realistic materials
          CustomPaint(
            size: const Size(280, 280),
            painter: _RealisticRotaryDialPainter(
              rotation: _currentRotation,
              isDragging: _isDragging,
              activeHoleIndex: activeHoleIndex,
              flashingNotches: _flashingNotches,
              fingerStopBounce: _fingerStopBounce,
            ),
          ),
          
          // Texture overlay if available
          if (_hasDialTexture)
            Positioned.fill(
              child: Transform.rotate(
                angle: _currentRotation,
                child: Opacity(
                  opacity: 0.15,
                  child: Image.asset(
                    'assets/images/dial_texture.png',
                    fit: BoxFit.cover,
                    color: Colors.white,
                    colorBlendMode: BlendMode.overlay,
                  ),
                ),
              ),
            ),
          
          // Debug overlay
          if (_showDebug)
            Positioned(
              top: 8,
              left: 8,
              child: Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.black.withOpacity(0.7),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('DEBUG', style: TextStyle(color: Colors.yellow, fontSize: 10, fontWeight: FontWeight.bold)),
                    Text('Hole: ${activeHoleIndex ?? "none"}', style: TextStyle(color: Colors.white, fontSize: 10)),
                    if (activeHoleIndex != null)
                      Text('Digit: ${_getDigitForHole(activeHoleIndex!)}', style: TextStyle(color: Colors.green, fontSize: 10)),
                    Text('Rotation: ${_currentRotation.toStringAsFixed(2)}', style: TextStyle(color: Colors.white, fontSize: 10)),
                    Text('Max: ${maxDragAngle.toStringAsFixed(2)}', style: TextStyle(color: Colors.orange, fontSize: 10)),
                    Text('Notch: $_lastNotchIndex', style: TextStyle(color: Colors.cyan, fontSize: 10)),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }
}

/// Realistic metal rotary dial painter with materials and depth
class _RealisticRotaryDialPainter extends CustomPainter {
  final double rotation;
  final bool isDragging;
  final int? activeHoleIndex;
  final List<int> flashingNotches;
  final double fingerStopBounce;

  _RealisticRotaryDialPainter({
    required this.rotation,
    required this.isDragging,
    this.activeHoleIndex,
    required this.flashingNotches,
    required this.fingerStopBounce,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = size.width / 2;
    final numberRadius = radius * 0.68;

    // Draw base shadow with parallax
    final shadowOffset = Offset(
      math.sin(rotation) * 6,
      math.cos(rotation) * 6 + 6,
    );
    final shadowPaint = Paint()
      ..color = Colors.black.withOpacity(0.5)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 25);
    canvas.drawCircle(center + shadowOffset, radius, shadowPaint);

    // Draw metallic rim with radial gradient and bevel
    _drawMetallicRim(canvas, center, radius);
    
    // Draw inner bevel
    _drawInnerBevel(canvas, center, radius);

    // Draw finger stop with glint
    _drawFingerStop(canvas, center, radius);

    // Draw notch flash highlights
    for (int notchIndex in flashingNotches) {
      _drawNotchFlash(canvas, center, radius, notchIndex);
    }

    // Save canvas for rotation
    canvas.save();
    canvas.translate(center.dx, center.dy);
    canvas.rotate(rotation);
    canvas.translate(-center.dx, -center.dy);

    // Draw number holes with realistic depth
    _drawNumberHoles(canvas, center, numberRadius);

    canvas.restore();

    // Draw center cap with brushed metal and phone icon
    _drawCenterCap(canvas, center);
    
    // Draw subtle grain overlay
    _drawGrainOverlay(canvas, center, radius);
  }

  void _drawMetallicRim(Canvas canvas, Offset center, double radius) {
    // Outer metallic gradient
    final rimGradient = RadialGradient(
      colors: [
        const Color(0xFF5a5a5a),
        const Color(0xFF3d3d3d),
        const Color(0xFF2a2a2a),
        const Color(0xFF1a1a1a),
      ],
      stops: const [0.0, 0.3, 0.7, 1.0],
    );
    
    final rimPaint = Paint()
      ..shader = rimGradient.createShader(Rect.fromCircle(center: center, radius: radius))
      ..style = PaintingStyle.fill;
    canvas.drawCircle(center, radius, rimPaint);

    // Metallic highlight ring
    final highlightPaint = Paint()
      ..color = const Color(0xFF8a8a8a).withOpacity(0.3)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3;
    canvas.drawCircle(center, radius - 2, highlightPaint);

    // Outer border
    final borderPaint = Paint()
      ..color = const Color(0xFF6b5335)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 4;
    canvas.drawCircle(center, radius, borderPaint);
  }

  void _drawInnerBevel(Canvas canvas, Offset center, double radius) {
    // Inner shadow for depth
    final innerShadowGradient = RadialGradient(
      colors: [
        Colors.transparent,
        Colors.black.withOpacity(0.4),
      ],
      stops: const [0.85, 1.0],
    );
    
    final innerShadowPaint = Paint()
      ..shader = innerShadowGradient.createShader(Rect.fromCircle(center: center, radius: radius * 0.92))
      ..style = PaintingStyle.fill;
    canvas.drawCircle(center, radius * 0.92, innerShadowPaint);

    // Inner highlight
    final innerHighlightPaint = Paint()
      ..color = const Color(0xFF5a5a5a).withOpacity(0.2)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2;
    canvas.drawCircle(center, radius * 0.88, innerHighlightPaint);
  }

  void _drawFingerStop(Canvas canvas, Offset center, double radius) {
    final fingerStopAngle = math.pi / 3;
    final bounceOffset = fingerStopBounce;
    final fingerStopX = center.dx + (radius * 0.88 + bounceOffset) * math.cos(fingerStopAngle);
    final fingerStopY = center.dy + (radius * 0.88 + bounceOffset) * math.sin(fingerStopAngle);
    
    // Finger stop shadow
    final stopShadowPaint = Paint()
      ..color = Colors.black.withOpacity(0.4)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 4);
    canvas.drawCircle(Offset(fingerStopX + 2, fingerStopY + 2), 12, stopShadowPaint);
    
    // Metallic finger stop with gradient
    final stopGradient = RadialGradient(
      colors: [
        const Color(0xFFf4d47f),
        const Color(0xFFd4af37),
        const Color(0xFFb4903f),
        const Color(0xFF8b7355),
      ],
      stops: const [0.0, 0.4, 0.7, 1.0],
    );
    
    final stopPaint = Paint()
      ..shader = stopGradient.createShader(
        Rect.fromCircle(center: Offset(fingerStopX, fingerStopY), radius: 12)
      )
      ..style = PaintingStyle.fill;
    canvas.drawCircle(Offset(fingerStopX, fingerStopY), 12, stopPaint);
    
    // Glint on finger stop
    final glintPaint = Paint()
      ..color = Colors.white.withOpacity(0.6)
      ..style = PaintingStyle.fill;
    canvas.drawCircle(Offset(fingerStopX - 3, fingerStopY - 3), 3, glintPaint);
    
    // Border
    final stopBorderPaint = Paint()
      ..color = const Color(0xFF6b5335)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2;
    canvas.drawCircle(Offset(fingerStopX, fingerStopY), 12, stopBorderPaint);
  }

  void _drawNotchFlash(Canvas canvas, Offset center, double radius, int notchIndex) {
    final notchAngle = notchIndex * NOTCH_ANGLE;
    final flashRadius = radius * 0.85;
    final flashX = center.dx + flashRadius * math.cos(notchAngle);
    final flashY = center.dy + flashRadius * math.sin(notchAngle);
    
    final flashPaint = Paint()
      ..color = const Color(0xFFffff00).withOpacity(0.5)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 20);
    canvas.drawCircle(Offset(flashX, flashY), 25, flashPaint);
  }

  void _drawNumberHoles(Canvas canvas, Offset center, double numberRadius) {
    const numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 0];
    
    for (int i = 0; i < numbers.length; i++) {
      final angle = -math.pi / 2 + (i * 2 * math.pi / 10);
      final x = center.dx + numberRadius * math.cos(angle);
      final y = center.dy + numberRadius * math.sin(angle);
      final isActive = activeHoleIndex == i;

      // Hole shadow (deep inset)
      final holeShadowGradient = RadialGradient(
        colors: [
          Colors.black.withOpacity(0.9),
          Colors.black.withOpacity(0.6),
          Colors.black.withOpacity(0.2),
        ],
        stops: const [0.0, 0.6, 1.0],
      );
      
      final holeShadowPaint = Paint()
        ..shader = holeShadowGradient.createShader(
          Rect.fromCircle(center: Offset(x, y), radius: 26)
        )
        ..style = PaintingStyle.fill;
      canvas.drawCircle(Offset(x, y), 26, holeShadowPaint);

      // Hole interior with slight gloss
      final holeGradient = RadialGradient(
        colors: isActive 
          ? [const Color(0xFF1a1a0a), const Color(0xFF0a0a0a)]
          : [const Color(0xFF0f0f0f), const Color(0xFF050505)],
      );
      
      final holePaint = Paint()
        ..shader = holeGradient.createShader(
          Rect.fromCircle(center: Offset(x, y), radius: 24)
        )
        ..style = PaintingStyle.fill;
      canvas.drawCircle(Offset(x, y), 24, holePaint);

      // Metallic rim around hole
      final holeRimPaint = Paint()
        ..color = isActive ? const Color(0xFFd4af37) : const Color(0xFF8a8a8a)
        ..style = PaintingStyle.stroke
        ..strokeWidth = isActive ? 3.0 : 2.0;
      canvas.drawCircle(Offset(x, y), 24, holeRimPaint);

      // Highlight on rim (top-left)
      final rimHighlightPaint = Paint()
        ..color = Colors.white.withOpacity(0.3)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.5;
      canvas.drawArc(
        Rect.fromCircle(center: Offset(x, y), radius: 24),
        -math.pi * 0.75,
        math.pi * 0.5,
        false,
        rimHighlightPaint,
      );

      // Number text with embossed effect
      final textPainter = TextPainter(
        text: TextSpan(
          text: numbers[i].toString(),
          style: TextStyle(
            color: isActive ? const Color(0xFFffff00) : const Color(0xFFc0c0c0),
            fontSize: 22,
            fontWeight: FontWeight.bold,
            shadows: [
              Shadow(
                color: Colors.black.withOpacity(0.8),
                offset: const Offset(1, 1),
                blurRadius: 2,
              ),
              Shadow(
                color: isActive ? const Color(0xFFffff00) : Colors.white.withOpacity(0.3),
                offset: const Offset(-1, -1),
                blurRadius: 1,
              ),
            ],
          ),
        ),
        textDirection: TextDirection.ltr,
      );
      textPainter.layout();
      textPainter.paint(
        canvas,
        Offset(x - textPainter.width / 2, y - textPainter.height / 2),
      );

      // Finger holes for authenticity
      for (int j = 0; j < 4; j++) {
        final holeAngle = angle + (j - 1.5) * 0.12;
        final holeRadius = numberRadius * 0.88;
        final holeX = center.dx + holeRadius * math.cos(holeAngle);
        final holeY = center.dy + holeRadius * math.sin(holeAngle);
        
        // Finger hole shadow
        final fingerHoleShadowPaint = Paint()
          ..color = Colors.black.withOpacity(0.6)
          ..style = PaintingStyle.fill;
        canvas.drawCircle(Offset(holeX, holeY), 4, fingerHoleShadowPaint);
        
        // Finger hole
        final fingerHolePaint = Paint()
          ..color = const Color(0xFF0a0a0a)
          ..style = PaintingStyle.fill;
        canvas.drawCircle(Offset(holeX, holeY), 3.5, fingerHolePaint);
      }
    }
  }

  void _drawCenterCap(Canvas canvas, Offset center) {
    // Center cap shadow
    final capShadowPaint = Paint()
      ..color = Colors.black.withOpacity(0.4)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 8);
    canvas.drawCircle(center + const Offset(2, 2), 50, capShadowPaint);

    // Brushed metal effect
    final capGradient = SweepGradient(
      colors: [
        const Color(0xFF3a3a3a),
        const Color(0xFF2a2a2a),
        const Color(0xFF3a3a3a),
        const Color(0xFF2a2a2a),
        const Color(0xFF3a3a3a),
      ],
      stops: const [0.0, 0.25, 0.5, 0.75, 1.0],
    );
    
    final capPaint = Paint()
      ..shader = capGradient.createShader(
        Rect.fromCircle(center: center, radius: 50)
      )
      ..style = PaintingStyle.fill;
    canvas.drawCircle(center, 50, capPaint);

    // Metallic border
    final capBorderPaint = Paint()
      ..color = const Color(0xFF8a8a8a)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3;
    canvas.drawCircle(center, 50, capBorderPaint);

    // Inner highlight ring
    final capHighlightPaint = Paint()
      ..color = Colors.white.withOpacity(0.2)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2;
    canvas.drawCircle(center, 46, capHighlightPaint);

    // Embossed phone icon (simple)
    final iconPaint = Paint()
      ..color = const Color(0xFF1a1a1a)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3
      ..strokeCap = StrokeCap.round;
    
    // Phone handset shape
    final path = Path();
    path.moveTo(center.dx - 12, center.dy - 8);
    path.quadraticBezierTo(center.dx - 15, center.dy - 5, center.dx - 12, center.dy - 2);
    path.lineTo(center.dx + 12, center.dy + 8);
    path.quadraticBezierTo(center.dx + 15, center.dy + 5, center.dx + 12, center.dy + 2);
    canvas.drawPath(path, iconPaint);
    
    // Icon highlight
    final iconHighlightPaint = Paint()
      ..color = Colors.white.withOpacity(0.1)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2
      ..strokeCap = StrokeCap.round;
    canvas.drawPath(path, iconHighlightPaint);
  }

  void _drawGrainOverlay(Canvas canvas, Offset center, double radius) {
    // Subtle noise/grain effect
    final grainPaint = Paint()
      ..color = Colors.white.withOpacity(0.02)
      ..style = PaintingStyle.fill;
    
    final random = math.Random(42); // Fixed seed for consistent grain
    for (int i = 0; i < 200; i++) {
      final angle = random.nextDouble() * 2 * math.pi;
      final distance = random.nextDouble() * radius;
      final x = center.dx + distance * math.cos(angle);
      final y = center.dy + distance * math.sin(angle);
      canvas.drawCircle(Offset(x, y), 0.5, grainPaint);
    }
  }

  @override
  bool shouldRepaint(_RealisticRotaryDialPainter oldDelegate) {
    return oldDelegate.rotation != rotation || 
           oldDelegate.isDragging != isDragging ||
           oldDelegate.activeHoleIndex != activeHoleIndex ||
           oldDelegate.flashingNotches != flashingNotches ||
           oldDelegate.fingerStopBounce != fingerStopBounce;
  }
}
