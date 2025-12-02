import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:rotary_dialer/main.dart' as app;
import 'package:rotary_dialer/utils/rotary_math.dart';
import 'dart:math' as math;

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('Rotary Dial Interaction Tests', () {
    testWidgets('Tap on hole 0 should dial digit 1', (WidgetTester tester) async {
      await app.main();
      await tester.pumpAndSettle();

      // Find the rotary dial widget
      final dialFinder = find.byType(GestureDetector).first;
      expect(dialFinder, findsOneWidget);

      // Get dial center and calculate hole 0 position (top)
      final dialBox = tester.getSize(dialFinder);
      final center = Offset(dialBox.width / 2, dialBox.height / 2);
      final numberRadius = (dialBox.width / 2) * 0.68;
      
      // Hole 0 is at top (-90 degrees)
      final hole0Angle = -math.pi / 2;
      final hole0Pos = Offset(
        center.dx + numberRadius * math.cos(hole0Angle),
        center.dy + numberRadius * math.sin(hole0Angle),
      );

      // Quick tap on hole 0
      await tester.tapAt(hole0Pos);
      await tester.pumpAndSettle();

      // Verify digit 1 was appended to display
      expect(find.text('1'), findsOneWidget, reason: 'Digit 1 should appear in display');
    });

    testWidgets('Drag on hole 4 should dial digit 5', (WidgetTester tester) async {
      await app.main();
      await tester.pumpAndSettle();

      final dialFinder = find.byType(GestureDetector).first;
      final dialBox = tester.getSize(dialFinder);
      final center = Offset(dialBox.width / 2, dialBox.height / 2);
      final numberRadius = (dialBox.width / 2) * 0.68;
      
      // Hole 4 position (4 * 36° clockwise from top)
      final hole4Angle = -math.pi / 2 + (4 * 2 * math.pi / 10);
      final hole4Pos = Offset(
        center.dx + numberRadius * math.cos(hole4Angle),
        center.dy + numberRadius * math.sin(hole4Angle),
      );

      // Drag from hole 4 clockwise (simulate rotation)
      final dragEndAngle = hole4Angle + 0.5; // Drag 0.5 radians (~29°)
      final dragEndPos = Offset(
        center.dx + numberRadius * math.cos(dragEndAngle),
        center.dy + numberRadius * math.sin(dragEndAngle),
      );

      await tester.dragFrom(hole4Pos, dragEndPos - hole4Pos);
      await tester.pumpAndSettle(const Duration(seconds: 2)); // Wait for spring-back

      // Verify digit 5 was appended
      expect(find.text('5'), findsOneWidget, reason: 'Digit 5 should appear in display');
    });

    testWidgets('Drag on hole 9 should dial digit 0', (WidgetTester tester) async {
      await app.main();
      await tester.pumpAndSettle();

      final dialFinder = find.byType(GestureDetector).first;
      final dialBox = tester.getSize(dialFinder);
      final center = Offset(dialBox.width / 2, dialBox.height / 2);
      final numberRadius = (dialBox.width / 2) * 0.68;
      
      // Hole 9 position (last hole, bottom-left)
      final hole9Angle = -math.pi / 2 + (9 * 2 * math.pi / 10);
      final hole9Pos = Offset(
        center.dx + numberRadius * math.cos(hole9Angle),
        center.dy + numberRadius * math.sin(hole9Angle),
      );

      // Drag from hole 9
      final dragEndAngle = hole9Angle + 0.5;
      final dragEndPos = Offset(
        center.dx + numberRadius * math.cos(dragEndAngle),
        center.dy + numberRadius * math.sin(dragEndAngle),
      );

      await tester.dragFrom(hole9Pos, dragEndPos - hole9Pos);
      await tester.pumpAndSettle(const Duration(seconds: 2));

      // Verify digit 0 was appended
      expect(find.text('0'), findsOneWidget, reason: 'Digit 0 should appear in display');
    });

    testWidgets('Short drag below threshold should not dial', (WidgetTester tester) async {
      await app.main();
      await tester.pumpAndSettle();

      final dialFinder = find.byType(GestureDetector).first;
      final dialBox = tester.getSize(dialFinder);
      final center = Offset(dialBox.width / 2, dialBox.height / 2);
      final numberRadius = (dialBox.width / 2) * 0.68;
      
      // Hole 1 position
      final hole1Angle = -math.pi / 2 + (1 * 2 * math.pi / 10);
      final hole1Pos = Offset(
        center.dx + numberRadius * math.cos(hole1Angle),
        center.dy + numberRadius * math.sin(hole1Angle),
      );

      // Very short drag (below 0.35 rad threshold)
      final dragEndAngle = hole1Angle + 0.1; // Only 0.1 radians (~6°)
      final dragEndPos = Offset(
        center.dx + numberRadius * math.cos(dragEndAngle),
        center.dy + numberRadius * math.sin(dragEndAngle),
      );

      await tester.dragFrom(hole1Pos, dragEndPos - hole1Pos);
      await tester.pumpAndSettle(const Duration(milliseconds: 500));

      // Display should still show "---" (no digit appended)
      expect(find.text('---'), findsOneWidget, reason: 'Short drag should not register');
      expect(find.text('2'), findsNothing, reason: 'Digit 2 should not appear');
    });

    testWidgets('Multiple taps should dial multiple digits', (WidgetTester tester) async {
      await app.main();
      await tester.pumpAndSettle();

      final dialFinder = find.byType(GestureDetector).first;
      final dialBox = tester.getSize(dialFinder);
      final center = Offset(dialBox.width / 2, dialBox.height / 2);
      final numberRadius = (dialBox.width / 2) * 0.68;
      
      // Tap hole 0 (digit 1)
      final hole0Angle = -math.pi / 2;
      final hole0Pos = Offset(
        center.dx + numberRadius * math.cos(hole0Angle),
        center.dy + numberRadius * math.sin(hole0Angle),
      );
      await tester.tapAt(hole0Pos);
      await tester.pumpAndSettle();

      // Tap hole 1 (digit 2)
      final hole1Angle = -math.pi / 2 + (1 * 2 * math.pi / 10);
      final hole1Pos = Offset(
        center.dx + numberRadius * math.cos(hole1Angle),
        center.dy + numberRadius * math.sin(hole1Angle),
      );
      await tester.tapAt(hole1Pos);
      await tester.pumpAndSettle();

      // Tap hole 2 (digit 3)
      final hole2Angle = -math.pi / 2 + (2 * 2 * math.pi / 10);
      final hole2Pos = Offset(
        center.dx + numberRadius * math.cos(hole2Angle),
        center.dy + numberRadius * math.sin(hole2Angle),
      );
      await tester.tapAt(hole2Pos);
      await tester.pumpAndSettle();

      // Verify "123" appears in display
      expect(find.text('123'), findsOneWidget, reason: 'Should dial 1-2-3');
    });

    testWidgets('Backspace button should remove last digit', (WidgetTester tester) async {
      await app.main();
      await tester.pumpAndSettle();

      // Dial some digits first
      final dialFinder = find.byType(GestureDetector).first;
      final dialBox = tester.getSize(dialFinder);
      final center = Offset(dialBox.width / 2, dialBox.height / 2);
      final numberRadius = (dialBox.width / 2) * 0.68;
      
      // Tap hole 0 (digit 1)
      final hole0Pos = Offset(
        center.dx + numberRadius * math.cos(-math.pi / 2),
        center.dy + numberRadius * math.sin(-math.pi / 2),
      );
      await tester.tapAt(hole0Pos);
      await tester.pumpAndSettle();

      // Tap hole 1 (digit 2)
      final hole1Pos = Offset(
        center.dx + numberRadius * math.cos(-math.pi / 2 + (1 * 2 * math.pi / 10)),
        center.dy + numberRadius * math.sin(-math.pi / 2 + (1 * 2 * math.pi / 10)),
      );
      await tester.tapAt(hole1Pos);
      await tester.pumpAndSettle();

      // Verify "12" is displayed
      expect(find.text('12'), findsOneWidget);

      // Tap backspace button
      final backspaceButton = find.text('Backspace');
      expect(backspaceButton, findsOneWidget);
      await tester.tap(backspaceButton);
      await tester.pumpAndSettle();

      // Verify only "1" remains
      expect(find.text('1'), findsOneWidget);
      expect(find.text('12'), findsNothing);
    });

    testWidgets('Call button should be present', (WidgetTester tester) async {
      await app.main();
      await tester.pumpAndSettle();

      // Verify Call button exists
      final callButton = find.text('Call');
      expect(callButton, findsOneWidget, reason: 'Call button should be present');
    });

    testWidgets('Touch outside holes should be ignored', (WidgetTester tester) async {
      await app.main();
      await tester.pumpAndSettle();

      final dialFinder = find.byType(GestureDetector).first;
      final dialBox = tester.getSize(dialFinder);
      final center = Offset(dialBox.width / 2, dialBox.height / 2);
      
      // Tap center (not on any hole)
      await tester.tapAt(center);
      await tester.pumpAndSettle();

      // Display should still show "---"
      expect(find.text('---'), findsOneWidget, reason: 'Center tap should be ignored');
    });
  });

  group('Rotary Math Integration', () {
    test('RotaryMath functions are accessible', () {
      // Verify utility functions work
      expect(RotaryMath.getDigitForHole(0), 1);
      expect(RotaryMath.getDigitForHole(9), 0);
      expect(RotaryMath.meetsThreshold(0.35), true);
      expect(RotaryMath.meetsThreshold(0.20), false);
    });
  });
}
