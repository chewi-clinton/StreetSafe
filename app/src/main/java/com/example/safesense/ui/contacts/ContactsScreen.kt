package com.example.safesense.ui.contacts

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safesense.domain.model.EmergencyContact
import com.example.safesense.ui.components.SafeSenseBottomNavBar

private val PrimaryRed = Color(0xFFD32F2F)
private val RedLight = Color(0xFFFFEBEE)
private val White = Color(0xFFFFFFFF)
private val Gray100 = Color(0xFFF5F5F5)
private val Gray200 = Color(0xFFEEEEEE)
private val Gray400 = Color(0xFFBDBDBD)
private val Gray600 = Color(0xFF757575)
private val Gray800 = Color(0xFF424242)
private val Gray900 = Color(0xFF212121)

private const val MAX_CONTACTS = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToManualAdd: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val importError by viewModel.importError.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<EmergencyContact?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let { viewModel.importFromPhonebook(it, context) }
    }

    val readContactsPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pickContactLauncher.launch(null)
        }
    }

    LaunchedEffect(importError) {
        importError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportError()
        }
    }

    if (showAddSheet) {
        AddContactBottomSheet(
            onDismiss = { showAddSheet = false },
            onChooseFromPhonebook = {
                showAddSheet = false
                readContactsPermission.launch(android.Manifest.permission.READ_CONTACTS)
            },
            onAddManually = {
                showAddSheet = false
                onNavigateToManualAdd()
            }
        )
    }

    contactToDelete?.let { contact ->
        DeleteConfirmDialog(
            contactName = contact.name,
            onConfirm = {
                viewModel.deleteContact(contact)
                contactToDelete = null
            },
            onDismiss = { contactToDelete = null }
        )
    }

    Scaffold(
        containerColor = White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            SafeSenseBottomNavBar(
                selectedIndex = 2,
                onItemSelected = { index ->
                    when (index) {
                        0 -> onNavigateToHome()
                        1 -> onNavigateToHistory()
                        3 -> onNavigateToSettings()
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item {
                ContactsHeader(onAddClick = { showAddSheet = true })
            }
            item {
                QuotaCard(current = contacts.size, max = MAX_CONTACTS)
            }
            item {
                InfoText()
            }
            items(items = contacts, key = { it.id }) { contact ->
                ContactItem(
                    contact = contact,
                    isPrimary = contacts.indexOf(contact) == 0,
                    onEdit = { onNavigateToEdit(contact.id) },
                    onDelete = { contactToDelete = contact }
                )
            }
            if (contacts.size < MAX_CONTACTS) {
                item {
                    AddPlaceholder(
                        remaining = MAX_CONTACTS - contacts.size,
                        onAddClick = { showAddSheet = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactsHeader(onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Emergency Contacts",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Gray900
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryRed)
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add contact",
                tint = White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun QuotaCard(current: Int, max: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = RedLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Contacts added",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryRed,
                    fontSize = 14.sp
                )
                Text(
                    text = "$current / $max",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryRed,
                    fontSize = 14.sp
                )
            }
            LinearProgressIndicator(
                progress = { current.toFloat() / max.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50)),
                color = PrimaryRed,
                trackColor = White
            )
        }
    }
}

@Composable
private fun InfoText() {
    Text(
        text = "Alerts are sent to all contacts simultaneously when an incident is confirmed.",
        fontSize = 14.sp,
        color = Gray600,
        lineHeight = 20.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun ContactItem(
    contact: EmergencyContact,
    isPrimary: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val initials = contact.name
        .trim()
        .split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

    val avatarColor = if (isPrimary) PrimaryRed else Gray800
    val statusLabel = if (isPrimary) "Primary contact" else "Trusted contact"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Gray200),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Gray900
                )
                Text(
                    text = contact.phoneNumber,
                    fontSize = 13.sp,
                    color = Gray600
                )
                Text(
                    text = "${contact.relationship} · $statusLabel",
                    fontSize = 12.sp,
                    color = Gray400
                )
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit contact",
                        tint = Gray600
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete contact",
                        tint = Gray600
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPlaceholder(remaining: Int, onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Gray100),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.PersonAdd,
                contentDescription = null,
                tint = Gray400,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "Add up to $remaining more contact${if (remaining > 1) "s" else ""}.\nMore contacts = more protection.",
                fontSize = 14.sp,
                color = Gray600,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Text(
                text = "+ Add another contact",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryRed,
                modifier = Modifier.clickable { onAddClick() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddContactBottomSheet(
    onDismiss: () -> Unit,
    onChooseFromPhonebook: () -> Unit,
    onAddManually: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Add Emergency Contact",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Gray900,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedButton(
                onClick = onChooseFromPhonebook,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Gray200),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gray900)
            ) {
                Text(
                    text = "Choose from Phonebook",
                    modifier = Modifier.padding(vertical = 6.dp),
                    fontSize = 15.sp
                )
            }

            Button(
                onClick = onAddManually,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
            ) {
                Text(
                    text = "Add Manually",
                    modifier = Modifier.padding(vertical = 6.dp),
                    fontSize = 15.sp,
                    color = White
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    contactName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Remove $contactName?",
                fontWeight = FontWeight.Bold,
                color = Gray900
            )
        },
        text = {
            Text(
                text = "This contact will no longer receive emergency alerts.",
                color = Gray600,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Remove", color = PrimaryRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Gray600)
            }
        }
    )
}
