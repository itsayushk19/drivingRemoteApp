# Control System Documentation

## Overview

The Driving Remote App supports multiple control types, each with extensive configuration options. This document details all available control types, their parameters, and best practices.

## Control Types

### 1. Steering Wheel

**Description**: Full-rotation steering wheel control for precise vehicle steering.

**Use Cases**:
- Car and truck steering
- Aircraft yaw control
- Boat rudder

**Configuration**:
```kotlin
{
  "type": "STEERING",
  "config": {
    "id": "steering",
    "label": "Steering",
    "outputAxis": "X",
    "deadzone": 0.02,
    "min": -1.0,
    "max": 1.0,
    "curve": "Linear"
  }
}
```

**Parameters**:
- **outputAxis**: Which axis to control (X, Y, Z, RX, RY, RZ)
- **deadzone**: Center deadzone (0.0-0.5, recommended: 0.02-0.05)
- **min/max**: Output range (typically -1.0 to 1.0)
- **curve**: Response curve (Linear, Quadratic, Cubic, Exponential)

**Technical Details**:
- Max rotation: ±1900 degrees (5.3 full turns)
- Touch tracking: Circular gesture recognition
- Auto-centering: No (maintains last position)
- Visual feedback: Rotating wheel image

**Tips**:
- Use small deadzone (2-5%) for precise control
- Linear curve recommended for realistic feel
- Place on left side of screen for easy thumb access

---

### 2. Slider (Axis)

**Description**: Linear slider for throttle, brake, and other continuous controls.

**Use Cases**:
- Throttle/accelerator
- Brake
- Clutch
- Mixture control
- Trim controls

**Configuration**:
```kotlin
{
  "type": "SLIDER",
  "config": {
    "id": "throttle",
    "label": "Throttle",
    "outputAxis": "Y",
    "deadzone": 0.05,
    "min": 0.0,
    "max": 1.0,
    "curve": "Linear",
    "sliderOrientation": "VERTICAL"
  }
}
```

**Parameters**:
- **sliderOrientation**: VERTICAL or HORIZONTAL
- **min/max**: 
  - Throttle: 0.0 to 1.0
  - Brake: 0.0 to 1.0
  - Combined axis: -1.0 to 1.0
- **deadzone**: Bottom/top deadzone (recommended: 0.05)
- **curve**: 
  - Linear: 1:1 mapping
  - Quadratic: More control at low end
  - Cubic: Even more precision at low values
  - Exponential: Aggressive low-end precision

**Technical Details**:
- Direction: Vertical = bottom to top, Horizontal = left to right
- Snapping: Optional snap to 0% or 100%
- Visual: Material 3 slider component

**Tips**:
- Vertical orientation recommended for throttle/brake
- Use Quadratic curve for better throttle control
- Place throttle/brake side-by-side on right side
- Larger size (10-15% width, 70-80% height) for easier use

---

### 3. Button (Hold)

**Description**: Momentary button - active only while pressed.

**Use Cases**:
- Horn
- Flash lights
- Handbrake
- Boost/nitro
- PTT (Push-to-talk)

**Configuration**:
```kotlin
{
  "type": "BUTTON_HOLD",
  "config": {
    "id": "horn",
    "label": "Horn",
    "buttonIndex": 1
  }
}
```

**Parameters**:
- **buttonIndex**: Button number (1-32)
- **label**: Display text on button

**Technical Details**:
- State: Boolean (pressed/released)
- Timing: Instant response on press/release
- Visual: Material button with press feedback

**Tips**:
- Use for actions that should stop immediately when released
- Group similar buttons together (lights, wipers, etc.)
- Make them large enough for quick access (10-15% screen size)

---

### 4. Button (Toggle)

**Description**: Toggle button - stays on/off until pressed again.

**Use Cases**:
- Headlights
- Engine start/stop
- Cruise control
- Parking brake
- Wipers on/off

**Configuration**:
```kotlin
{
  "type": "BUTTON_TOGGLE",
  "config": {
    "id": "lights",
    "label": "Lights",
    "buttonIndex": 2
  }
}
```

