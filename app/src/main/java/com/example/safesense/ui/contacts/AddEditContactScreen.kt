package com.example.safesense.ui.contacts

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val PrimaryRed = Color(0xFFD32F2F)
private val White = Color(0xFFFFFFFF)
private val Gray100 = Color(0xFFF5F5F5)
private val Gray200 = Color(0xFFEEEEEE)
private val Gray400 = Color(0xFFBDBDBD)
private val Gray600 = Color(0xFF757575)
private val Gray900 = Color(0xFF212121)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactScreen(
    contactId: Int? = null,
    onSaveComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val existingContact = remember(contactId, contacts) {
        if (contactId != null) contacts.find { it.id == contactId } else null
    }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+237") }
    var relationship by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(existingContact) {
        if (existingContact != null && !initialized) {
            name = existingContact.name
            phone = existingContact.phoneNumber
            relationship = existingContact.relationship
            initialized = true
        }
        if (existingContact == null && contactId == null && !initialized) {
            initialized = true
        }
    }

    val focusManager = LocalFocusManager.current

    val initials = remember(name) {
        name.trim()
            .split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
    }

    val isPhoneValid = phone.startsWith("+237") && phone.length >= 13
    val isSaveEnabled = name.trim().isNotEmpty() && isPhoneValid

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PrimaryRed,
        unfocusedBorderColor = Gray200,
        focusedLabelColor = PrimaryRed,
        unfocusedLabelColor = Gray600,
        focusedLeadingIconColor = PrimaryRed,
        unfocusedLeadingIconColor = Gray600,
        cursorColor = PrimaryRed,
        focusedTextColor = Gray900,
        unfocusedTextColor = Gray900,
        focusedContainerColor = White,
        unfocusedContainerColor = White
    )

    Scaffold(
        containerColor = White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (contactId != null) "Edit Contact" else "New Contact",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Gray900
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Gray900
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White)
            ) {
                HorizontalDivider(color = Gray200, thickness = 1.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = {
                            val contact = com.example.safesense.domain.model.EmergencyContact(
                                id = existingContact?.id ?: 0,
                                name = name.trim(),
                                phoneNumber = phone.trim(),
                                relationship = relationship.trim(),
                                isActive = existingContact?.isActive ?: true
                            )
                            if (existingContact != null) {
                                viewModel.updateContact(contact)
                            } else {
                                viewModel.insertContact(contact)
                            }
                            onSaveComplete()
                        },
                        enabled = isSaveEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryRed,
                            disabledContainerColor = Gray400,
                            contentColor = White,
                            disabledContentColor = White
                        )
                    ) {
                        Text(
                            text = "Save Contact",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(if (initials.isNotEmpty()) PrimaryRed else Gray100),
                contentAlignment = Alignment.Center
            ) {
                if (initials.isNotEmpty()) {
                    Text(
                        text = initials,
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = Gray400,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Person, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { newValue ->
                    if (newValue.startsWith("+237")) {
                        phone = newValue
                    }
                },
                label = { Text("Phone Number") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Phone, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                isError = phone.length > 4 && !isPhoneValid,
                supportingText = {
                    if (phone.length > 4 && !isPhoneValid) {
                        Text(
                            text = "Must start with +237 and have at least 9 digits after it",
                            color = PrimaryRed,
                            fontSize = 12.sp
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = relationship,
                onValueChange = { relationship = it },
                label = { Text("Relationship (e.g. Wife, Brother)") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.FavoriteBorder, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}