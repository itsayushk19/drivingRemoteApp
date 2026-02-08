package com.usb.drivingremote.data.repository

import android.content.Context
import com.usb.drivingremote.data.models.*
import com.usb.drivingremote.utils.LayoutSerializer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Repository for managing controller layouts.
 * Handles CRUD operations and persistence to internal storage.
 */
class LayoutRepository(private val context: Context) {
    
    private val layoutsFile = File(context.filesDir, "layouts.json")
    private val layouts = mutableListOf<ControllerLayout>()
    
    init {
        loadLayouts()
        ensureDefaultLayout()
    }
    
    /**
     * Get all layouts.
     */
    fun getAllLayouts(): List<ControllerLayout> {
        return layouts.toList()
    }
    
    /**
     * Get a layout by ID.
     */
    fun getLayout(id: String): ControllerLayout? {
        return layouts.firstOrNull { it.id == id }
    }
    
    /**
     * Add a new layout.
     */
    fun addLayout(layout: ControllerLayout) {
        layouts.add(layout)
        saveLayouts()
    }
    
    /**
     * Update an existing layout.
     */
    fun updateLayout(layout: ControllerLayout) {
        val index = layouts.indexOfFirst { it.id == layout.id }
        if (index >= 0) {
            layouts[index] = layout.copy(lastModified = System.currentTimeMillis())
            saveLayouts()
        }
    }
    
    /**
     * Delete a layout (only if not built-in).
     */
    fun deleteLayout(id: String): Boolean {
        val layout = layouts.firstOrNull { it.id == id }
        if (layout?.isBuiltIn == true) {
            return false // Cannot delete built-in layouts
        }
        
        layouts.removeAll { it.id == id }
        saveLayouts()
        return true
    }
    
    /**
     * Create a new custom layout with default empty configuration.
     */
    fun createCustomLayout(name: String): ControllerLayout {
        val newLayout = ControllerLayout(
            id = UUID.randomUUID().toString(),
            name = name,
            isBuiltIn = false,
            controls = emptyList()
        )
        addLayout(newLayout)
        return newLayout
    }
    
    /**
     * Import a layout from JSON string.
     * Generates a new ID to avoid conflicts.
     */
    fun importLayout(jsonString: String): ControllerLayout {
        val (name, controls) = LayoutSerializer.deserialize(jsonString)
        val newLayout = ControllerLayout(
            id = UUID.randomUUID().toString(),
            name = name,
            isBuiltIn = false,
            controls = controls,
            lastModified = System.currentTimeMillis()
        )
        addLayout(newLayout)
        return newLayout
    }
    
    /**
     * Export a layout to JSON string.
     */
    fun exportLayout(id: String): String? {
        val layout = getLayout(id) ?: return null
        return LayoutSerializer.serialize(layout)
    }
    