**Parameters**:
- **buttonIndex**: Button number (1-32)
- **label**: Display text on button

**Technical Details**:
- State: Boolean (on/off)
- Visual: Different color when active
- Persistent: Maintains state until toggled

**Tips**:
- Use for settings that persist (lights, cruise control)
- Clear labeling to show current state
- Consider using different button index than momentary buttons

---

### 5. H-Shifter

**Description**: Manual transmission gear selector with multiple patterns.

**Use Cases**:
- Truck manual transmission
- Car stick shift
- Sequential shifter (in future)

**Configuration**:
```kotlin
{
  "type": "H_SHIFTER",
  "config": {
    "id": "shifter",
    "label": "Gearbox",
    "shifterType": "EATON_18_SPEED",
    "outputAxis": "Shifter"
  }
}
```

**Parameters**:
- **shifterType**: Pattern type (see below)

**Available Patterns**:

#### Standard 6-Speed (Car)
```
Layout:  R  1  3  5
            2  4  6
            
Gears: R, N, 1-6
```
Use case: Regular car transmission

#### H-Pattern 5-Speed (Classic Car)
```
Layout:  R  1  3  5
            2  4
            
Gears: R, N, 1-5
```
Use case: Older vehicles, sports cars

#### Eaton Fuller 10-Speed (Truck)
```
Layout: Split-range transmission
Low Range:  1L 2L 3L 4L 5L
High Range: 1H 2H 3H 4H 5H

Gears: R, N, 1-10
```
Use case: Medium-duty trucks

#### Eaton Fuller 13-Speed (Truck)
```
Layout: Extended range
Gears: R, N, 1-13

Pattern: 2x6.5 split range
```
Use case: Heavy-duty trucks

#### Eaton Fuller 18-Speed (Truck)
```
Layout: Full range transmission
Gears: R, N, 1-18

Pattern: 2x9 split range with splitter
```
Use case: Long-haul trucks, ETS2/ATS

**Technical Details**:
- Gear detection: Closest position to touch
- Visual: Gates, rails, and gear labels
- Current gear: Highlighted in cyan
- Neutral: Always available at center

**Tips**:
- Place in center or right side of screen
- Size: 30-40% width, 40-60% height
- Practice pattern before driving
- For Eaton transmissions:
  - Low range (1L-5L): City driving
  - High range (1H-5H): Highway driving
  - Use splitter for fine control

---

## Output Axis Mapping

Controls send data to specific axes. Common mappings:

| Axis | Common Use | Range | Notes |
|------|------------|-------|-------|
| X | Steering | -1.0 to 1.0 | Left negative, right positive |
| Y | Throttle | 0.0 to 1.0 | Or -1.0 to 1.0 for combined |
| Z | Brake | 0.0 to 1.0 | |
| RX | Look Left/Right | -1.0 to 1.0 | Head tracking |
| RY | Look Up/Down | -1.0 to 1.0 | Head tracking |
| RZ | Roll | -1.0 to 1.0 | Aircraft |
| Slider1 | Clutch | 0.0 to 1.0 | |
| Slider2 | Custom | 0.0 to 1.0 | Mixture, trim, etc. |

**Important**: Different simulators may use different mappings. Check your simulator's input settings.

---

## Deadzone Explained

**What is Deadzone?**
A range around the center (for centered axes) or edges (for 0-1 axes) where input is ignored.

**Why Use It?**
- Eliminates jitter from touch input
- Prevents accidental inputs
- Creates a "neutral" zone

**Recommended Values**:
- **Steering**: 2-5% (precise control needed)
- **Throttle/Brake**: 5-10% (prevent ghost inputs)
- **Sliders**: 5% (depends on sensitivity)

**Example**:
With 5% deadzone on throttle (0.0-1.0):
- 0.00-0.05 → Output: 0.0
- 0.05-1.00 → Output: 0.0-1.0 (scaled)

---

## Response Curves

Response curves change how input values map to output values.

