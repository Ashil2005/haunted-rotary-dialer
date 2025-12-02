import 'package:flutter/material.dart';
import 'package:rotary_dialer/services/overlay_service.dart';
import 'rotary_dial_widget.dart';

/// Wrapper widget for the rotary dial that's used in the overlay
/// Provides transparent background and digit callback
class RotaryOverlayWidget extends StatefulWidget {
  const RotaryOverlayWidget({super.key});

  @override
  State<RotaryOverlayWidget> createState() => _RotaryOverlayWidgetState();
}

class _RotaryOverlayWidgetState extends State<RotaryOverlayWidget> {
  final OverlayService _overlayService = OverlayService();
  String _dialedNumber = '';

  void _onNumberDialed(int number) {
    setState(() {
      _dialedNumber += number.toString();
    });
    
    // Send digit to system dial pad via native code
    _overlayService.sendDigit(number.toString());
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Container(
        color: Colors.black.withOpacity(0.3), // Semi-transparent background
        child: SafeArea(
          child: Column(
            children: [
              // Number display
              Expanded(
                flex: 2,
                child: Container(
                  padding: const EdgeInsets.all(20),
                  child: Center(
                    child: Text(
                      _dialedNumber.isEmpty ? 'Dial a number' : _dialedNumber,
                      style: const TextStyle(
                        fontSize: 32,
                        fontFamily: 'monospace',
                        color: Colors.white,
                        letterSpacing: 2,
                        fontWeight: FontWeight.w400,
                        shadows: [
                          Shadow(
                            color: Colors.black,
                            offset: Offset(2, 2),
                            blurRadius: 4,
                          ),
                        ],
                      ),
                      textAlign: TextAlign.center,
                    ),
                  ),
                ),
              ),

              // Rotary dial
              Expanded(
                flex: 3,
                child: Center(
                  child: RotaryDialWidget(
                    onNumberDialed: _onNumberDialed,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
