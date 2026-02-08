package com.usb.drivingremote

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usb.drivingremote.network.*

import com.usb.drivingremote.ui.theme.DrivingRemoteTheme
import kotlinx.coroutines.delay
import com.usb.drivingremote.ui.theme.*
import androidx.compose.runtime.Composable
import com.usb.drivingremote.controls.ControlManager
import com.usb.drivingremote.network.Device
import com.usb.drivingremote.network.WebSocketManager
import com.usb.drivingremote.ui.TestControlsScreen
import com.usb.drivingremote.ui.components.GlassCard
import com.usb.drivingremote.ui.components.LatencyIndicator
import com.usb.drivingremote.ui.theme.TextSecondary
import com.usb.drivingremote.data.repository.LayoutRepository
import com.usb.drivingremote.ui.layouts.LayoutListScreen
import com.usb.drivingremote.ui.playmode.PlayModeScreen
import com.usb.drivingremote.ui.editor.EditModeScreen
import com.usb.drivingremote.utils.FileUtils

/* ======================= ACTIVITY ======================= */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DrivingRemoteTheme {
                DrivingRemoteApp()
            }
        }
    }
}

/* ======================= APP ROOT ======================= */

@Composable
fun DrivingRemoteApp() {

    val context = LocalContext.current

    // 🔑 SINGLE source of truth
    val socketManager = remember { WebSocketManager("") }

    // 🔍 UDP discovery feeds directly into WebSocketManager
    val discoveryListener = remember {
        DiscoveryListener(context) { device ->
            socketManager.addOrUpdateDevice(device)
        }
    }

    DisposableEffect(Unit) {
        discoveryListener.start()
        onDispose { discoveryListener.stop() }
    }

    var currentDestination by rememberSaveable {
        mutableStateOf(AppDestination.CONNECTION)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                AppDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == currentDestination,
                        onClick = { currentDestination = destination },
                        icon = { destination.icon() },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentDestination) {
            AppDestination.CONNECTION -> ConnectionScreen(
                modifier = Modifier.padding(innerPadding),
                socketManager = socketManager
            )

            AppDestination.CONTROLLER -> ControllerScreen(
                modifier = Modifier.padding(innerPadding),
                socketManager = socketManager
            )
        }
    }
}

/* ======================= NAV ======================= */

enum class AppDestination(
    val label: String,
    val icon: @Composable () -> Unit
) {
    CONNECTION("Connection", {
        Icon(Icons.Default.Settings, contentDescription = null)
    }),
    CONTROLLER("Controller", {
        Icon(Icons.Default.Create, contentDescription = null)
    })
}

enum class ConnectionTab { WIFI, USB }

/* ======================= CONNECTION SCREEN ======================= */

@Composable
fun ConnectionScreen(
    modifier: Modifier = Modifier,
    socketManager: WebSocketManager
) {
    var selectedTab by remember { mutableStateOf(ConnectionTab.WIFI) }

    Column(modifier.fillMaxSize()) {

        TabRow(selectedTabIndex = selectedTab.ordinal) {
            Tab(
                selected = selectedTab == ConnectionTab.WIFI,
                onClick = { selectedTab = ConnectionTab.WIFI },
                text = { Text("Wi-Fi") }
            )
            Tab(
                selected = selectedTab == ConnectionTab.USB,
                onClick = { selectedTab = ConnectionTab.USB },
                text = { Text("USB") }
            )
        }

        when (selectedTab) {
            ConnectionTab.WIFI -> WifiConnectionContent(socketManager)
            ConnectionTab.USB -> UsbConnectionScreen(socketManager)
        }
    }
}

/* ======================= WIFI ======================= */

@Composable
private fun WifiConnectionContent(socketManager: WebSocketManager) {

    val devices = socketManager.devices
    var manualUrl by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(16.dp)
    ) {

        Text("Wi-Fi Connection", fontSize = 24.sp)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = manualUrl,
            onValueChange = { manualUrl = it },
            label = { Text("Server URL") },
            placeholder = { Text("ws://192.168.x.x:5000/ws") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (manualUrl.isNotBlank())
                    socketManager.connectTo(manualUrl)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect Manually")
        }

        Spacer(Modifier.height(24.dp))
        Text("Discovered Devices", fontSize = 20.sp)

        devices.forEach { device ->
            DeviceCard(device, socketManager)

        }
    }
}

/* ======================= USB ======================= */

