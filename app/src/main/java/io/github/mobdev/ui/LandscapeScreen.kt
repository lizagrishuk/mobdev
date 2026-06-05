package io.github.mobdev.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.mobdev.R
import io.github.mobdev.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandscapeScreen(
    viewModel: ChatViewModel,
    onImageClick: (String) -> Unit,
    onLogout: () -> Unit
) {
    val channels by viewModel.channels.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val error by viewModel.error.collectAsState()

// диалог ошибки
    error?.let { msg ->
        if (msg != "401") {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text(stringResource(R.string.error_title)) },
                text = { Text(msg) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadChannels()
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // левая панель - список чатов
        Column(
            modifier = Modifier
                .width(250.dp)
                .fillMaxHeight()
        ) {
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
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(channels) { channel ->
                    ListItem(
                        headlineContent = { Text(channel) },
                        modifier = Modifier
                            .clickable {
                                viewModel.selectChannel(channel)
                            }
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

        VerticalDivider()

        // правая панель - сообщения или подсказка
        if (selectedChannel == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.select_chat),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(selectedChannel ?: "") }
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading && messages.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            reverseLayout = true
                        ) {
                            items(messages.reversed()) { message ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = message.from,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        message.data.Text?.let {
                                            Text(text = it.text)
                                        }
                                        message.data.Image?.link?.let { link ->
                                            AsyncImage(
                                                model = "https://faerytea.name/thumb/$link",
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(150.dp)
                                                    .clickable { onImageClick(link) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.message_hint)) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send)
                        )
                    }
                }
            }
        }
    }
}