    /**
     * Load layouts from disk.
     */
    private fun loadLayouts() {
        if (!layoutsFile.exists()) {
            return
        }
        
        try {
            val jsonString = layoutsFile.readText()
            val json = JSONObject(jsonString)
            val layoutsArray = json.getJSONArray("layouts")
            
            layouts.clear()
            for (i in 0 until layoutsArray.length()) {
                val layoutObj = layoutsArray.getJSONObject(i)
                layouts.add(parseLayout(layoutObj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If load fails, clear and start fresh
            layouts.clear()
        }
    }
    
    /**
     * Save layouts to disk.
     */
    private fun saveLayouts() {
        try {
            val json = JSONObject()
            val layoutsArray = JSONArray()
            
            layouts.forEach { layout ->
                layoutsArray.put(serializeLayout(layout))
            }
            
            json.put("layouts", layoutsArray)
            layoutsFile.writeText(json.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Ensure the default ETS2/ATS layout exists.
     */
    private fun ensureDefaultLayout() {
        if (layouts.none { it.id == DEFAULT_LAYOUT_ID }) {
            layouts.add(0, createDefaultLayout())
            saveLayouts()
        } else {
            // Always restore built-in layout on app start
            val index = layouts.indexOfFirst { it.id == DEFAULT_LAYOUT_ID }
            if (index >= 0) {
                layouts[index] = createDefaultLayout()
                saveLayouts()
            }
        }
    }
    
    /**
     * Parse a layout from JSON object.
     */
    private fun parseLayout(obj: JSONObject): ControllerLayout {
        val id = obj.getString("id")
        val name = obj.getString("name")
        val isBuiltIn = obj.optBoolean("isBuiltIn", false)
        val lastModified = obj.optLong("lastModified", System.currentTimeMillis())
        
        val controlsArray = obj.getJSONArray("controls")
        val controls = mutableListOf<LayoutControl>()
        
        for (i in 0 until controlsArray.length()) {
            val controlObj = controlsArray.getJSONObject(i)
            controls.add(parseControl(controlObj))
        }
        
        return ControllerLayout(id, name, isBuiltIn, controls, lastModified)
    }
    
    /**
     * Parse a control from JSON object.
     */
    private fun parseControl(obj: JSONObject): LayoutControl {
        val type = ControlKind.valueOf(obj.getString("type"))
        val x = obj.getDouble("x").toFloat()
        val y = obj.getDouble("y").toFloat()
        val width = obj.getDouble("width").toFloat()
        val height = obj.getDouble("height").toFloat()
        
        val configObj = obj.getJSONObject("config")
        val config = ControlConfiguration(
            id = configObj.getString("id"),
            label = configObj.optString("label", ""),
            outputAxis = configObj.optString("outputAxis", "X"),
            deadzone = configObj.optDouble("deadzone", 0.05).toFloat(),
            min = configObj.optDouble("min", -1.0).toFloat(),
            max = configObj.optDouble("max", 1.0).toFloat(),
            curve = ResponseCurve.valueOf(configObj.optString("curve", "LINEAR")),
            buttonIndex = if (configObj.has("buttonIndex")) 
                configObj.getInt("buttonIndex") else null,
            sliderOrientation = SliderOrientation.valueOf(
                configObj.optString("sliderOrientation", "VERTICAL")
            ),
            shifterType = ShifterType.valueOf(
                configObj.optString("shifterType", "STANDARD_6_SPEED")
            )
        )
        
        return LayoutControl(type, x, y, width, height, config)
    }
    
    /**
     * Serialize a layout to JSON object.
     */
    private fun serializeLayout(layout: ControllerLayout): JSONObject {
        val obj = JSONObject()
        obj.put("id", layout.id)
        obj.put("name", layout.name)
        obj.put("isBuiltIn", layout.isBuiltIn)
        obj.put("lastModified", layout.lastModified)
        
        val controlsArray = JSONArray()
        layout.controls.forEach { control ->
            controlsArray.put(serializeControl(control))
        }
        obj.put("controls", controlsArray)
        
        return obj
    }
    
    /**
     * Serialize a control to JSON object.
     */
    private fun serializeControl(control: LayoutControl): JSONObject {
        val obj = JSONObject()
        obj.put("type", control.controlType.name)
        obj.put("x", control.x)
        obj.put("y", control.y)
        obj.put("width", control.width)
        obj.put("height", control.height)
        
        val configObj = JSONObject()
        configObj.put("id", control.config.id)
        configObj.put("label", control.config.label)
        configObj.put("outputAxis", control.config.outputAxis)
        configObj.put("deadzone", control.config.deadzone)
        configObj.put("min", control.config.min)
        configObj.put("max", control.config.max)
        configObj.put("curve", control.config.curve.name)
        
        control.config.buttonIndex?.let { configObj.put("buttonIndex", it) }
        configObj.put("sliderOrientation", control.config.sliderOrientation.name)
        configObj.put("shifterType", control.config.shifterType.name)
        
        obj.put("config", configObj)
        
        return obj
    }
    
    companion object {
        const val DEFAULT_LAYOUT_ID = "ets2_ats_default"
        
        /**
         * Create the default ETS2/ATS layout.
         */
        fun createDefaultLayout(): ControllerLayout {
            val controls = listOf(
                // Steering wheel - left side (45% width)
                LayoutControl(
                    controlType = ControlKind.STEERING,
                    x = 0.025f,
                    y = 0.2f,
                    width = 0.45f,
                    height = 0.6f,
                    config = ControlConfiguration(
                        id = "steering",
                        label = "Steering",
                        outputAxis = "X",
                        deadzone = 0.02f,
                        min = -1f,
                        max = 1f,
                        curve = ResponseCurve.LINEAR
                    )
                ),
                // Throttle slider - right side
                LayoutControl(
                    controlType = ControlKind.SLIDER,
                    x = 0.68f,
                    y = 0.1f,
                    width = 0.12f,
                    height = 0.8f,
                    config = ControlConfiguration(
                        id = "throttle",
                        label = "Throttle",
                        outputAxis = "Y",
                        deadzone = 0.05f,
                        min = 0f,
                        max = 1f,
                        curve = ResponseCurve.LINEAR,
                        sliderOrientation = SliderOrientation.VERTICAL
                    )
                ),
                // Brake slider - right side
                LayoutControl(
                    controlType = ControlKind.SLIDER,
                    x = 0.85f,
                    y = 0.1f,
                    width = 0.12f,
                    height = 0.8f,
                    config = ControlConfiguration(
                        id = "brake",
                        label = "Brake",
                        outputAxis = "Z",
                        deadzone = 0.05f,
                        min = 0f,
                        max = 1f,
                        curve = ResponseCurve.LINEAR,
                        sliderOrientation = SliderOrientation.VERTICAL
                    )
                ),
                // Horn button - top center
                LayoutControl(
                    controlType = ControlKind.BUTTON_HOLD,
                    x = 0.50f,
                    y = 0.05f,
                    width = 0.10f,
                    height = 0.10f,
                    config = ControlConfiguration(
                        id = "horn",
                        label = "Horn",
                        buttonIndex = 1
                    )
                ),
                // Lights toggle - top center
                LayoutControl(
                    controlType = ControlKind.BUTTON_TOGGLE,
                    x = 0.62f,
                    y = 0.05f,
                    width = 0.10f,
                    height = 0.10f,
                    config = ControlConfiguration(
                        id = "lights",
                        label = "Lights",
                        buttonIndex = 2
                    )
                )
            )
            
            return ControllerLayout(
                id = DEFAULT_LAYOUT_ID,
                name = "ETS2/ATS",
                isBuiltIn = true,
                controls = controls
            )
        }
    }
}
