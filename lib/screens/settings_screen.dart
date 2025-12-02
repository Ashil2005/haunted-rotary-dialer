import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool _soundEnabled = true;
  bool _hapticEnabled = true;
  double _dialSensitivity = 1.0;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _soundEnabled = prefs.getBool('sound_enabled') ?? true;
      _hapticEnabled = prefs.getBool('haptic_enabled') ?? true;
      _dialSensitivity = prefs.getDouble('dial_sensitivity') ?? 1.0;
      _isLoading = false;
    });
  }

  Future<void> _saveSetting(String key, dynamic value) async {
    final prefs = await SharedPreferences.getInstance();
    if (value is bool) {
      await prefs.setBool(key, value);
    } else if (value is double) {
      await prefs.setDouble(key, value);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
        elevation: 0,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16.0),
              children: [
                // Sound Effects Section
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Icon(
                              Icons.volume_up,
                              color: colorScheme.primary,
                            ),
                            const SizedBox(width: 12),
                            Text(
                              'Sound Effects',
                              style: theme.textTheme.titleLarge,
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Play authentic rotary dial sounds',
                          style: theme.textTheme.bodyMedium?.copyWith(
                            color: colorScheme.onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(height: 12),
                        SwitchListTile(
                          value: _soundEnabled,
                          onChanged: (value) {
                            setState(() {
                              _soundEnabled = value;
                            });
                            _saveSetting('sound_enabled', value);
                          },
                          title: Text(
                            _soundEnabled ? 'Enabled' : 'Disabled',
                            style: theme.textTheme.bodyLarge,
                          ),
                          contentPadding: EdgeInsets.zero,
                        ),
                      ],
                    ),
                  ),
                ),

                const SizedBox(height: 16),

                // Haptic Feedback Section
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Icon(
                              Icons.vibration,
                              color: colorScheme.primary,
                            ),
                            const SizedBox(width: 12),
                            Text(
                              'Haptic Feedback',
                              style: theme.textTheme.titleLarge,
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Feel vibrations when dialing',
                          style: theme.textTheme.bodyMedium?.copyWith(
                            color: colorScheme.onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(height: 12),
                        SwitchListTile(
                          value: _hapticEnabled,
                          onChanged: (value) {
                            setState(() {
                              _hapticEnabled = value;
                            });
                            _saveSetting('haptic_enabled', value);
                          },
                          title: Text(
                            _hapticEnabled ? 'Enabled' : 'Disabled',
                            style: theme.textTheme.bodyLarge,
                          ),
                          contentPadding: EdgeInsets.zero,
                        ),
                      ],
                    ),
                  ),
                ),

                const SizedBox(height: 16),

                // Dial Sensitivity Section
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Icon(
                              Icons.tune,
                              color: colorScheme.primary,
                            ),
                            const SizedBox(width: 12),
                            Text(
                              'Dial Sensitivity',
                              style: theme.textTheme.titleLarge,
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Adjust rotation speed (${_dialSensitivity.toStringAsFixed(1)}x)',
                          style: theme.textTheme.bodyMedium?.copyWith(
                            color: colorScheme.onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(height: 12),
                        Row(
                          children: [
                            Text(
                              '0.5x',
                              style: theme.textTheme.bodySmall,
                            ),
                            Expanded(
                              child: Slider(
                                value: _dialSensitivity,
                                min: 0.5,
                                max: 2.0,
                                divisions: 15,
                                label: '${_dialSensitivity.toStringAsFixed(1)}x',
                                onChanged: (value) {
                                  setState(() {
                                    _dialSensitivity = value;
                                  });
                                },
                                onChangeEnd: (value) {
                                  _saveSetting('dial_sensitivity', value);
                                },
                              ),
                            ),
                            Text(
                              '2.0x',
                              style: theme.textTheme.bodySmall,
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(
                          _dialSensitivity < 1.0
                              ? 'Slower rotation for precise control'
                              : _dialSensitivity > 1.0
                                  ? 'Faster rotation for quick dialing'
                                  : 'Normal rotation speed',
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: colorScheme.onSurfaceVariant,
                            fontStyle: FontStyle.italic,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),

                const SizedBox(height: 24),

                // Info Section
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
                            const SizedBox(width: 12),
                            Text(
                              'About Settings',
                              style: theme.textTheme.titleMedium?.copyWith(
                                color: colorScheme.onSecondaryContainer,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Settings apply to the rotary dial overlay. Changes take effect immediately.',
                          style: theme.textTheme.bodyMedium?.copyWith(
                            color: colorScheme.onSecondaryContainer,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
    );
  }
}
