package com.example.safesense.sensor.alert

import com.example.safesense.domain.model.IncidentType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AlertMessageBuilder
 *
 * Single responsibility: produce the correct SMS string in the correct language,
 * always under 155 characters. Nothing else.
 */
object AlertMessageBuilder {

    // Maximum allowed characters for a single-part SMS
    private const val SMS_MAX_CHARS = 155

    /**
     * Builds the emergency SMS string.
     *
     * @param userName      The user's display name (from UserPreferences)
     * @param incidentType  The type of incident detected (FALL, COLLISION, SHAKE, MANUAL)
     * @param latitude      GPS latitude as a Double
     * @param longitude     GPS longitude as a Double
     * @param language      Reserved for future use (defaulting all to English for now)
     * @return              A formatted SMS string, guaranteed under 155 characters
     */
    fun build(
        userName: String,
        incidentType: IncidentType,
        latitude: Double,
        longitude: Double,
        language: String
    ): String {

        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val lat = "%.4f".format(latitude)
        val lng = "%.4f".format(longitude)
        val mapUrl = "https://maps.google.com/?q=$lat,$lng"

        // Forcing English labels for all alerts as requested
        val typeLabel = getIncidentLabel(incidentType)

        // Using English template for all alerts
        val message = buildEnglish(userName, typeLabel, time, mapUrl)

        check(message.length <= SMS_MAX_CHARS) {
            "SMS message exceeds $SMS_MAX_CHARS characters! Length: ${message.length}"
        }

        return message
    }

    private fun buildEnglish(
        name: String,
        typeLabel: String,
        time: String,
        mapUrl: String
    ): String {
        return "[SafeSense] EMERGENCY\nFrom: $name\nType: $typeLabel\nTime: $time\nLocation: $mapUrl"
    }

    private fun getIncidentLabel(incidentType: IncidentType): String {
        return when (incidentType) {
            IncidentType.FALL      -> "Fall Detected"
            IncidentType.COLLISION -> "Collision Detected"
            IncidentType.SHAKE     -> "Distress Signal"
            IncidentType.MANUAL    -> "Manual Alert"
            IncidentType.SOUND     -> "Distress Sound"
            IncidentType.WALK_MODE -> "Walk Mode Completed"
        }
    }
}
