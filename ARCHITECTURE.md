# Architecture Documentation

## Overview

The Driving Remote App is built using modern Android development practices with Jetpack Compose for UI and a clean architecture pattern for separation of concerns.

## Technology Stack

- **UI Framework**: Jetpack Compose (Material 3)
- **Language**: Kotlin
- **Networking**: OkHttp (WebSocket)
- **State Management**: Compose State & remember
- **Storage**: JSON files in internal storage
- **Build System**: Gradle (Kotlin DSL)

## Package Structure

```
com.usb.drivingremote/
├── controls/                    # Control system (core logic)
│   ├── Axis.kt                  # Generic axis control
│   ├── ButtonControl.kt         # Momentary button
│   ├── Toggle.kt                # Toggle button
│   ├── Steering.kt              # Steering wheel control
│   ├── HShifter.kt              # H-Shifter control
│   ├── Control.kt               # Base control class
│   ├── ControlConfig.kt         # Control configuration
│   ├── ControlState.kt          # Control state sealed class
│   ├── ControlManager.kt        # Manages control lifecycle
│   ├── ControlSerializer.kt     # Binary packet serialization
│   └── ControlOutput.kt         # Output mapping
│
├── network/                     # Network layer
│   ├── WebSocketManager.kt      # WebSocket client
│   ├── WebSocketState.kt        # Connection state
│   ├── DiscoveryListener.kt     # UDP discovery
│   └── DebugLogger.kt           # Debug logging
│
├── data/                        # Data layer
│   ├── models/                  # Data models
│   │   ├── ControllerLayout.kt  # Layout data class
│   │   └── LayoutControl.kt     # Control in layout
│   └── repository/              # Data repositories
│       └── LayoutRepository.kt  # Layout CRUD operations
│
├── ui/                          # UI layer
│   ├── layouts/                 # Layout management screens
│   │   └── LayoutListScreen.kt  # Layout list & management
│   ├── editor/                  # Layout editor
│   │   └── EditModeScreen.kt    # Edit mode UI
│   ├── playmode/                # Play mode screens
│   │   └── PlayModeScreen.kt    # Control playback
│   ├── controls/                # UI components for controls
│   │   ├── SteeringWheel.kt     # Steering wheel UI
│   │   └── HShifterView.kt      # H-Shifter UI
│   ├── components/              # Reusable UI components
│   │   ├── GlassCard.kt         # Card with glass effect
│   │   └── LatencyIndicator.kt  # Network latency display
│   ├── theme/                   # App theming
│   └── TestControlsScreen.kt    # Legacy test screen
│
├── utils/                       # Utilities
│   └── LayoutSerializer.kt      # JSON import/export
│
└── MainActivity.kt              # App entry point
```

## Architecture Layers

### 1. Data Layer

**Purpose**: Manages data persistence and business logic

**Components**:
- `LayoutRepository`: CRUD operations for layouts
- Data models: Kotlin data classes with serialization support
- JSON storage: Persistent storage in app's internal directory

**Key Features**:
- Automatic creation of default ETS2/ATS layout
- Built-in layout restoration on app start
- Import/export with validation

### 2. Control System

**Purpose**: Core logic for handling input controls

**Components**:
- `Control`: Base class for all controls
- `ControlConfig`: Configuration (type, label, range, output)
- `ControlState`: Current state (sealed class with variants)
- `ControlManager`: Lifecycle management and packet sending

**Control Types**:
```kotlin
enum class ControlType {
    AXIS,      // Continuous value (-1 to 1 or 0 to 1)
    BUTTON,    // Momentary button (press/release)
    TOGGLE,    // Toggle switch (on/off)
    HAT,       // D-pad / POV hat
    SHIFTER,   // Gear shifter
    SENSOR     // Gyroscope/accelerometer
}
```

**Control Flow**:
1. User interacts with UI control
2. UI updates Control state
3. UI calls `controlManager.markDirty()`
4. ControlManager serializes all controls to binary packet
5. Binary packet sent via WebSocket

### 3. Network Layer

**Purpose**: Communication with PC server

**Components**:
- `WebSocketManager`: Manages WebSocket connection
- `DiscoveryListener`: UDP broadcast listener for auto-discovery
- Binary protocol: Efficient control data transmission

**Network Protocol**:

#### Discovery (UDP)
- Server broadcasts on port 8888
- Payload: JSON with server info
```json
{
  "name": "My PC",
  "url": "ws://192.168.1.100:5000/ws"
}
```

#### Control Descriptor (JSON)
Sent once on connection to describe available controls:
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

#### Control Data (Binary)
Efficient binary packets sent at ~60 Hz:
```
[Header: 0xFF, 0xFE]
[Control Count: 1 byte]
For each control:
  [Control ID: 1 byte]
  [Value: 4 bytes float, little-endian]
```

### 4. UI Layer

**Purpose**: User interface with Jetpack Compose

**Screen Hierarchy**:
```
DrivingRemoteApp
├── ConnectionScreen
│   ├── WifiConnectionContent
│   └── UsbConnectionScreen
└── ControllerScreen
    ├── LayoutListScreen (default)
    ├── PlayModeScreen (when layout opened)
    └── EditModeScreen (when editing layout)
```

**Key UI Patterns**:
- `remember` for state management
- `LaunchedEffect` for side effects
- `DisposableEffect` for cleanup
- Landscape orientation lock for play/edit modes
- Material 3 design system

## Control Manager Deep Dive

