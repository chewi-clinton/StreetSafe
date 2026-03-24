package com.example.safesense.ui.contacts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safesense.domain.model.EmergencyContact
import com.example.safesense.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// @HiltViewModel tells Hilt to build this ViewModel and inject ContactRepository automatically
@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val repository: ContactRepository
) : ViewModel() {

    // stateIn converts the Flow from the repository into a StateFlow.
    // StateFlow always holds the latest value — the UI reads from it directly.
    // SharingStarted.WhileSubscribed(5000) keeps the flow alive for 5 seconds after
    // the UI disappears (e.g. screen rotation) so it doesn't restart unnecessarily.
    val contacts: StateFlow<List<EmergencyContact>> = repository
        .getAllContacts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError

    fun updateContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.updateContact(contact)
        }
    }

    fun importFromPhonebook(uri: android.net.Uri, context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val contactId = uri.lastPathSegment ?: return@launch

                val nameCursor = context.contentResolver.query(
                    uri, null, null, null, null
                )
                var displayName = ""
                nameCursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(
                            android.provider.ContactsContract.Contacts.DISPLAY_NAME
                        )
                        if (nameIndex >= 0) displayName = it.getString(nameIndex) ?: ""
                    }
                }
                if (displayName.isEmpty()) return@launch

                val phoneCursor = context.contentResolver.query(
                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )
                var phoneNumber = "+237"
                phoneCursor?.use {
                    if (it.moveToFirst()) {
                        val numberIndex = it.getColumnIndex(
                            android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
                        )
                        if (numberIndex >= 0) {
                            val raw = it.getString(numberIndex)
                                ?.replace(" ", "")
                                ?.replace("-", "")
                                ?.replace("(", "")
                                ?.replace(")", "")
                                ?: ""
                            phoneNumber = when {
                                raw.startsWith("+237") -> raw
                                raw.startsWith("237") -> "+$raw"
                                raw.startsWith("6") || raw.startsWith("2") -> "+237$raw"
                                else -> raw.ifEmpty { "+237" }
                            }
                        }
                    }
                }

                repository.insertContact(
                    EmergencyContact(
                        name = displayName,
                        phoneNumber = phoneNumber,
                        relationship = "",
                        isActive = true
                    )
                )
            } catch (e: SecurityException) {
                _importError.value = "Contact permission denied. Please grant it in Settings."
            }
        }
    }

    // viewModelScope.launch runs the suspend function on a background thread safely
    fun deleteContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
        }
    }

    // Flips isActive and saves the updated contact back to the database
    fun toggleActive(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.updateContact(contact.copy(isActive = !contact.isActive))
        }
    }

    // Used by AddEditContactScreen to save a new contact
    fun insertContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.insertContact(contact)
        }
    }

    fun clearImportError() {
        _importError.value = null
    }
}
