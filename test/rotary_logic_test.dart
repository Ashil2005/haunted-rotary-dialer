import 'package:flutter_test/flutter_test.dart';
import 'package:rotary_dialer/utils/rotary_math.dart';
import 'dart:math' as math;

void main() {
  group('RotaryMath - Digit Mapping', () {
    test('getDigitForHole maps hole indices correctly', () {
      expect(RotaryMath.getDigitForHole(0), 1); // Hole 0 → Digit 1
      expect(RotaryMath.getDigitForHole(1), 2); // Hole 1 → Digit 2
      expect(RotaryMath.getDigitForHole(4), 5); // Hole 4 → Digit 5
      expect(RotaryMath.getDigitForHole(8), 9); // Hole 8 → Digit 9
      expect(RotaryMath.getDigitForHole(9), 0); // Hole 9 → Digit 0
    });

    test('getDigitForHole throws on invalid index', () {
      expect(() => RotaryMath.getDigitForHole(-1), throwsArgumentError);
      expect(() => RotaryMath.getDigitForHole(10), throwsArgumentError);
      expect(() => RotaryMath.getDigitForHole(100), throwsArgumentError);
    });
  });

  group('RotaryMath - Threshold Validation', () {
    test('Case 1: Below threshold - should NOT register', () {
      const dragAngle = 0.20; // Below 0.35 threshold
      const activeHoleIndex = 0;
      
      expect(RotaryMath.meetsThreshold(dragAngle), false);
      
      final digit = RotaryMath.shouldRegisterDigit(
        activeHoleIndex: activeHoleIndex,
        maxDragAngle: dragAngle,
        wasTap: false,
      );
      
      expect(digit, null, reason: 'Drag below threshold should not register');
    });

    test('Case 2: Minimal valid threshold - should register', () {
      const dragAngle = 0.35; // Exactly at threshold
      const activeHoleIndex = 0;
      
      expect(RotaryMath.meetsThreshold(dragAngle), true);
      
      final digit = RotaryMath.shouldRegisterDigit(
        activeHoleIndex: activeHoleIndex,
        maxDragAngle: dragAngle,
        wasTap: false,
      );
      
      expect(digit, 1, reason: 'Drag at threshold should register digit 1');
    });

    test('Case 3: Maximum rotation - should register', () {
      const dragAngle = 2.0; // Max rotation
      const activeHoleIndex = 9;
      
      expect(RotaryMath.meetsThreshold(dragAngle), true);
      
      final digit = RotaryMath.shouldRegisterDigit(
        activeHoleIndex: activeHoleIndex,
        maxDragAngle: dragAngle,
        wasTap: false,
      );
      
      expect(digit, 0, reason: 'Max rotation on hole 9 should register digit 0');
    });

    test('Case 4: Edge notch (hole 0) - should register digit 1', () {
      const dragAngle = 0.50;
      const activeHoleIndex = 0; // First hole
      
      final digit = RotaryMath.shouldRegisterDigit(
        activeHoleIndex: activeHoleIndex,
        maxDragAngle: dragAngle,
        wasTap: false,
      );
      
      expect(digit, 1, reason: 'Hole 0 should always map to digit 1');
    });

    test('Case 5: Edge notch (hole 9) - should register digit 0', () {
      const dragAngle = 0.50;
      const activeHoleIndex = 9; // Last hole
      
      final digit = RotaryMath.shouldRegisterDigit(
        activeHoleIndex: activeHoleIndex,
        maxDragAngle: dragAngle,
        wasTap: false,
      );
      
      expect(digit, 0, reason: 'Hole 9 should always map to digit 0');
    });

    test('Case 6: Fast drag (large angle) - should register correctly', () {
      const dragAngle = 1.8; // Fast drag, near max
      const activeHoleIndex = 4; // Middle hole
      
      expect(RotaryMath.meetsThreshold(dragAngle), true);
      
      final digit = RotaryMath.shouldRegisterDigit(
        activeHoleIndex: activeHoleIndex,
        maxDragAngle: dragAngle,
        wasTap: false,
      );
      
      expect(digit, 5, reason: 'Fast drag on hole 4 should register digit 5');
    });
  });

  group('RotaryMath - Notch Calculation', () {
    test('calculateNotchIndex returns correct notch', () {
      const notchAngle = math.pi / 5; // 36°
      
      expect(RotaryMath.calculateNotchIndex(0.0), 0);
      expect(RotaryMath.calculateNotchIndex(notchAngle * 0.5), 0);
      expect(RotaryMath.calculateNotchIndex(notchAngle * 1.0), 1);
      expect(RotaryMath.calculateNotchIndex(notchAngle * 1.5), 1);
      expect(RotaryMath.calculateNotchIndex(notchAngle * 2.0), 2);
      expect(RotaryMath.calculateNotchIndex(notchAngle * 5.0), 5);
    });

    test('calculateNotchIndex handles negative rotation', () {
      expect(RotaryMath.calculateNotchIndex(-0.1), -1);
      expect(RotaryMath.calculateNotchIndex(-1.0), -1);
    });

    test('calculateNotchesPassed counts correctly', () {
      const notchAngle = math.pi / 5;
      
      // No notches passed
      expect(RotaryMath.calculateNotchesPassed(0.0, 0.1), 0);
      
      // One notch passed
      expect(RotaryMath.calculateNotchesPassed(0.0, notchAngle * 1.5), 1);
      
      // Multiple notches passed (fast drag)
      expect(RotaryMath.calculateNotchesPassed(0.0, notchAngle * 3.5), 3);
      
      // Backwards (should return 0)
      expect(RotaryMath.calculateNotchesPassed(notchAngle * 2, notchAngle * 1), 0);
    });
  });

  group('RotaryMath - Tap Detection', () {
    test('isTap detects quick taps', () {
      expect(
        RotaryMath.isTap(
          tapDuration: const Duration(milliseconds: 100),
          tapMovement: 0.05,
        ),
        true,
        reason: 'Quick tap with minimal movement',
      );
    });

    test('isTap rejects slow taps', () {
      expect(
        RotaryMath.isTap(
          tapDuration: const Duration(milliseconds: 300),
          tapMovement: 0.05,
        ),
        false,
        reason: 'Too slow to be a tap',
      );
    });

    test('isTap rejects large movements', () {
      expect(
        RotaryMath.isTap(
          tapDuration: const Duration(milliseconds: 100),
          tapMovement: 0.5,
        ),
        false,
        reason: 'Too much movement to be a tap',
      );
    });

    test('shouldRegisterDigit handles taps correctly', () {
      final digit = RotaryMath.shouldRegisterDigit(
        activeHoleIndex: 3,
        maxDragAngle: 0.05, // Below threshold
        wasTap: true, // But it's a tap
      );
      
      expect(digit, 4, reason: 'Taps should register even below threshold');
    });
  });

  group('RotaryMath - Angle Utilities', () {
    test('clampRotation clamps to valid range', () {
      expect(RotaryMath.clampRotation(-0.5), 0.0);
      expect(RotaryMath.clampRotation(0.5), 0.5);
      expect(RotaryMath.clampRotation(1.0), 1.0);
      expect(RotaryMath.clampRotation(2.0), 2.0);
      expect(RotaryMath.clampRotation(3.0), 2.0);
    });

    test('calculateRotationDelta handles wraparound', () {
      // Normal case
      expect(RotaryMath.calculateRotationDelta(0.5, 0.3), closeTo(0.2, 0.01));
      
      // Wraparound at π boundary
      expect(
        RotaryMath.calculateRotationDelta(-math.pi + 0.1, math.pi - 0.1),
        closeTo(0.2, 0.01),
      );
      
      // Wraparound at -π boundary
      expect(
        RotaryMath.calculateRotationDelta(math.pi - 0.1, -math.pi + 0.1),
        closeTo(-0.2, 0.01),
      );
    });

    test('normalizeAngle normalizes to 0-2π', () {
      expect(RotaryMath.normalizeAngle(0.0), 0.0);
      expect(RotaryMath.normalizeAngle(math.pi), math.pi);
      expect(RotaryMath.normalizeAngle(2 * math.pi), closeTo(0.0, 0.01));
      expect(RotaryMath.normalizeAngle(-math.pi), closeTo(math.pi, 0.01));
      expect(RotaryMath.normalizeAngle(3 * math.pi), closeTo(math.pi, 0.01));
    });

    test('radiansToDegrees converts correctly', () {
      expect(RotaryMath.radiansToDegrees(0), 0);
      expect(RotaryMath.radiansToDegrees(math.pi), 180);
      expect(RotaryMath.radiansToDegrees(2 * math.pi), 360);
      expect(RotaryMath.radiansToDegrees(math.pi / 2), 90);
    });

    test('degreesToRadians converts correctly', () {
      expect(RotaryMath.degreesToRadians(0), 0);
      expect(RotaryMath.degreesToRadians(180), closeTo(math.pi, 0.01));
      expect(RotaryMath.degreesToRadians(360), closeTo(2 * math.pi, 0.01));
      expect(RotaryMath.degreesToRadians(90), closeTo(math.pi / 2, 0.01));
    });
  });

  group('RotaryMath - Hole Angles', () {
    test('getHoleAngle returns correct angles', () {
      // Hole 0 should be at top (-90°)
      expect(
        RotaryMath.getHoleAngle(0),
        closeTo(-math.pi / 2, 0.01),
        reason: 'Hole 0 at top',
      );
      
      // Hole 2 should be at ~18° (clockwise from top)
      expect(
        RotaryMath.getHoleAngle(2),
        closeTo(-math.pi / 2 + 2 * (2 * math.pi / 10), 0.01),
      );
    });

    test('getHoleAngle throws on invalid index', () {
      expect(() => RotaryMath.getHoleAngle(-1), throwsArgumentError);
      expect(() => RotaryMath.getHoleAngle(10), throwsArgumentError);
    });
  });

  group('RotaryMath - Edge Cases', () {
    test('shouldRegisterDigit handles null activeHoleIndex', () {
      final digit = RotaryMath.shouldRegisterDigit(
        activeHoleIndex: null,
        maxDragAngle: 1.0,
        wasTap: false,
      );
      
      expect(digit, null, reason: 'No hole selected should return null');
    });

    test('shouldRegisterDigit handles zero drag', () {
      final digit = RotaryMath.shouldRegisterDigit(
        activeHoleIndex: 0,
        maxDragAngle: 0.0,
        wasTap: false,
      );
      
      expect(digit, null, reason: 'Zero drag should not register');
    });

    test('All holes map to correct digits', () {
      for (int i = 0; i < 10; i++) {
        final expectedDigit = i == 9 ? 0 : i + 1;
        expect(
          RotaryMath.getDigitForHole(i),
          expectedDigit,
          reason: 'Hole $i should map to digit $expectedDigit',
        );
      }
    });
  });
}
