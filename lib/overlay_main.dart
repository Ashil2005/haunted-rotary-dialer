import 'package:flutter/material.dart';
import 'widgets/rotary_overlay_widget.dart';

/// Entry point for the overlay Flutter engine
/// This runs in a separate isolate from the main app
@pragma('vm:entry-point')
void overlayMain() {
  runApp(const RotaryOverlayApp());
}

class RotaryOverlayApp extends StatelessWidget {
  const RotaryOverlayApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      debugShowCheckedModeBanner: false,
      home: RotaryOverlayWidget(),
    );
  }
}