### Responsibilities
1. **Registry**: Track all active controls
2. **Serialization**: Convert controls to binary packets
3. **Transmission**: Send packets via WebSocketManager
4. **Lifecycle**: Start/stop background coroutines
5. **Descriptor**: Send control metadata to server

### Implementation Details

```kotlin
class ControlManager(
    private val webSocketManager: WebSocketManager
) {
    private val controls = mutableMapOf<String, Control>()
    private val idMap = mutableMapOf<String, Byte>()
    
    fun register(control: Control) {
        // Assign unique ID
        // Add to registry
        // Send immediately
    }
    
    fun markDirty() {
        // Throttled packet send
        // Uses atomic flag to prevent overlap
    }
    
    private fun sendPacket() {
        // Serialize all controls
        // Send binary via WebSocket
    }
}
```

### Packet Building

1. Collect all control states
2. Map each control to its ID (1-byte)
3. Serialize state to 4-byte float
4. Prepend header and count
5. Send via `webSocketManager.sendBinary()`

## Layout System

### Data Model

```kotlin
data class ControllerLayout(
    val id: String,
    val name: String,
    val isBuiltIn: Boolean,
    val controls: List<LayoutControl>,
    val lastModified: Long
)

data class LayoutControl(
    val controlType: ControlKind,
    val x: Float,         // 0.0-1.0 (screen percentage)
    val y: Float,         // 0.0-1.0
    val width: Float,     // 0.0-1.0
    val height: Float,    // 0.0-1.0
    val config: ControlConfiguration
)
```

### Persistence

Layouts are stored as JSON in `app/files/layouts.json`:
```json
{
  "layouts": [
    {
      "id": "ets2_ats_default",
      "name": "ETS2/ATS",
      "isBuiltIn": true,
      "controls": [...]
    }
  ]
}
```

### Import/Export Format (.dr files)

```json
{
  "version": "1.0",
  "layout": {
    "name": "My Layout",
    "controls": [
      {
        "type": "STEERING",
        "x": 0.0,
        "y": 0.3,
        "width": 0.45,
        "height": 0.6,
        "config": {
          "id": "steering",
          "outputAxis": "X",
          "deadzone": 0.05,
          "min": -1.0,
          "max": 1.0,
          "curve": "Linear"
        }
      }
    ]
  }
}
```

## Adding New Control Types

### 1. Define Control Logic

Create a new object in `controls/` package:

```kotlin
object MyControl {
    fun create(
        id: String,
        label: String,
        // ... config params
    ): Control {
        return Control(
            ControlConfig(
                id = id,
                type = ControlType.AXIS,  // or new type
                label = label,
                // ... other config
            ),
            ControlState.Axis(0f)  // initial state
        )
    }
    
    fun set(control: Control, value: Any) {
        // Update control state
    }
    
    fun get(control: Control): Any {
        // Get current value
    }
}
```

### 2. Create UI Component

In `ui/controls/`:

```kotlin
@Composable
fun MyControlView(
    control: Control,
    onChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Render control
    // Handle user input
    // Call onChange() when state changes
}
```

### 3. Add to ControlKind Enum

```kotlin
enum class ControlKind {
    STEERING,
    SLIDER,
    BUTTON_HOLD,
    BUTTON_TOGGLE,
    H_SHIFTER,
    MY_CONTROL  // Add here
}
```

### 4. Update PlayModeScreen

Add rendering case in `RenderControl()`:

```kotlin
when (layoutControl.controlType) {
    // ... existing cases
    ControlKind.MY_CONTROL -> {
        MyControlView(
            control = control,
            onChange = onUpdate
        )
    }
}
```

### 5. Update createControlFromLayout

```kotlin
ControlKind.MY_CONTROL -> {
    MyControl.create(
        id = config.id,
        label = config.label,
        // ... other params from config
    )
}
```

## Performance Considerations

### 1. Control Updates
- Throttled to prevent packet spam
- Atomic flag prevents overlapping sends
- Binary format minimizes bandwidth

### 2. UI Rendering
- `remember` prevents unnecessary recomposition
- `derivedStateOf` for computed values
- Canvas used for complex drawings (H-Shifter)

### 3. Network
- WebSocket for low-latency bidirectional communication
- Binary packets instead of JSON for control data
- UDP discovery to minimize connection setup time

## State Management

### Connection State
```kotlin
sealed class WebSocketState {
    object Disconnected : WebSocketState()
    object Connecting : WebSocketState()
    object Connected : WebSocketState()
    data class Error(val message: String) : WebSocketState()
}
```

### Control State
```kotlin
sealed class ControlState {
    data class Axis(val value: Float)
    data class Button(val pressed: Boolean)
    data class Toggle(val on: Boolean)
    data class Shifter(val currentGear: Int, val shifterType: String)
}
```

## Security Considerations

1. **Network**: Uses WebSocket (WS, not WSS) - only for local network
2. **Storage**: Layouts stored in app-private directory
3. **Permissions**: Minimal required (Internet, Network State, Wi-Fi State)
4. **No External APIs**: All communication is local

## Testing Strategy

1. **Unit Tests**: Control logic and serialization
2. **Integration Tests**: LayoutRepository operations
3. **UI Tests**: Screen navigation and control interactions
4. **Manual Testing**: Actual gameplay with ETS2/ATS

## Future Enhancements

- Custom control types via plugins
- Cloud layout sharing
- Haptic feedback
- Voice commands
- Multiple device support (co-op)
- Bluetooth connectivity
