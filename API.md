# API Documentation

## Overview

This document describes the network protocol used by the Driving Remote App to communicate with the PC server. This is useful for implementing custom servers or integrating with other applications.

## Network Architecture

```
┌─────────────┐                    ┌──────────────┐
│   Android   │                    │   PC Server  │
│     App     │                    │              │
├─────────────┤                    ├──────────────┤
│             │   UDP Discovery    │              │
│ Discovery   │◄──────────────────►│  Broadcast   │
│  Listener   │    Port 8888       │              │
│             │                    │              │
│             │                    │              │
│             │   WebSocket        │              │
│  WebSocket  │◄──────────────────►│  WebSocket   │
│   Manager   │    Port 5000       │   Server     │
│             │                    │              │
└─────────────┘                    └──────────────┘
```

## Discovery Protocol (UDP)

### Purpose
Automatic server discovery on local network without manual IP entry.

### Port
**8888** (UDP broadcast)

### Server Behavior
1. Bind to UDP port 8888
2. Send broadcast packets every 1-2 seconds
3. Include server information in JSON format

### Broadcast Packet Format
```json
{
  "name": "My Gaming PC",
  "url": "ws://192.168.1.100:5000/ws",
  "version": "1.0"
}
```

**Fields**:
- `name`: Human-readable server name (displayed in app)
- `url`: Full WebSocket URL (ws://IP:PORT/PATH)
- `version`: Protocol version (currently "1.0")

### Client Behavior (Android App)
1. Listen on UDP port 8888
2. Receive and parse broadcast packets
3. Extract server URL and name
4. Display in device list
5. Update existing entries if same URL

### Example Server Code (Python)

```python
import socket
import json
import time

def broadcast_server():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    
    message = json.dumps({
        "name": "My PC",
        "url": "ws://192.168.1.100:5000/ws",
        "version": "1.0"
    })
    
    while True:
        sock.sendto(message.encode(), ('<broadcast>', 8888))
        time.sleep(1)
```

### Example Server Code (Node.js)

```javascript
const dgram = require('dgram');
const server = dgram.createSocket('udp4');

server.bind(8888, () => {
    server.setBroadcast(true);
    
    setInterval(() => {
        const message = JSON.stringify({
            name: 'My PC',
            url: 'ws://192.168.1.100:5000/ws',
            version: '1.0'
        });
        
        server.send(message, 8888, '255.255.255.255');
    }, 1000);
});
```

---

## WebSocket Protocol

### Connection

**URL**: `ws://<server-ip>:5000/ws`

**Protocol**: WebSocket (RFC 6455)

**Handshake**: Standard WebSocket handshake

### Message Types

The app sends two types of messages over WebSocket:
1. **Control Descriptor** (JSON, sent once on connection)
2. **Control Data** (Binary, sent continuously at ~60 Hz)

---

## Control Descriptor Message

### Purpose
Inform server about available controls and their properties.

### When Sent
- Once immediately after WebSocket connection
- After registering new controls
- On control configuration change

### Format
JSON text message

### Structure
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
    },
    {
      "id": 2,
      "kind": "axis",
      "label": "Throttle",
      "min": 0.0,
      "max": 1.0,
      "center": null
    },
    {
      "id": 3,
      "kind": "button",
      "label": "Horn",
      "min": 0.0,
      "max": 1.0,
      "center": null
    }
  ]
}
```

### Fields

**Message Level**:
- `type`: Always "control_descriptor"
- `controls`: Array of control definitions

**Control Definition**:
- `id`: Unique control identifier (1-255)
- `kind`: Control type string
  - `"axis"`: Continuous value (steering, throttle, brake)
  - `"button"`: Momentary button
  - `"toggle"`: Toggle switch
  - `"hat"`: D-pad / POV
  - `"shifter"`: Gear selector
- `label`: Human-readable name
- `min`: Minimum output value
- `max`: Maximum output value
- `center`: Center/neutral value (null if not applicable)

### Example Server Handler (Python)

```python
import json

def handle_descriptor(message):
    data = json.loads(message)
    controls = data['controls']
    
    for control in controls:
        id = control['id']
        kind = control['kind']
        label = control['label']
        min_val = control['min']
        max_val = control['max']
        
        print(f"Control {id}: {label} ({kind})")
        print(f"  Range: {min_val} to {max_val}")
        
        # Store control info for later use
        # Map to virtual joystick, vJoy, etc.
