import 'package:flutter/services.dart';

/// Service for managing the rotary dial overlay
/// Communicates with Android native code via Method Channel
class OverlayService {
  static const platform = MethodChannel('com.example.rotary_dialer/overlay');

  /// Enable the rotary dial overlay
  /// Returns true if successful
  /// Throws PlatformException if service fails to start
  Future<bool> enableOverlay() async {
    try {
      final result = await platform.invokeMethod('enableOverlay');
      return result == true;
    } on PlatformException catch (e) {
      print('Failed to enable overlay: ${e.code} - ${e.message}');
      rethrow;
    } catch (e) {
      print('Unexpected error enabling overlay: $e');
      return false;
    }
  }

  /// Disable the rotary dial overlay
  /// Returns true if successful
  /// Throws PlatformException if service fails to stop
  Future<bool> disableOverlay() async {
    try {
      final result = await platform.invokeMethod('disableOverlay');
      return result == true;
    } on PlatformException catch (e) {
      print('Failed to disable overlay: ${e.code} - ${e.message}');
      rethrow;
    } catch (e) {
      print('Unexpected error disabling overlay: $e');
      return false;
    }
  }

  /// Check if all required permissions are granted
  /// Returns true if overlay and accessibility permissions are granted
  Future<bool> checkPermissions() async {
    try {
      final result = await platform.invokeMethod('checkPermissions');
      return result == true;
    } on PlatformException catch (e) {
      print('Failed to check permissions: ${e.code} - ${e.message}');
      return false;
    } catch (e) {
      print('Unexpected error checking permissions: $e');
      return false;
    }
  }

  /// Request required permissions from the user
  /// Opens system settings for permission grants
  /// Throws PlatformException if settings cannot be opened
  Future<void> requestPermissions() async {
    try {
      await platform.invokeMethod('requestPermissions');
    } on PlatformException catch (e) {
      print('Failed to request permissions: ${e.code} - ${e.message}');
      rethrow;
    } catch (e) {
      print('Unexpected error requesting permissions: $e');
      rethrow;
    }
  }

  /// Send a dialed digit to the system dial pad
  /// Called when user rotates the dial and registers a number
  /// Returns true if digit was successfully injected
  Future<bool> sendDigit(String digit) async {
    try {
      final result = await platform.invokeMethod('sendDigit', {'digit': digit});
      return result == true;
    } on PlatformException catch (e) {
      print('Failed to send digit: ${e.code} - ${e.message}');
      return false;
    } catch (e) {
      print('Unexpected error sending digit: $e');
      return false;
    }
  }
}
