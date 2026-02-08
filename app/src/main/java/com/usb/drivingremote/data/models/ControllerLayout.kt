package com.usb.drivingremote.data.models

/**
 * Represents a controller layout configuration.
 *
 * @property id Unique identifier for the layout
 * @property name Display name of the layout
 * @property isBuiltIn Whether this is a built-in layout (cannot be deleted)
 * @property controls List of controls in this layout
 * @property lastModified Timestamp of last modification (milliseconds since epoch)
 */
data class ControllerLayout(
    val id: String,
    val name: String,
    val isBuiltIn: Boolean = false,
    val controls: List<LayoutControl> = emptyList(),
    val lastModified: Long = System.currentTimeMillis()
)
