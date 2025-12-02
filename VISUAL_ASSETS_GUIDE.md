# Visual Assets Guide

## Required Image Assets

Place these files in `assets/images/`:

### 1. `dial_texture.png`
**Purpose:** Subtle grain/noise overlay for realism
**Specifications:**
- Size: 512x512px or 1024x1024px
- Format: PNG with transparency
- Content: Fine grain texture, scratches, or subtle noise
- Opacity: Will be applied at 15% with overlay blend mode
- Color: Grayscale (will be tinted)

**How to create:**
- Use Photoshop/GIMP: Add Noise filter (Gaussian, 5-10%)
- Or use online texture generators
- Or photograph real metal surface

---

### 2. `metal_rim.png` (Optional)
**Purpose:** High-res metallic rim texture
**Specifications:**
- Size: 1024x1024px
- Format: PNG with transparency
- Content: Brushed metal or radial gradient
- Use: Can replace gradient in code

---

### 3. `hole_mask.png` (Optional)
**Purpose:** Mask for number holes
**Specifications:**
- Size: 128x128px per hole
- Format: PNG with alpha
- Content: Circular mask with soft edges

---

## Recommended Lottie Animations

Place in `assets/animations/`:

### 1. `rotary_gloss.json`
**Purpose:** Animated glossy highlight that moves across dial
**Search terms:**
- "metallic shine animation"
- "glossy surface reflection"
- "chrome glint effect"

**Lottie sources:**
- LottieFiles.com
- Search: "metal shine", "gloss effect", "reflection sweep"

---

### 2. `dial_rotation.json` (Optional)
**Purpose:** Particle effects during rotation
**Search terms:**
- "rotation particles"
- "circular motion trail"
- "spin effect"

---

### 3. `spring_bounce.json` (Optional)
**Purpose:** Visual effect when dial springs back
**Search terms:**
- "spring bounce"
- "elastic snap"
- "recoil effect"

---

## Suggested Lottie/Illustration Styles

### Style 1: "Vintage Rotary Phone Animation"
- Classic 1960s aesthetic
- Warm sepia tones
- Mechanical movements

### Style 2: "Metallic Chrome Reflection"
- Modern chrome finish
- Dynamic light reflections
- Glossy highlights

### Style 3: "Steampunk Mechanical Dial"
- Brass and copper tones
- Gear mechanisms
- Industrial aesthetic

---

## Asset Creation Tools

### Free Tools:
1. **GIMP** - Texture creation, noise filters
2. **Inkscape** - Vector graphics
3. **Blender** - 3D renders of metal surfaces
4. **Photopea** - Online Photoshop alternative

### Texture Resources:
1. **Textures.com** - Free metal textures
2. **Unsplash** - High-res metal photos
3. **Pexels** - Free stock photos

### Lottie Resources:
1. **LottieFiles.com** - Free animations
2. **Adobe After Effects** - Create custom Lottie
3. **Haiku Animator** - Lottie creation tool

---

## Quick Setup (No Assets)

The dial works beautifully **without any assets**! The code includes:
- ✅ Programmatic gradients
- ✅ Metallic effects via code
- ✅ Shadows and highlights
- ✅ Grain overlay (generated)

Assets are **optional enhancements** for extra realism.

---

## Asset Integration

### Add Texture Overlay:
1. Place `dial_texture.png` in `assets/images/`
2. Update `pubspec.yaml`:
   ```yaml
   assets:
     - assets/images/dial_texture.png
   ```
3. Rebuild app - texture auto-detected

### Add Lottie Animation:
1. Download `rotary_gloss.json` from LottieFiles
2. Place in `assets/animations/`
3. Add to `pubspec.yaml`:
   ```yaml
   assets:
     - assets/animations/rotary_gloss.json
   ```
4. Add lottie package:
   ```yaml
   dependencies:
     lottie: ^3.0.0
   ```
5. Code will auto-detect and use it

---

## Current Visual Features (No Assets Needed)

### ✅ Implemented:
1. **Metallic Rim** - Radial gradient with bevel
2. **Inner Shadow** - Depth and dimension
3. **Finger Stop** - Animated bounce with glint
4. **Number Holes** - Deep inset with shadows
5. **Glossy Highlights** - Rim and hole highlights
6. **Center Cap** - Brushed metal with phone icon
7. **Grain Overlay** - Procedural noise
8. **Parallax Shadow** - Moves with rotation
9. **Notch Flashes** - Yellow radial highlights
10. **Embossed Text** - 3D number effect

### 🎨 Material Effects:
- Radial gradients for depth
- Multiple shadow layers
- Highlight rings
- Metallic sheens
- Brushed metal patterns
- Inset shadows
- Gloss effects

---

## Testing Visuals

1. **Run app**: `flutter run`
2. **Observe**:
   - Metallic rim with gradient
   - Deep hole shadows
   - Finger stop glint
   - Rotation parallax
   - Spring-back bounce
3. **Add texture** (optional):
   - Place `dial_texture.png`
   - Hot reload
   - See subtle grain overlay

---

## Performance Notes

- All effects are GPU-accelerated
- Gradients cached by Flutter
- Grain overlay: 200 dots (minimal impact)
- Texture overlay: 15% opacity (light)
- Lottie: Only if asset exists

---

## Customization

### Change Metal Color:
Edit colors in `_drawMetallicRim()`:
```dart
colors: [
  const Color(0xFF7a6a5a), // Brass
  const Color(0xFF5d4d3d),
  const Color(0xFF3a2a1a),
]
```

### Adjust Gloss:
Edit opacity in `_drawInnerBevel()`:
```dart
..color = Colors.white.withOpacity(0.4) // More gloss
```

### Change Grain:
Edit loop count in `_drawGrainOverlay()`:
```dart
for (int i = 0; i < 500; i++) { // More grain
```

---

## Recommended Asset Sizes

| Asset | Size | Format | Priority |
|-------|------|--------|----------|
| dial_texture.png | 512x512 | PNG | Optional |
| metal_rim.png | 1024x1024 | PNG | Optional |
| hole_mask.png | 128x128 | PNG | Optional |
| rotary_gloss.json | N/A | JSON | Optional |

**Total size**: ~500KB - 2MB (if all assets added)

---

## Next Steps

1. **Test current visuals** - Already looks great!
2. **Add texture** (optional) - For extra realism
3. **Add Lottie** (optional) - For animated gloss
4. **Customize colors** - Match your theme
5. **Share feedback** - Iterate on design
