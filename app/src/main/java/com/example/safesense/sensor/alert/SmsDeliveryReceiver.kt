package com.example.safesense.sensor.alert

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.safesense.domain.model.AlertStatus
import com.example.safesense.domain.repository.IncidentRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SmsDeliveryReceiver
 *
 * A BroadcastReceiver that listens for the two PendingIntents attached by
 * SmsAlertDispatcher: one for SENT, one for DELIVERED.
 *
 * When each fires, this receiver reads the incidentId and contactId from
 * the Intent extras, then updates the correct row in the Room incidents table.
 */
@AndroidEntryPoint
class SmsDeliveryReceiver : BroadcastReceiver() {

    // Hilt injects the repository so we can update the Room database
    @Inject
    lateinit var incidentRepository: IncidentRepository

    override fun onReceive(context: Context, intent: Intent) {

        // Read the incident and contact IDs that SmsAlertDispatcher packed
        // into the PendingIntent when the SMS was originally dispatched
        val incidentId = intent.getLongExtra(EXTRA_INCIDENT_ID, -1L)
        val contactId  = intent.getLongExtra(EXTRA_CONTACT_ID, -1L)

        // If either ID is missing, something went wrong in dispatcher — do nothing
        if (incidentId == -1L || contactId == -1L) return

        when (intent.action) {

            ACTION_SMS_SENT -> {
                // resultCode tells us whether the SMS left the device successfully
                val status = when (resultCode) {
                    Activity.RESULT_OK -> AlertStatus.SENT
                    else               -> AlertStatus.FAILED
                }
                // Update the alertStatus column in the incidents table
                CoroutineScope(Dispatchers.IO).launch {
                    val incident = incidentRepository.getIncidentById(incidentId)
                    incident?.let {
                        incidentRepository.updateIncident(it.copy(alertStatus = status))
                    }
                }
            }

            ACTION_SMS_DELIVERED -> {
                // resultCode tells us whether the recipient's handset confirmed receipt
                // For delivery, we just update the contactsAlerted count if OK
                if (resultCode == Activity.RESULT_OK) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val incident = incidentRepository.getIncidentById(incidentId)
                        incident?.let {
                            incidentRepository.updateIncident(
                                it.copy(contactsAlerted = it.contactsAlerted + 1)
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_SMS_SENT      = "com.example.safesense.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.example.safesense.SMS_DELIVERED"
        const val EXTRA_INCIDENT_ID = "extra_incident_id"
        const val EXTRA_CONTACT_ID  = "extra_contact_id"
    }
}
