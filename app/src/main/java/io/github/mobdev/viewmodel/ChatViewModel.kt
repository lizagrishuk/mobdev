package io.github.mobdev.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.mobdev.db.AppDatabase
import io.github.mobdev.db.MessageEntity
import io.github.mobdev.network.Message
import io.github.mobdev.network.MessageData
import io.github.mobdev.network.RetrofitClient
import io.github.mobdev.network.SendMessageRequest
import io.github.mobdev.network.TextData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.messageDao()
    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    private val _username = MutableStateFlow<String>("")
    val username: StateFlow<String> = _username

    private val _channels = MutableStateFlow<List<String>>(emptyList())
    val channels: StateFlow<List<String>> = _channels

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _selectedChannel = MutableStateFlow<String?>(null)
    val selectedChannel: StateFlow<String?> = _selectedChannel

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _hasMoreMessages = MutableStateFlow(true)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun login(name: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.api.login(
                    io.github.mobdev.network.LoginRequest(name, password)
                )
                when (response.code()) {
                    200 -> {
                        val token = response.body()?.string()?.trim()
                        if (token != null) {
                            _token.value = token
                            _username.value = name
                            _error.value = null
                            onSuccess()
                        }
                    }
                    401 -> _error.value = "Неверный логин или пароль"
                    else -> _error.value = "Ошибка сервера: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка подключения"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadChannels() {
        if (_channels.value.isNotEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            if (isNetworkAvailable()) {
                try {
                    val currentToken = _token.value ?: return@launch
                    _channels.value = RetrofitClient.api.getChannels(currentToken)
                    _isOnline.value = true
                    _error.value = null
                } catch (e: retrofit2.HttpException) {
                    if (e.code() == 401) _error.value = "401"
                    else _error.value = "Ошибка загрузки каналов"
                } catch (e: Exception) {
                    _isOnline.value = false
                    _error.value = "Нет подключения к сети"
                }
            } else {
                _isOnline.value = false
                _error.value = "Нет подключения к сети"
            }
            _isLoading.value = false
        }
    }

    fun selectChannel(channel: String) {
        if (_selectedChannel.value == channel && _messages.value.isNotEmpty()) return
        _selectedChannel.value = channel
        _messages.value = emptyList()
        _hasMoreMessages.value = true
        loadMessages(channel)
    }

    fun loadMessages(channel: String) {
        if (_messages.value.isNotEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true

            val cached = dao.getMessages(channel)
            if (cached.isNotEmpty()) {
                _messages.value = cached.map { it.toMessage() }
            }

            if (isNetworkAvailable()) {
                try {
                    val currentToken = _token.value ?: return@launch
                    var lastId = "0"
                    var allMessages = emptyList<Message>()
                    while (true) {
                        val batch = RetrofitClient.api.getMessages(
                            currentToken, channel, limit = 20, lastKnownId = lastId
                        )
                        if (batch.isEmpty()) break
                        allMessages = allMessages + batch
                        lastId = batch.maxOf { it.id }.toString()
                        if (batch.size < 20) break
                    }
                    if (allMessages.isNotEmpty()) {
                        _messages.value = allMessages
                        dao.insertMessages(allMessages.map { it.toEntity(channel) })
                    }
                    _isOnline.value = true
                    _error.value = null
                } catch (e: retrofit2.HttpException) {
                    if (e.code() == 401) _error.value = "401"
                    else _error.value = "Ошибка загрузки сообщений"
                } catch (e: Exception) {
                    _isOnline.value = false
                    if (_messages.value.isEmpty()) {
                        _error.value = "Нет подключения, показаны кэшированные данные"
                    }
                }
            } else {
                _isOnline.value = false
                if (_messages.value.isEmpty()) {
                    _error.value = "Нет подключения к сети"
                }
            }
            _isLoading.value = false
        }
    }

    fun loadMoreMessages() {
        if (_isLoadingMore.value || !_hasMoreMessages.value) return
        val currentChannel = _selectedChannel.value ?: return
        if (!isNetworkAvailable()) return
        val lastId = _messages.value.maxOfOrNull { it.id } ?: return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val currentToken = _token.value ?: return@launch
                val result = RetrofitClient.api.getMessages(
                    currentToken, currentChannel, limit = 20, lastKnownId = lastId.toString()
                )
                if (result.isEmpty()) {
                    _hasMoreMessages.value = false
                } else {
                    _messages.value = _messages.value + result
                    _hasMoreMessages.value = result.size >= 20
                    dao.insertMessages(result.map { it.toEntity(currentChannel) })
                }
            } catch (e: Exception) {
                _isOnline.value = false
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun sendMessage(text: String) {
        if (!isNetworkAvailable()) {
            _error.value = "Нет подключения — отправка недоступна"
            return
        }
        val currentToken = _token.value ?: return
        val currentChannel = _selectedChannel.value ?: return
        val currentUsername = _username.value
        viewModelScope.launch {
            try {
                RetrofitClient.api.sendMessage(
                    currentToken,
                    SendMessageRequest(
                        from = currentUsername,
                        to = currentChannel,
                        data = MessageData(Text = TextData(text))
                    )
                )
                var lastId = "0"
                var allMessages = emptyList<Message>()
                while (true) {
                    val batch = RetrofitClient.api.getMessages(
                        currentToken, currentChannel, limit = 20, lastKnownId = lastId
                    )
                    if (batch.isEmpty()) break
                    allMessages = allMessages + batch
                    lastId = batch.maxOf { it.id }.toString()
                    if (batch.size < 20) break
                }
                if (allMessages.isNotEmpty()) {
                    _messages.value = allMessages
                    dao.insertMessages(allMessages.map { it.toEntity(currentChannel) })
                }
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401) _error.value = "401"
                else _error.value = "Ошибка отправки"
            } catch (e: Exception) {
                _error.value = "Ошибка подключения"
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        val currentToken = _token.value ?: return
        viewModelScope.launch {
            try {
                RetrofitClient.api.logout(currentToken)
            } catch (e: Exception) {
                // игнорируем
            } finally {
                _token.value = null
                _username.value = ""
                _channels.value = emptyList()
                _messages.value = emptyList()
                _selectedChannel.value = null
                onSuccess()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
            val channel = _selectedChannel.value ?: return
            _messages.value = emptyList()
            loadMessages(channel)
        }
    }

    init {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    override fun onCleared() {
        super.onCleared()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}

private fun Message.toEntity(channel: String) = MessageEntity(
    id = id,
    channelName = channel,
    fromUser = from,
    textContent = data.Text?.text,
    imageLink = data.Image?.link,
    time = time
)

private fun MessageEntity.toMessage() = Message(
    id = id,
    from = fromUser,
    to = channelName,
    data = io.github.mobdev.network.MessageData(
        Text = textContent?.let { io.github.mobdev.network.TextData(it) },
        Image = imageLink?.let { io.github.mobdev.network.ImageData(it) }
    ),
    time = time
)