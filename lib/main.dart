import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'services/overlay_service.dart';
import 'screens/settings_screen.dart';
import 'widgets/rotary_dial_widget.dart';

void main() {
  runApp(const RotaryToggleApp());
}

/// Entry point for overlay - shows rotary dial directly
@pragma('vm:entry-point')
void overlayMain() {
  runApp(const RotaryOverlayDirectApp());
}

class RotaryToggleApp extends StatelessWidget {
  const RotaryToggleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Rotary Dial Overlay',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFFd4af37),
          brightness: Brightness.light,
        ),
      ),
      home: const ToggleScreen(),
    );
  }
}

/// Direct rotary dial app for overlay
class RotaryOverlayDirectApp extends StatelessWidget {
  const RotaryOverlayDirectApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark(),
      home: const RotaryDialScreen(),
    );
  }
}

/// Screen that shows the rotary dial overlay
class RotaryDialScreen extends StatefulWidget {
  const RotaryDialScreen({super.key});

  @override
  State<RotaryDialScreen> createState() => _RotaryDialScreenState();
}

class _RotaryDialScreenState extends State<RotaryDialScreen> {
  final OverlayService _overlayService = OverlayService();

  void _onNumberDialed(int number) {
    // Send digit to system dial pad by clicking the actual button
    _overlayService.sendDigit(number.toString());
  }

  void _closeOverlay() {
    // Close the overlay by exiting the Flutter engine
    SystemNavigator.pop();
  }

  @override
  Widget build(BuildContext context) {
    final screenHeight = MediaQuery.of(context).size.height;
    final screenWidth = MediaQuery.of(context).size.width;

    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark(),
      home: Scaffold(
        backgroundColor: Colors.transparent,
        body: Stack(
          children: [
            // Rotary dial positioned at bottom center (over dial pad area)
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              height: screenHeight * 0.6, // Cover bottom 60% where dial pad usually is
              child: Container(
                color: Colors.white.withOpacity(0.98),
                child: Center(
                  child: SizedBox(
                    width: screenWidth * 0.85,
                    height: screenWidth * 0.85,
                    child: RotaryDialWidget(
                      onNumberDialed: _onNumberDialed,
                    ),
                  ),
                ),
              ),
            ),

            // Close button at top right
            Positioned(
              top: 40,
              right: 16,
              child: Material(
                color: Colors.transparent,
                child: IconButton(
                  icon: Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.black.withOpacity(0.6),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.close,
                      color: Colors.white,
                      size: 24,
                    ),
                  ),
                  onPressed: _closeOverlay,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class ToggleScreen extends StatefulWidget {
  const ToggleScreen({super.key});

  @override
  State<ToggleScreen> createState() => _ToggleScreenState();
}

class _ToggleScreenState extends State<ToggleScreen> {
  bool _isEnabled = false;
  bool _isLoading = true;
  bool _showExplanation = true;
  final OverlayService _overlayService = OverlayService();

  @override
  void initState() {
    super.initState();
    _loadState();
  }

  Future<void> _loadState() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _isEnabled = prefs.getBool('overlay_enabled') ?? false;
      _showExplanation = prefs.getBool('show_explanation') ?? true;
      _isLoading = false;
    });
  }

  Future<void> _saveState(bool enabled) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('overlay_enabled', enabled);
  }

  Future<void> _dismissExplanation() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('show_explanation', false);
    setState(() {
      _showExplanation = false;
    });
  }

