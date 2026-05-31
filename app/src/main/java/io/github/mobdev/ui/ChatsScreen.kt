package io.github.mobdev.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mobdev.R
import io.github.mobdev.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    viewModel: ChatViewModel,
    onChatClick: (String) -> Unit,
    onLogout: () -> Unit
) {
    val channels by viewModel.channels.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadChannels()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chats_title)) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.logout)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
            if (isLoading && channels.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(channels) { channel ->
                        ListItem(
                            headlineContent = { Text(channel) },
                            modifier = Modifier
                                .clickable { onChatClick(channel) }
                                .fillMaxWidth(),
                            colors = if (channel == selectedChannel)
                                ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            else ListItemDefaults.colors()
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}