```

---

## Control Data Message (Binary)

### Purpose
Send real-time control values to server.

### When Sent
- Continuously at approximately 60 Hz
- When any control value changes
- Throttled to prevent excessive network traffic

### Format
Binary message (WebSocket binary frame)

### Packet Structure

```
Offset | Size | Type   | Description
-------|------|--------|----------------------------------
0      | 1    | byte   | Header byte 1 (0xFF)
1      | 1    | byte   | Header byte 2 (0xFE)
2      | 1    | byte   | Control count (N)
3      | 5*N  | array  | Control data (N controls)
```

### Control Data Entry (5 bytes each)

```
Offset | Size | Type   | Description
-------|------|--------|----------------------------------
0      | 1    | byte   | Control ID (1-255)
1      | 4    | float  | Control value (little-endian)
```

### Example Packet

For 3 controls (Steering, Throttle, Brake):

```
Hex:
FF FE 03 01 3F800000 02 3F000000 03 00000000

Breakdown:
FF FE       - Header
03          - 3 controls
01 3F800000 - Control 1, value 1.0
02 3F000000 - Control 2, value 0.5
03 00000000 - Control 3, value 0.0
```

**Total size**: 2 + 1 + (3 * 5) = 18 bytes

### Value Encoding
- **Format**: IEEE 754 single-precision float (32-bit)
- **Byte Order**: Little-endian
- **Range**: Typically -1.0 to 1.0 or 0.0 to 1.0

### Example Decoder (Python)

```python
import struct

def decode_control_packet(data):
    # Check header
    if data[0] != 0xFF or data[1] != 0xFE:
        print("Invalid header")
        return None
    
    # Get control count
    count = data[2]
    controls = {}
    
    # Parse each control
    offset = 3
    for i in range(count):
        control_id = data[offset]
        value_bytes = data[offset+1:offset+5]
        value = struct.unpack('<f', value_bytes)[0]
        
        controls[control_id] = value
        offset += 5
    
    return controls

# Usage
data = bytes.fromhex('FFFE0301 3F800000 02 3F000000')
controls = decode_control_packet(data)
print(controls)  # {1: 1.0, 2: 0.5}
```

### Example Decoder (Node.js)

```javascript
function decodeControlPacket(buffer) {
    // Check header
    if (buffer[0] !== 0xFF || buffer[1] !== 0xFE) {
        console.error('Invalid header');
        return null;
    }
    
    const count = buffer[2];
    const controls = {};
    
    let offset = 3;
    for (let i = 0; i < count; i++) {
        const id = buffer[offset];
        const value = buffer.readFloatLE(offset + 1);
        
        controls[id] = value;
        offset += 5;
    }
    
    return controls;
}

// Usage
const data = Buffer.from('FFFE03013F80000002 3F000000', 'hex');
const controls = decodeControlPacket(data);
console.log(controls);  // { 1: 1.0, 2: 0.5 }
```

---

## Server Implementation Guide

### Minimal Server Requirements

1. **UDP Broadcast**:
   - Send discovery packets on port 8888
   - Include server name and WebSocket URL

2. **WebSocket Server**:
   - Listen on port 5000 (or custom port)
   - Accept connections on `/ws` path

3. **Message Handling**:
   - Receive and parse control descriptor (JSON)
   - Receive and decode control data (binary)
   - Map controls to virtual joystick or simulator

### Recommended Features

1. **Multiple Clients**: Support multiple concurrent connections
2. **Latency Tracking**: Respond to ping messages
3. **Error Handling**: Graceful handling of disconnections
4. **Control Mapping**: Flexible control-to-axis mapping
5. **Configuration**: Customizable port, name, etc.

### Example Full Server (Python)

```python
import asyncio
import websockets
import json
import struct
import socket
import threading

# UDP Discovery
def broadcast_discovery():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    
    message = json.dumps({
        "name": "My PC",
        "url": "ws://192.168.1.100:5000/ws",
        "version": "1.0"
    })
    
    while True:
        sock.sendto(message.encode(), ('<broadcast>', 8888))
        time.sleep(1)

# WebSocket Handler
async def handle_client(websocket, path):
    print(f"Client connected: {websocket.remote_address}")
    
    try:
        async for message in websocket:
            if isinstance(message, str):
                # JSON descriptor
                data = json.loads(message)
                if data.get('type') == 'control_descriptor':
                    print("Received control descriptor")
                    for control in data['controls']:
                        print(f"  {control['id']}: {control['label']}")
            else:
                # Binary control data
                controls = decode_control_packet(message)
                if controls:
                    # Update virtual joystick, send to simulator, etc.
                    update_controls(controls)
    except Exception as e:
        print(f"Error: {e}")
    finally:
        print(f"Client disconnected: {websocket.remote_address}")

def decode_control_packet(data):
    if data[0] != 0xFF or data[1] != 0xFE:
        return None
    
    count = data[2]
    controls = {}
    offset = 3
    
    for i in range(count):
        control_id = data[offset]
        value = struct.unpack('<f', data[offset+1:offset+5])[0]
        controls[control_id] = value
        offset += 5
    
    return controls

