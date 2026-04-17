package io.github.mobdev

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsApp() {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    var selectedContact by remember { mutableStateOf<Contact?>(null) }

    val contacts = remember(hasPermission) {
        if (hasPermission) context.fetchAllContacts() else emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedContact != null)
                            stringResource(R.string.contact_details)
                        else
                            stringResource(R.string.contacts_title)
                    )
                },
                navigationIcon = {
                    if (selectedContact != null) {
                        IconButton(onClick = { selectedContact = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
            when {
                selectedContact != null -> {
                    ContactDetailScreen(contact = selectedContact!!)
                }
                hasPermission -> {
                    ContactListScreen(
                        contacts = contacts,
                        onContactClick = { selectedContact = it }
                    )
                }
                else -> {
                    NoPermissionScreen(
                        onRequestPermission = {
                            launcher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NoPermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.permission_required),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text(stringResource(R.string.request_permission))
        }
    }
}

@Composable
fun ContactListScreen(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(contacts) { contact ->
            ListItem(
                headlineContent = {
                    Text(contact.name ?: stringResource(R.string.no_name))
                },
                modifier = Modifier
                    .clickable { onContactClick(contact) }
                    .fillMaxWidth()
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun ContactDetailScreen(contact: Contact) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = contact.name ?: stringResource(R.string.no_name),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.phone_label) + " " +
                    (contact.phoneNumber ?: stringResource(R.string.no_phone)),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.email_label) + " " +
                    (contact.email ?: stringResource(R.string.no_email)),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}