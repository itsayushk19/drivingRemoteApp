# Driving Remote App

A production-ready virtual steering application for controlling truck and car simulators (ETS2, ATS, etc.) from your Android device via Wi-Fi or USB connection.

## Features

- **🎮 Layout Management**: Create, edit, and manage multiple controller layouts
- **🚗 Built-in ETS2/ATS Layout**: Pre-configured layout with steering wheel, throttle, brake, and buttons
- **✏️ Visual Editor**: Drag-and-drop interface for customizing controls
- **🔧 Highly Configurable**: Adjust axis mapping, deadzone, curves, and more
- **🚛 H-Shifter Support**: Multiple truck transmission patterns (Eaton Fuller 10/13/18-speed, 6-speed, 5-speed)
- **💾 Import/Export**: Share layouts with .dr file format
- **📡 Network Options**: Connect via Wi-Fi or USB tethering
- **⚡ Low Latency**: Optimized for real-time control with minimal lag

## Supported Controls

- **Steering Wheel**: Full rotation with customizable max degrees
- **Sliders**: Vertical/horizontal axis controls (throttle, brake, etc.)
- **Buttons**: Hold and toggle types for horn, lights, wipers, etc.
- **H-Shifter**: Manual transmission with various gear patterns

## Installation

1. Download the APK from the [Releases](https://github.com/itsayushk19/drivingRemoteApp/releases) page
2. Install on your Android device (requires Android 8.0 or higher)
3. Install the companion server software on your PC (see [Server Setup](#server-setup))

## Quick Start

### 1. Connection

#### Wi-Fi Connection
1. Ensure your Android device and PC are on the same network
2. Start the server on your PC
3. Open the app and go to the **Connection** tab
4. The app will automatically discover nearby servers
5. Tap **Connect** on your server

#### USB Connection
1. Enable USB tethering on your Android device (Settings → Network → Tethering)
2. Connect your device to PC via USB cable
3. Start the server on your PC
4. Open the app and go to the **Connection** tab → **USB** tab
5. Tap **Connect** when the server appears

### 2. Using a Layout

1. Go to the **Controller** tab
2. You'll see a list of available layouts (including the built-in ETS2/ATS layout)
3. **Tap** a layout to open it in play mode
4. **Long press** a layout to see options (Open, Edit, Delete)

### 3. Playing

- The device will automatically rotate to landscape mode
- Use the on-screen controls to operate your simulator
- Tap the **X** button to exit play mode

## Creating Custom Layouts

### Using the Editor

1. In the **Controller** tab, tap the **+** button to create a new layout
2. The editor will open in landscape mode
3. Tap the **+ FAB** button to add controls:
   - Steering Wheel
   - Slider (Throttle/Brake)
   - Button (Hold/Toggle)
   - H-Shifter

### Configuring Controls

1. **Tap a control** to open its configuration dialog
2. Adjust the following settings:
   - **Label**: Display name for the control
   - **Output Axis**: Which axis to send data to (X, Y, Z, RX, RY, RZ, Slider1, Slider2)
   - **Size**: Width and height as percentage of screen
   - **Deadzone**: Dead zone percentage (0-50%)
   - **Min/Max**: Output range limits
   - **Curve**: Response curve (Linear, Quadratic, Cubic, Exponential)

3. **Drag controls** to reposition them
4. **Tap Save** (top-right) to save your layout

## Import/Export Layouts

### Exporting
1. Long press a layout in the list
2. Select **Export**
3. Choose where to save the .dr file
4. Share the file with others

### Importing
1. Tap the **folder icon** in the Controller tab
2. Select a .dr file
3. The layout will be imported and appear in your list

## H-Shifter Patterns

The app supports multiple transmission patterns:

- **Standard 6-Speed**: Car H-pattern (1-2-3-4-5-6 + R)
- **5-Speed H-Pattern**: Classic car transmission
- **Eaton Fuller 10-Speed**: Truck transmission with splitter
- **Eaton Fuller 13-Speed**: Extended range truck transmission
- **Eaton Fuller 18-Speed**: Full range truck transmission

To use:
1. Add an H-Shifter control in the editor
2. Configure the shifter type in the control settings
3. Tap gear positions in play mode to shift

## Server Setup

### Requirements
- PC running Windows, Linux, or macOS
- Node.js (for the JavaScript server) OR
- Python 3.7+ (for the Python server)

### Installation
See the server repository for detailed setup instructions: [Server Repository Link]

## Troubleshooting

### Connection Issues

**Problem**: Server not appearing in device list
- Ensure both devices are on the same network
- Check firewall settings on PC
- Try manual connection by entering the server URL

**Problem**: High latency
- Use USB connection for best performance
- Close background apps on your Android device
- Reduce network traffic on your Wi-Fi

### Control Issues

**Problem**: Controls not responding
- Check that you're connected to the server
- Verify the control is configured with a valid output axis
- Restart the app and reconnect

**Problem**: Steering feels wrong
- Adjust the deadzone in edit mode
- Try different response curves (Linear, Quadratic, etc.)
- Check the min/max range settings

### Layout Issues

**Problem**: Can't delete a layout
- Built-in layouts (like ETS2/ATS) cannot be deleted
- They can be edited, but will be restored on app restart

**Problem**: Import failed
- Ensure the .dr file is valid JSON
- Check that the file version is compatible (currently v1.0)

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built with Jetpack Compose
- Uses OkHttp for WebSocket communication
- Inspired by Euro Truck Simulator 2 and American Truck Simulator

## Support

For issues, questions, or feature requests, please open an issue on [GitHub](https://github.com/itsayushk19/drivingRemoteApp/issues).

---

**Happy Trucking! 🚛**