  Future<void> _toggleOverlay(bool value) async {
    setState(() {
      _isLoading = true;
    });

    try {
      if (value) {
        // Check permissions first
        final hasPermissions = await _overlayService.checkPermissions();
        if (!hasPermissions) {
          // Request permissions
          try {
            await _overlayService.requestPermissions();
            // Give user time to grant permissions
            await Future.delayed(const Duration(milliseconds: 500));
            // After requesting, check again
            final permissionsGranted = await _overlayService.checkPermissions();
            if (!permissionsGranted) {
              _showMessage(
                  'Please grant overlay and accessibility permissions in settings');
              setState(() {
                _isEnabled = false;
                _isLoading = false;
              });
              return;
            }
          } on PlatformException catch (e) {
            _showMessage('Permission error: ${e.message ?? e.code}');
            setState(() {
              _isEnabled = false;
              _isLoading = false;
            });
            return;
          }
        }

        // Enable overlay
        try {
          final success = await _overlayService.enableOverlay();
          if (success) {
            setState(() {
              _isEnabled = true;
            });
            await _saveState(true);
            _showMessage('Rotary overlay enabled');
          } else {
            _showMessage('Failed to enable overlay. Check permissions.');
            setState(() {
              _isEnabled = false;
            });
          }
        } on PlatformException catch (e) {
          String errorMsg = 'Failed to enable overlay';
          if (e.code == 'SERVICE_ERROR') {
            errorMsg = 'Service error: ${e.message ?? 'Unknown error'}';
          } else if (e.code == 'PERMISSION_ERROR') {
            errorMsg = 'Permission denied. Please check settings.';
          }
          _showMessage(errorMsg);
          setState(() {
            _isEnabled = false;
          });
        }
      } else {
        // Disable overlay
        try {
          final success = await _overlayService.disableOverlay();
          if (success) {
            setState(() {
              _isEnabled = false;
            });
            await _saveState(false);
            _showMessage('Rotary overlay disabled');
          } else {
            _showMessage('Failed to disable overlay');
          }
        } on PlatformException catch (e) {
          _showMessage('Error disabling: ${e.message ?? e.code}');
          // Still mark as disabled since we tried
          setState(() {
            _isEnabled = false;
          });
          await _saveState(false);
        }
      }
    } on PlatformException catch (e) {
      _showMessage('Platform error: ${e.message ?? e.code}');
      setState(() {
        _isEnabled = false;
      });
    } catch (e) {
      _showMessage('Unexpected error: ${e.toString()}');
      setState(() {
        _isEnabled = false;
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        behavior: SnackBarBehavior.floating,
        duration: const Duration(seconds: 2),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Rotary Dial Overlay'),
        centerTitle: true,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => const SettingsScreen(),
                ),
              );
            },
            tooltip: 'Settings',
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(24.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Main toggle card
                  Card(
                    elevation: 4,
                    child: Padding(
                      padding: const EdgeInsets.all(24.0),
                      child: Column(
                        children: [
                          // Icon
                          Container(
                            width: 80,
                            height: 80,
                            decoration: BoxDecoration(
                              color: _isEnabled
                                  ? colorScheme.primaryContainer
                                  : colorScheme.surfaceVariant,
                              shape: BoxShape.circle,
                            ),
                            child: Icon(
                              Icons.phone_in_talk_rounded,
                              size: 40,
                              color: _isEnabled
                                  ? colorScheme.onPrimaryContainer
                                  : colorScheme.onSurfaceVariant,
                            ),
                          ),
                          const SizedBox(height: 24),

                          // Status text
                          Text(
                            _isEnabled ? 'Enabled' : 'Disabled',
                            style: theme.textTheme.headlineMedium?.copyWith(
                              fontWeight: FontWeight.bold,
                              color: _isEnabled
                                  ? colorScheme.primary
                                  : colorScheme.onSurfaceVariant,
                            ),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            _isEnabled
                                ? 'Rotary dial will appear on dial pads'
                                : 'Rotary dial is currently off',
                            style: theme.textTheme.bodyMedium?.copyWith(
                              color: colorScheme.onSurfaceVariant,
                            ),
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 24),

                          // Toggle switch
                          Transform.scale(
                            scale: 1.2,
                            child: Switch(
                              value: _isEnabled,
                              onChanged: (value) {
                                HapticFeedback.mediumImpact();
                                _toggleOverlay(value);
                              },
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),

                  // First-time explanation card
                  if (_showExplanation) ...[
                    const SizedBox(height: 16),
                    Card(
                      color: colorScheme.secondaryContainer,
                      child: Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Icon(
                                  Icons.info_outline,
                                  color: colorScheme.onSecondaryContainer,
                                ),
                                const SizedBox(width: 8),
                                Expanded(
                                  child: Text(
                                    'How it works',
                                    style: theme.textTheme.titleMedium
                                        ?.copyWith(
                                      fontWeight: FontWeight.bold,
                                      color: colorScheme.onSecondaryContainer,
                                    ),
                                  ),
                                ),
                                IconButton(
                                  icon: const Icon(Icons.close, size: 20),
                                  onPressed: _dismissExplanation,
                                  color: colorScheme.onSecondaryContainer,
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),
                            Text(
                              'When enabled, a nostalgic rotary dial will appear over your phone\'s dial pad. '
                              'Rotate the dial to enter numbers just like old rotary phones!\n\n'
                              'You\'ll need to grant permissions for:\n'
                              '• Drawing over other apps\n'
                              '• Accessibility service (to detect dial pads)',
                              style: theme.textTheme.bodyMedium?.copyWith(
                                color: colorScheme.onSecondaryContainer,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],

                  // Features list
                  const SizedBox(height: 24),
                  Text(
                    'Features',
                    style: theme.textTheme.titleLarge?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 12),
                  _FeatureItem(
                    icon: Icons.phone_callback,
                    title: 'Nostalgic Experience',
                    description:
                        'Dial numbers with a realistic rotary phone interface',
                  ),
                  _FeatureItem(
                    icon: Icons.layers,
                    title: 'Works Everywhere',
                    description:
                        'Appears automatically on any dial pad in your phone',
                  ),
                  _FeatureItem(
                    icon: Icons.toggle_on,
                    title: 'Easy Toggle',
                    description:
                        'Turn on/off anytime to switch between rotary and normal',
                  ),
                  _FeatureItem(
                    icon: Icons.battery_charging_full,
                    title: 'Lightweight',
                    description:
                        'Minimal battery and memory usage in background',
                  ),
                ],
              ),
            ),
    );
  }
}

class _FeatureItem extends StatelessWidget {
  final IconData icon;
  final String title;
  final String description;

  const _FeatureItem({
    required this.icon,
    required this.title,
    required this.description,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Padding(
      padding: const EdgeInsets.only(bottom: 16.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: colorScheme.primaryContainer,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(
              icon,
              color: colorScheme.onPrimaryContainer,
              size: 24,
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  description,
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