def update_controls(controls):
    # Map to vJoy, DirectInput, or simulator API
    for id, value in controls.items():
        print(f"Control {id}: {value:.3f}")

# Start server
async def main():
    # Start UDP broadcast in background
    threading.Thread(target=broadcast_discovery, daemon=True).start()
    
    # Start WebSocket server
    async with websockets.serve(handle_client, '0.0.0.0', 5000):
        print("Server started on port 5000")
        await asyncio.Future()  # Run forever

if __name__ == '__main__':
    asyncio.run(main())
```

---

## Integration Examples

### vJoy (Windows)

```python
import pyvjoy

# Initialize vJoy device
joystick = pyvjoy.VJoyDevice(1)

def update_controls(controls):
    for id, value in controls.items():
        if id == 1:  # Steering
            # Map -1..1 to 0x1..0x8000
            axis_value = int((value + 1) * 0x4000)
            joystick.set_axis(pyvjoy.HID_USAGE_X, axis_value)
        elif id == 2:  # Throttle
            axis_value = int(value * 0x8000)
            joystick.set_axis(pyvjoy.HID_USAGE_Y, axis_value)
        elif id == 3:  # Button
            joystick.set_button(1, int(value))
```

### Linux uinput

```python
import uinput

device = uinput.Device([
    uinput.ABS_X + (0, 255, 0, 0),
    uinput.ABS_Y + (0, 255, 0, 0),
    uinput.BTN_JOYSTICK
])

def update_controls(controls):
    for id, value in controls.items():
        if id == 1:  # Steering
            device.emit(uinput.ABS_X, int((value + 1) * 127))
        elif id == 2:  # Throttle
            device.emit(uinput.ABS_Y, int(value * 255))
        elif id == 3:  # Button
            device.emit(uinput.BTN_JOYSTICK, int(value))
```

---

## Latency Optimization

### App-Side
- Binary format (18 bytes vs ~200 bytes JSON)
- Throttled updates (~60 Hz, not every frame)
- Single WebSocket connection

### Server-Side
- Avoid heavy processing in WebSocket handler
- Use async/non-blocking I/O
- Batch updates to virtual device

### Network
- Use Wi-Fi 5 GHz for lower latency
- USB tethering for best performance
- Keep devices on same subnet

---

## Testing Tools

### netcat (UDP broadcast test)
```bash
echo '{"name":"Test","url":"ws://192.168.1.100:5000/ws","version":"1.0"}' | nc -u -b 255.255.255.255 8888
```

### Wireshark
- Filter: `udp.port == 8888` for discovery
- Filter: `websocket` for WebSocket traffic

### Browser WebSocket Test
```javascript
const ws = new WebSocket('ws://192.168.1.100:5000/ws');
ws.onmessage = (event) => {
    console.log('Received:', event.data);
};
```

---

## Protocol Versioning

Current version: **1.0**

Future versions may add:
- Compression
- Encryption
- Extended control types
- Bidirectional communication (force feedback)
- Multiple layout sync

Version is included in discovery broadcast and descriptor for compatibility checking.

---

## Error Handling

### Connection Errors
- **Connection Refused**: Server not running
- **Timeout**: Network issues or wrong IP
- **Connection Lost**: Handle reconnection gracefully

### Message Errors
- **Invalid JSON**: Ignore and log
- **Invalid Binary**: Check header, discard packet
- **Unknown Control ID**: Ignore or map to default

### Best Practices
- Log all errors for debugging
- Implement automatic reconnection
- Validate all incoming data
- Handle partial packets

---

## Security Considerations

### Current Design
- **No Authentication**: Designed for trusted local networks
- **No Encryption**: WebSocket (WS), not WSS
- **No Validation**: Assumes client is legitimate

### Recommendations
- Use on private/isolated networks only
- Consider firewall rules to limit access
- For public networks: Add authentication layer
- Future: Implement TLS/SSL for encryption

---

## Frequently Asked Questions

### Why binary format for control data?
**Answer**: Efficiency. JSON would be ~200 bytes vs 18 bytes for 3 controls. At 60 Hz, that's 12 KB/s vs 1 KB/s.

### Why UDP for discovery instead of mDNS/Bonjour?
**Answer**: Simplicity and cross-platform compatibility. UDP broadcast works everywhere without additional libraries.

### Can I use a different port?
**Answer**: Yes, but update both server URL in broadcast and actual WebSocket server port.

### How to handle multiple controllers?
**Answer**: Use different control ID ranges (1-50 for controller 1, 51-100 for controller 2).

### Maximum number of controls?
**Answer**: 255 (due to 1-byte ID). Practical limit is lower (~50) for performance.

---

## Change Log

### Version 1.0 (Current)
- Initial protocol design
- UDP discovery
- WebSocket control data
- Binary packet format
- JSON descriptor format
