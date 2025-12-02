# Rotary Dialer Development Standards

## Design Principles
- Maintain dark/retro aesthetic with gold (#d4af37) and brown (#8b7355) accents
- Use circular layouts to mimic classic rotary phone design
- Keep animations smooth and tactile (200-300ms transitions)

## Color Palette
- Background: #1a1a1a (dark gray)
- Primary: #2d2d2d (medium gray)
- Accent: #d4af37 (gold)
- Secondary: #8b7355 (brown)
- Display: #0a0a0a (near black)

## Code Standards
- Use StatefulWidget for interactive components
- Implement proper state management for dialed numbers
- Add visual feedback for user interactions (color changes, shadows)
- Follow Flutter best practices for widget composition

## Future Considerations
- Plan for sound integration in assets/sounds/
- Reserve assets/images/ for rotary phone graphics
- Consider gesture-based rotation for enhanced UX
