package com.usb.drivingremote.utils

import com.usb.drivingremote.data.models.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Utility for serializing and deserializing controller layouts to/from JSON.
 * 
 * File format (.dr):
 * {
 *   "version": "1.0",
 *   "layout": {
 *     "name": "Layout Name",
 *     "controls": [...]
 *   }
 * }
 */
object LayoutSerializer {
    
    private const val VERSION = "1.0"
    
    /**
     * Serialize a layout to JSON string for export.
     */
    fun serialize(layout: ControllerLayout): String {
        val json = JSONObject()
        json.put("version", VERSION)
        
        val layoutObj = JSONObject()
        layoutObj.put("name", layout.name)
        
        val controlsArray = JSONArray()
        layout.controls.forEach { control ->
            val controlObj = JSONObject()
            controlObj.put("type", control.controlType.name)
            controlObj.put("x", control.x)
            controlObj.put("y", control.y)
            controlObj.put("width", control.width)
            controlObj.put("height", control.height)
            
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
            
            controlObj.put("config", configObj)
            controlsArray.put(controlObj)
        }
        
        layoutObj.put("controls", controlsArray)
        json.put("layout", layoutObj)
        
        return json.toString(2) // Pretty print with 2-space indent
    }
    
    /**
     * Deserialize a layout from JSON string (from import).
     * 
     * @return Pair of layout name and list of controls (without ID, to be generated)
     * @throws IllegalArgumentException if JSON is invalid
     */
    fun deserialize(jsonString: String): Pair<String, List<LayoutControl>> {
        try {
            val json = JSONObject(jsonString)
            val version = json.optString("version", "1.0")
            
            // Version check (for future compatibility)
            if (version != VERSION) {
                throw IllegalArgumentException("Unsupported layout version: $version")
            }
            
            val layoutObj = json.getJSONObject("layout")
            val name = layoutObj.getString("name")
            val controlsArray = layoutObj.getJSONArray("controls")
            
            val controls = mutableListOf<LayoutControl>()
            for (i in 0 until controlsArray.length()) {
                val controlObj = controlsArray.getJSONObject(i)
                
                val type = ControlKind.valueOf(controlObj.getString("type"))
                val x = controlObj.getDouble("x").toFloat()
                val y = controlObj.getDouble("y").toFloat()
                val width = controlObj.getDouble("width").toFloat()
                val height = controlObj.getDouble("height").toFloat()
                
                val configObj = controlObj.getJSONObject("config")
                val config = ControlConfiguration(
                    id = configObj.getString("id"),
                    label = configObj.optString("label", ""),
                    outputAxis = configObj.optString("outputAxis", "X"),
                    deadzone = configObj.optDouble("deadzone", 0.05).toFloat(),
                    min = configObj.optDouble("min", -1.0).toFloat(),
                    max = configObj.optDouble("max", 1.0).toFloat(),
                    curve = ResponseCurve.valueOf(
                        configObj.optString("curve", "LINEAR")
                    ),
                    buttonIndex = if (configObj.has("buttonIndex")) 
                        configObj.getInt("buttonIndex") else null,
                    sliderOrientation = SliderOrientation.valueOf(
                        configObj.optString("sliderOrientation", "VERTICAL")
                    ),
                    shifterType = ShifterType.valueOf(
                        configObj.optString("shifterType", "STANDARD_6_SPEED")
                    )
                )
                
                controls.add(
                    LayoutControl(
                        controlType = type,
                        x = x,
                        y = y,
                        width = width,
                        height = height,
                        config = config
                    )
                )
            }
            
            return Pair(name, controls)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid layout file: ${e.message}", e)
        }
    }
}