@Composable
fun UsbConnectionScreen(socketManager: WebSocketManager) {

    val context = LocalContext.current
    val isUsbOn = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isUsbOn.value = isUsbTetheringEnabled()
            delay(1000)
        }
    }

    val usbDevices = socketManager.devices.filter {
        it.id.startsWith("ws://10.") || it.id.startsWith("ws://192.")
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp)
    ) {

        Text("USB Connection", fontSize = 24.sp)
        Spacer(Modifier.height(16.dp))

        if (!isUsbOn.value) {
            Text("USB tethering OFF", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { openUsbTetheringSettings(context) }) {
                Text("Enable USB Tethering")
            }
            return@Column
        }

        if (usbDevices.isEmpty()) {
            Text("Waiting for PC…")
        } else {
            usbDevices.forEach { device ->
                DeviceCard(device, socketManager)
            }
        }
    }
}
/* ======================= DEVICE CARD ======================= */

@Composable
fun DeviceCard(
    device: Device,
    socketManager: WebSocketManager
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {

        // 🔑 THIS COLUMN FIXES EVERYTHING
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            /* ---------- HEADER ---------- */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = device.id,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                if (device.isConnected && device.lastLatencyMs != null) {
                    LatencyIndicator(device.lastLatencyMs!!)
                } else {
                    Text(
                        "Disconnected",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            /* ---------- ACTION BUTTONS ---------- */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                if (!device.isConnected) {
                    Button(
                        onClick = { socketManager.connectTo(device.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Connect")
                    }
                } else {
                    OutlinedButton(
                        onClick = { socketManager.connectTo(device.id) }, // refresh
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Refresh")
                    }

                    OutlinedButton(
                        onClick = { socketManager.disconnect() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Disconnect")
                    }
                }
            }
        }
    }
}

/* ======================= CONTROLLER ======================= */

@Composable
fun ControllerScreen(
    modifier: Modifier = Modifier,
    socketManager: WebSocketManager
) {
    val context = LocalContext.current
    val state = socketManager.state
    val latency = socketManager.latencyMs
    
    // Layout repository
    val layoutRepository = remember { LayoutRepository(context) }
    
    // Control manager (single instance)
    val controlManager = remember {
        ControlManager(socketManager)
    }
    
    // Navigation state
    var currentMode by remember { mutableStateOf<ControllerMode>(ControllerMode.LayoutList) }
    
    // State for export
    var layoutToExport by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            layoutToExport?.let { (layoutId, _) ->
                val json = layoutRepository.exportLayout(layoutId)
                if (json != null) {
                    val success = FileUtils.writeToUri(context, uri, json)
                    Toast.makeText(
                        context,
                        if (success) "Layout exported successfully" else "Export failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            layoutToExport = null
        }
    }
    
    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val json = FileUtils.readFromUri(context, uri)
                if (json != null) {
                    val imported = layoutRepository.importLayout(json)
                    Toast.makeText(
                        context,
                        "Imported: ${imported.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    LaunchedEffect(Unit) {
        controlManager.start()
    }
    
    DisposableEffect(Unit) {
        onDispose { controlManager.stop() }
    }
    
    // Render current mode
    when (val mode = currentMode) {
        is ControllerMode.LayoutList -> {
            LayoutListScreen(
                layoutRepository = layoutRepository,
                onOpenLayout = { layoutId ->
                    currentMode = ControllerMode.Play(layoutId)
                },
                onEditLayout = { layoutId ->
                    currentMode = ControllerMode.Edit(layoutId)
                },
                onImportLayout = {
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                },
                onExportLayout = { layoutId, fileName ->
                    layoutToExport = Pair(layoutId, fileName)
                    exportLauncher.launch(fileName)
                },
                modifier = modifier
            )
        }
        
        is ControllerMode.Play -> {
            PlayModeScreen(
                layoutId = mode.layoutId,
                layoutRepository = layoutRepository,
                controlManager = controlManager,
                onClose = { currentMode = ControllerMode.LayoutList }
            )
        }
        
        is ControllerMode.Edit -> {
            EditModeScreen(
                layoutId = mode.layoutId,
                layoutRepository = layoutRepository,
                onClose = { currentMode = ControllerMode.LayoutList }
            )
        }
    }
}

/**
 * Controller mode state.
 */
sealed class ControllerMode {
    object LayoutList : ControllerMode()
    data class Play(val layoutId: String) : ControllerMode()
    data class Edit(val layoutId: String) : ControllerMode()
}

