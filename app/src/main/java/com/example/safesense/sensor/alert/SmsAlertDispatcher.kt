package com.example.safesense.sensor.alert

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.os.Build
import com.example.safesense.domain.model.EmergencyContact
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SmsAlertDispatcher
 *
 * Single responsibility: take a built SMS string and a list of contacts,
 * and send the message to each contact using sendMultipartTextMessage.
 *
 * WHY sendMultipartTextMessage and NEVER sendTextMessage?
 * sendTextMessage silently truncates messages that exceed 160 characters.
 * On some carriers in Cameroon it drops the message entirely.
 * sendMultipartTextMessage handles splitting automatically AND attaches
 * delivery receipts so we know whether the SMS actually arrived.
 * Even if our message is under 155 chars, we always use the multipart API
 * because it is the only way to attach both a sent PendingIntent AND a
 * delivered PendingIntent per contact.
 *
 * WHY GPS from cache and never from a live request?
 * At the moment of an emergency, the user may be unconscious. A live GPS
 * request can take 30–90 seconds to resolve — or never resolve indoors.
 * GPSTracker keeps the last known fix in memory, updated continuously while
 * the Foreground Service is running. We read that cached value instantly.
 *
 * WHY PendingIntent per contact?
 * Each contact gets its own unique PendingIntent with a unique request code.
 * This is required so SmsDeliveryReceiver can identify WHICH contact's SMS
 * was sent or delivered and update the correct row in the Room database.
 * If all contacts shared one PendingIntent, we would not know which one
 * succeeded or failed.
 */
@Singleton
class SmsAlertDispatcher @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Sends the SMS message to every active contact in the list.
     *
     * @param contacts   List of active EmergencyContact objects from the database
     * @param message    The fully built SMS string from AlertMessageBuilder
     * @param incidentId The Room database ID of the incident — stored in the
     *                   PendingIntent so SmsDeliveryReceiver can update the
     *                   correct row when the receipt arrives
     */
    fun dispatch(
        contacts: List<EmergencyContact>,
        message: String,
        incidentId: Long
    ) {
        // Get the correct SmsManager for the device's Android version.
        // On Android 12+ (API 31+), SmsManager.getDefault() is deprecated.
        // createForDefaultSmsSubscriptionId() is the correct modern API.
        val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        // Split the message into parts. For messages under 155 characters this
        // will always return a list with exactly one part. We still use the
        // multipart method because it is the only way to attach PendingIntents.
        val parts = smsManager.divideMessage(message)

        contacts.forEachIndexed { index, contact ->

            // Each contact gets a unique request code so SmsDeliveryReceiver
            // can match the broadcast back to this specific contact + incident.
            // We combine incidentId and contact index to guarantee uniqueness.
            val requestCodeSent      = (incidentId * 1000 + index * 2).toInt()
            val requestCodeDelivered = (incidentId * 1000 + index * 2 + 1).toInt()

            // SENT PendingIntent — fired when the SMS leaves the device.
            // This does NOT mean the recipient received it — only that the
            // modem handed it to the carrier network.
            val sentIntent = Intent(SmsDeliveryReceiver.ACTION_SMS_SENT).apply {
                putExtra(SmsDeliveryReceiver.EXTRA_INCIDENT_ID, incidentId)
                putExtra(SmsDeliveryReceiver.EXTRA_CONTACT_ID, contact.id)
                setPackage(context.packageName) // Security: restrict to our app only
            }
            val sentPendingIntent = PendingIntent.getBroadcast(
                context,
                requestCodeSent,
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // DELIVERED PendingIntent — fired when the carrier confirms the
            // recipient's handset actually received the SMS.
            // This is the definitive proof of delivery we store in the database.
            val deliveredIntent = Intent(SmsDeliveryReceiver.ACTION_SMS_DELIVERED).apply {
                putExtra(SmsDeliveryReceiver.EXTRA_INCIDENT_ID, incidentId)
                putExtra(SmsDeliveryReceiver.EXTRA_CONTACT_ID, contact.id)
                setPackage(context.packageName)
            }
            val deliveredPendingIntent = PendingIntent.getBroadcast(
                context,
                requestCodeDelivered,
                deliveredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build matching lists of PendingIntents — one per message part.
            // Since our message is always under 155 chars, parts.size == 1,
            // so these lists will each contain exactly one PendingIntent.
            val sentIntents      = ArrayList(parts.map { sentPendingIntent })
            val deliveredIntents = ArrayList(parts.map { deliveredPendingIntent })

            // Send the SMS. This is the only send call in the entire project.
            // phoneNumber must include +237 country code (enforced at contact creation).
            smsManager.sendMultipartTextMessage(
                contact.phoneNumber,  // e.g. "+237612345678"
                null,                 // null = use default SMSC (correct for Cameroon carriers)
                parts,
                sentIntents,
                deliveredIntents
            )
        }
    }
}