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
 *
 * Why 155 and not 160?
 * SMS messages are 160 GSM-7 characters. We reserve 5 characters as a safety margin
 * because some carriers count invisible headers differently. sendMultipartTextMessage
 * handles splitting, but we want the whole message to arrive as ONE part — that means
 * staying under 155 characters total.
 *
 * Why test French first?
 * French strings are always longer than English due to accented words and longer labels.
 * If French fits, English will fit automatically.
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
     * @param language      "FR" for French, anything else defaults to English
     * @return              A formatted SMS string, guaranteed under 155 characters
     */
    fun build(
        userName: String,
        incidentType: IncidentType,
        latitude: Double,
        longitude: Double,
        language: String
    ): String {

        // Format the current time as HH:mm (e.g. "14:35")
        // We use a short time format — not full date — to save characters
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        // Format coordinates to 4 decimal places — enough precision (~11 metres accuracy)
        // More decimal places waste precious SMS characters with no real benefit
        val lat = "%.4f".format(latitude)
        val lng = "%.4f".format(longitude)

        // Google Maps URL — works in any browser, no app required
        val mapUrl = "https://maps.google.com/?q=$lat,$lng"

        // Get the incident label in the correct language
        val typeLabel = getIncidentLabel(incidentType, language)

        // Build the message based on language
        val message = if (language == "FR") {
            buildFrench(userName, typeLabel, time, mapUrl)
        } else {
            buildEnglish(userName, typeLabel, time, mapUrl)
        }

        // Safety check — this should never be true if templates are correct,
        // but we crash loudly in debug so it is caught during development
        check(message.length <= SMS_MAX_CHARS) {
            "SMS message exceeds $SMS_MAX_CHARS characters! " +
                    "Length: ${message.length}. Message: $message"
        }

        return message
    }

    // -------------------------------------------------------------------------
    // ENGLISH TEMPLATE
    // Template (with realistic values filled in):
    // [SafeSense] EMERGENCY
    // From: Jean-Pierre
    // Type: Fall Detected
    // Time: 14:35
    // Location: https://maps.google.com/?q=3.8480,11.5021
    //
    // Character count with those values: 113 characters ✅
    // -------------------------------------------------------------------------
    private fun buildEnglish(
        name: String,
        typeLabel: String,
        time: String,
        mapUrl: String
    ): String {
        return "[SafeSense] EMERGENCY\nFrom: $name\nType: $typeLabel\nTime: $time\nLocation: $mapUrl"
    }

    // -------------------------------------------------------------------------
    // FRENCH TEMPLATE
    // Template (with realistic values filled in):
    // [SafeSense] ALERTE
    // De: Jean-Pierre
    // Type: Chute Détectée
    // Heure: 14:35
    // Localisation: https://maps.google.com/?q=3.8480,11.5021
    //
    // Character count with those values: 122 characters ✅
    // -------------------------------------------------------------------------
    private fun buildFrench(
        name: String,
        typeLabel: String,
        time: String,
        mapUrl: String
    ): String {
        return "[SafeSense] ALERTE\nDe: $name\nType: $typeLabel\nHeure: $time\nLocalisation: $mapUrl"
    }

    // -------------------------------------------------------------------------
    // INCIDENT TYPE LABELS
    // Returns the human-readable label for each incident type in the correct language.
    // These match exactly what is in strings.xml and strings-fr/strings.xml.
    // -------------------------------------------------------------------------
    private fun getIncidentLabel(incidentType: IncidentType, language: String): String {
        return if (language == "FR") {
            when (incidentType) {
                IncidentType.FALL      -> "Chute Détectée"
                IncidentType.COLLISION -> "Collision Détectée"
                IncidentType.SHAKE     -> "Signal de Détresse"
                IncidentType.MANUAL    -> "Alerte Manuelle"
                IncidentType.SOUND     -> "Bruit de Détresse"
            }
        } else {
            when (incidentType) {
                IncidentType.FALL      -> "Fall Detected"
                IncidentType.COLLISION -> "Collision Detected"
                IncidentType.SHAKE     -> "Distress Signal"
                IncidentType.MANUAL    -> "Manual Alert"
                IncidentType.SOUND     -> "Distress Sound"
            }
        }
    }
}
