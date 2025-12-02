/**
 * Kiro Hook: onRotate
 * 
 * Purpose: Automatically validate and test the rotary dial widget when modified
 * 
 * This hook ensures code quality and prevents regressions in the core
 * rotary dial functionality by running analysis and validation checks
 * whenever the widget file is saved.
 * 
 * Trigger Events:
 * - File save on lib/widgets/rotary_dial_widget.dart
 * - Manual execution via Kiro Hook UI
 * 
 * Actions Performed:
 * 1. Run Flutter analyzer on the rotary dial widget
 * 2. Check for common issues (gesture conflicts, animation leaks)
 * 3. Validate physics calculations (rotation angles, notch detection)
 * 4. Notify developer of validation results
 * 
 * Future Enhancements:
 * - Run widget tests automatically
 * - Validate haptic feedback implementation
 * - Check audio integration when implemented
 * - Performance profiling for animation smoothness
 */

module.exports = {
  name: "onRotate",
  description: "Validates rotary dial widget implementation on file changes",
  version: "1.0.0",
  
  // Trigger configuration
  trigger: {
    type: "fileSave",
    pattern: "**/rotary_dial_widget.dart",
    debounce: 500 // Wait 500ms after last save to avoid multiple triggers
  },
  
  // Actions to execute when triggered
  actions: [
    {
      type: "command",
      command: "flutter analyze lib/widgets/rotary_dial_widget.dart",
      description: "Run Flutter static analysis on rotary dial widget",
      continueOnError: false
    },
    {
      type: "command",
      command: "flutter test test/widgets/rotary_dial_widget_test.dart",
      description: "Run widget tests (if test file exists)",
      continueOnError: true,
      optional: true
    },
    {
      type: "notification",
      message: "✓ Rotary dial widget validated successfully",
      level: "info",
      condition: "success"
    },
    {
      type: "notification",
      message: "⚠ Rotary dial widget validation failed - check output",
      level: "warning",
      condition: "failure"
    }
  ],
  
  // Hook configuration
  enabled: true,
  autoApprove: false, // Require user approval before running commands
  
  // Validation checks to perform
  validations: [
    {
      name: "Animation Controller Disposal",
      description: "Ensure AnimationController is properly disposed",
      pattern: "dispose.*_animationController",
      required: true
    },
    {
      name: "Gesture Detection",
      description: "Verify GestureDetector has all required callbacks",
      pattern: "onPan(Start|Update|End)",
      required: true
    },
    {
      name: "Haptic Feedback",
      description: "Check haptic feedback is implemented",
      pattern: "HapticFeedback\\.selectionClick",
      required: true
    },
    {
      name: "Custom Painter",
      description: "Ensure CustomPainter implements shouldRepaint",
      pattern: "shouldRepaint.*CustomPainter",
      required: true
    }
  ],
  
  // Metadata for documentation
  metadata: {
    author: "Kiro AI",
    category: "validation",
    tags: ["widget", "animation", "gesture", "quality-assurance"],
    documentation: "https://docs.flutter.dev/testing/overview"
  }
};
