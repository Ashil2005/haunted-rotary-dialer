// Widget tests for Rotary Toggle app

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:rotary_dialer/main.dart';

void main() {
  testWidgets('Rotary Toggle app smoke test', (WidgetTester tester) async {
    // Build our app and trigger a frame
    await tester.pumpWidget(const RotaryToggleApp());

    // Verify that the app title is present
    expect(find.text('Rotary Dial Overlay'), findsOneWidget);

    // Verify that status text is present
    expect(find.text('Disabled'), findsOneWidget);
  });

  testWidgets('Toggle switch is present', (WidgetTester tester) async {
    await tester.pumpWidget(const RotaryToggleApp());

    // Verify that a Switch widget is present
    expect(find.byType(Switch), findsOneWidget);
  });

  testWidgets('Features list is displayed', (WidgetTester tester) async {
    await tester.pumpWidget(const RotaryToggleApp());

    // Find and verify features section
    expect(find.text('Features'), findsOneWidget);
    expect(find.text('Nostalgic Experience'), findsOneWidget);
  });

  testWidgets('App uses Material 3 theme', (WidgetTester tester) async {
    await tester.pumpWidget(const RotaryToggleApp());

    // Verify Material 3 is used
    final MaterialApp app = tester.widget(find.byType(MaterialApp));
    expect(app.theme?.useMaterial3, true);
  });
}