### Linear (Default)
```
Input:  0.0  0.25  0.5  0.75  1.0
Output: 0.0  0.25  0.5  0.75  1.0
```
**Use**: Most controls, realistic feel

### Quadratic
```
Input:  0.0  0.25  0.5  0.75  1.0
Output: 0.0  0.06  0.25  0.56  1.0
```
**Use**: Throttle for fine control at low speeds

### Cubic
```
Input:  0.0  0.25  0.5  0.75  1.0
Output: 0.0  0.02  0.13  0.42  1.0
```
**Use**: Very precise low-end control

### Exponential
```
Input:  0.0  0.25  0.5  0.75  1.0
Output: 0.0  0.08  0.33  0.73  1.0
```
**Use**: Aggressive low-end response

**Visual Comparison**:
```
1.0 |                    /
    |                  /
    |               /  ← Linear
    |            /  ← Exponential
0.5 |         /   ← Quadratic
    |      /     ← Cubic
    |   /
0.0 |/________________
    0.0      0.5     1.0
```

---

## Min/Max Range

Limits the output range of a control.

**Examples**:

1. **Limited Throttle** (for fuel economy):
```
min: 0.0
max: 0.75
→ Full slider = 75% throttle
```

2. **Inverted Brake**:
```
min: 1.0
max: 0.0
→ Bottom = full brake, top = no brake
```

3. **Reduced Steering**:
```
min: -0.5
max: 0.5
→ Half steering range (easier control)
```

---

## Best Practices

### Layout Design
1. **Ergonomics**: Place frequently used controls within easy reach
2. **Grouping**: Group related controls (lights, wipers, etc.)
3. **Size**: Larger = easier to use, but takes more screen space
4. **Spacing**: Leave gaps to prevent accidental presses

### Control Configuration
1. **Test**: Try different deadzones and curves
2. **Consistency**: Use similar settings for similar controls
3. **Simulator**: Match your simulator's expected input
4. **Practice**: Test layout before long sessions

### H-Shifter Usage
1. **Learn Pattern**: Memorize gear positions before playing
2. **Visual**: Watch gear labels while learning
3. **Muscle Memory**: Practice shifting without looking
4. **Realistic**: Match your in-game truck's transmission

### Performance
1. **Minimal Controls**: Only add controls you actually use
2. **Reasonable Updates**: Avoid excessive control updates
3. **Optimize**: Use larger deadzones if experiencing lag

---

## Control Protocol Details

### Control Descriptor
Sent once on connection:
```json
{
  "type": "control_descriptor",
  "controls": [
    {
      "id": 1,
      "kind": "axis",
      "label": "Steering",
      "min": -1.0,
      "max": 1.0,
      "center": 0.0
    }
  ]
}
```

### Control Updates
Binary packet (60 Hz):
```
Header: [0xFF, 0xFE]
Count:  [0x03]           # 3 controls
Control 1:
  ID:    [0x01]          # Steering
  Value: [0x3F800000]    # 1.0 (float, little-endian)
Control 2:
  ID:    [0x02]          # Throttle
  Value: [0x3F000000]    # 0.5
Control 3:
  ID:    [0x03]          # Brake
  Value: [0x00000000]    # 0.0
```

Total packet size: 2 + 1 + (3 * 5) = 18 bytes

---

## Troubleshooting

### Problem: Controls not responding
- Check connection status
- Verify output axis is correct
- Ensure control is registered
- Check server logs

### Problem: Jittery input
- Increase deadzone
- Check network latency
- Reduce number of active controls

### Problem: Wrong direction
- Swap min/max values
- Check simulator input settings
- Verify axis mapping

### Problem: H-Shifter wrong gear
- Touch directly on gear position
- Try different shifter pattern
- Check if neutral is detected

---

## Future Control Types (Planned)

- **D-Pad / HAT**: 8-direction POV control
- **Gyroscope**: Tilt steering
- **Accelerometer**: Motion controls
- **Sequential Shifter**: Up/down only
- **Rotary Encoder**: Infinite rotation dial
- **Custom**: User-defined control types
