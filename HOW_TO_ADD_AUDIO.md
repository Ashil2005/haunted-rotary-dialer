# How to Add Audio Files

## Quick Start

1. **Get audio files** (see sources below)
2. **Name them exactly:**
   - `click.wav`
   - `release.wav`
3. **Place in:** `assets/sounds/`
4. **Rebuild:** `flutter build apk --debug`

## Audio Specifications

### click.wav
- **Duration:** 50-100ms (very short)
- **Sound:** Mechanical click, ratchet sound
- **Volume:** Medium
- **Format:** WAV or MP3
- **Sample Rate:** 44.1kHz recommended

### release.wav
- **Duration:** 300-500ms
- **Sound:** Spring release, whoosh, mechanical return
- **Volume:** Slightly louder than click
- **Format:** WAV or MP3
- **Sample Rate:** 44.1kHz recommended

## Where to Get Audio

### Option 1: Record from Real Rotary Phone (Best!)
- Use your phone's voice recorder
- Record the click sound when rotating
- Record the spring-back sound when releasing
- Trim to appropriate length using Audacity (free)

### Option 2: Free Sound Libraries

#### Freesound.org
1. Go to https://freesound.org
2. Search: "rotary phone click" or "mechanical click"
3. Search: "spring release" or "rotary phone dial"
4. Download WAV files
5. May need free account

#### Zapsplat.com
1. Go to https://www.zapsplat.com
2. Search: "rotary dial" or "phone dial"
3. Download sounds
4. Free with attribution

#### BBC Sound Effects
1. Go to https://sound-effects.bbcrewind.co.uk
2. Search: "telephone" or "dial"
3. Free for personal use

### Option 3: Create Synthetic Sounds

Use Audacity (free) to create simple sounds:

**Click Sound:**
1. Generate → Tone → 800Hz, 0.05 seconds
2. Effect → Fade Out
3. Effect → Amplify to -6dB
4. Export as WAV

**Release Sound:**
1. Generate → Tone → 400Hz, 0.3 seconds
2. Effect → Sliding Stretch (pitch down)
3. Effect → Fade Out
4. Export as WAV

## Editing Audio (Audacity)

### Install Audacity
- Download: https://www.audacityteam.org
- Free and open source

### Trim Audio
1. Open audio file
2. Select unwanted parts
3. Press Delete
4. File → Export → Export as WAV

### Adjust Volume
1. Select all (Ctrl+A)
2. Effect → Amplify
3. Set to -6dB for click, -3dB for release
4. OK

### Convert Format
1. File → Open (your audio file)
2. File → Export → Export as WAV
3. Choose 16-bit PCM
4. Save

## Testing Audio

### Test in App
1. Add audio files to `assets/sounds/`
2. Run: `flutter run`
3. Rotate dial to hear clicks
4. Release to hear spring sound

### If No Sound
- Check file names are exact: `click.wav` and `release.wav`
- Check files are in `assets/sounds/` folder
- Check `pubspec.yaml` has `assets/sounds/` listed
- Run `flutter clean` then `flutter pub get`
- Rebuild app

### Adjust Volume in Code

Edit `lib/widgets/rotary_dial_widget.dart`:

```dart
// Make clicks quieter
await _audioPlayer.play(AssetSource('sounds/click.wav'), volume: 0.3);

// Make release louder
await _audioPlayer.play(AssetSource('sounds/release.wav'), volume: 0.9);
```

## Troubleshooting

### "Asset not found" error
- Verify files are in `assets/sounds/` folder
- Check `pubspec.yaml` includes `assets/sounds/`
- Run `flutter clean` and rebuild

### No sound plays
- Check device volume is up
- Test on physical device (emulator audio can be unreliable)
- Check audio files aren't corrupted
- Try different audio format (WAV vs MP3)

### Sound is too loud/quiet
- Adjust volume parameter in code (0.0 to 1.0)
- Or edit audio file volume in Audacity

### Sound is delayed
- Use shorter audio files (< 100ms for click)
- Use WAV instead of MP3 (faster loading)
- Preload audio in initState() if needed

## Example Audio Files

If you want to test quickly, you can use these search terms to find suitable sounds:

**For click.wav:**
- "ratchet click"
- "mechanical click"
- "switch click"
- "camera shutter"

**For release.wav:**
- "spring boing"
- "spring release"
- "mechanical whoosh"
- "dial tone end"

## App Works Without Audio

The app is designed to work gracefully without audio files:
- Errors are caught and logged
- Haptic feedback still works
- Visual feedback still works
- No crashes

But audio makes it **much** more satisfying!
