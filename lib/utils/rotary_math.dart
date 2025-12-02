import 'dart:math' as math;

/// Pure functions for rotary dial mathematics
/// These functions are stateless and easily testable

class RotaryMath {
  // Constants from widget
  static const double notchAngle = math.pi / 5; // 36° per notch
  static const double minDigitThreshold = 0.35; // 20° minimum
  static const double maxRotation = 2.0; // 115° maximum
  static const int numberOfDigits = 10;
  static const List<int> numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 0];

  /// Get the digit for a hole index (0-9)
  /// 
  /// Examples:
  /// - holeIndex 0 → digit 1
  /// - holeIndex 4 → digit 5
  /// - holeIndex 9 → digit 0
  static int getDigitForHole(int holeIndex) {
    if (holeIndex < 0 || holeIndex >= numberOfDigits) {
      throw ArgumentError('Hole index must be 0-9, got $holeIndex');
    }
    return numbers[holeIndex];
  }

  /// Check if drag angle meets minimum threshold
  /// 
  /// Returns true if the drag is far enough to register a digit
  static bool meetsThreshold(double dragAngle) {
    return dragAngle >= minDigitThreshold;
  }

  /// Calculate which notch index we're at based on rotation
  /// 
  /// Returns the notch index (0-based) or -1 if before first notch
  static int calculateNotchIndex(double absoluteRotation) {
    if (absoluteRotation < 0) return -1;
    return (absoluteRotation / notchAngle).floor();
  }

  /// Calculate how many notches were passed during a drag
  /// 
  /// Used for haptic/audio feedback
  static int calculateNotchesPassed(double startRotation, double endRotation) {
    final startNotch = calculateNotchIndex(startRotation);
    final endNotch = calculateNotchIndex(endRotation);
    return math.max(0, endNotch - startNotch);
  }

  /// Determine if a drag should register a digit
  /// 
  /// Returns the digit to append, or null if drag should be cancelled
  static int? shouldRegisterDigit({
    required int? activeHoleIndex,
    required double maxDragAngle,
    required bool wasTap,
  }) {
    // No hole selected
    if (activeHoleIndex == null) return null;

    // Quick tap always registers
    if (wasTap) return getDigitForHole(activeHoleIndex);

    // Check if drag meets threshold
    if (!meetsThreshold(maxDragAngle)) return null;

    // Valid drag - return digit
    return getDigitForHole(activeHoleIndex);
  }

  /// Clamp rotation to valid range
  static double clampRotation(double rotation) {
    return math.max(0.0, math.min(rotation, maxRotation));
  }

  /// Calculate rotation delta handling angle wraparound
  static double calculateRotationDelta(double currentAngle, double previousAngle) {
    double delta = currentAngle - previousAngle;
    
    // Handle angle wrapping (-π to π boundary)
    if (delta > math.pi) {
      delta -= 2 * math.pi;
    } else if (delta < -math.pi) {
      delta += 2 * math.pi;
    }
    
    return delta;
  }

  /// Normalize angle to 0-2π range
  static double normalizeAngle(double angle) {
    double normalized = angle % (2 * math.pi);
    if (normalized < 0) normalized += 2 * math.pi;
    return normalized;
  }

  /// Check if a tap is quick enough (duration and movement)
  static bool isTap({
    required Duration tapDuration,
    required double tapMovement,
    int maxDurationMs = 200,
    double maxMovement = 0.1,
  }) {
    return tapDuration.inMilliseconds < maxDurationMs && tapMovement < maxMovement;
  }

  /// Get hole position angle (for testing/debugging)
  /// 
  /// Returns angle in radians for a given hole index
  static double getHoleAngle(int holeIndex) {
    if (holeIndex < 0 || holeIndex >= numberOfDigits) {
      throw ArgumentError('Hole index must be 0-9, got $holeIndex');
    }
    // Start at top (-90 degrees) and go clockwise
    return -math.pi / 2 + (holeIndex * 2 * math.pi / numberOfDigits);
  }

  /// Convert radians to degrees (for debugging)
  static double radiansToDegrees(double radians) {
    return radians * 180 / math.pi;
  }

  /// Convert degrees to radians (for testing)
  static double degreesToRadians(double degrees) {
    return degrees * math.pi / 180;
  }
}
