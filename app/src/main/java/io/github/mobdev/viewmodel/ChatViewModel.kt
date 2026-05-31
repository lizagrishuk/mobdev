package io.github.mobdev.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mobdev.network.Message
import io.github.mobdev.network.MessageData
import io.github.mobdev.network.RetrofitClient
import io.github.mobdev.network.SendMessageRequest
import io.github.mobdev.network.TextData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

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
        // не загружаем если уже есть данные - защита от повторной загрузки при повороте
        if (_channels.value.isNotEmpty()) return
        val currentToken = _token.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _channels.value = RetrofitClient.api.getChannels(currentToken)
                _error.value = null
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401) _error.value = "401"
                else _error.value = "Ошибка загрузки каналов"
            } catch (e: Exception) {
                _error.value = "Ошибка подключения"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectChannel(channel: String) {
        // если тот же канал уже выбран - не перезагружаем
        if (_selectedChannel.value == channel && _messages.value.isNotEmpty()) return
        _selectedChannel.value = channel
        _messages.value = emptyList()
        _hasMoreMessages.value = true
        loadMessages(channel)
    }

    fun loadMessages(channel: String) {
        // не загружаем если уже есть данные - защита от повторной загрузки при повороте
        if (_messages.value.isNotEmpty()) return
        val currentToken = _token.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = RetrofitClient.api.getMessages(
                    currentToken, channel, limit = 20, lastKnownId = "0"
                )
                _messages.value = result
                _hasMoreMessages.value = result.size >= 20
                _error.value = null
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401) _error.value = "401"
                else _error.value = "Ошибка загрузки сообщений"
            } catch (e: Exception) {
                _error.value = "Ошибка подключения"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreMessages() {
        // подгружаем следующие 20 при скролле вверх
        if (_isLoadingMore.value || !_hasMoreMessages.value) return
        val currentToken = _token.value ?: return
        val currentChannel = _selectedChannel.value ?: return
        val lastId = _messages.value.maxOfOrNull { it.id } ?: return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val result = RetrofitClient.api.getMessages(
                    currentToken, currentChannel, limit = 20, lastKnownId = lastId.toString()
                )
                if (result.isEmpty()) {
                    _hasMoreMessages.value = false
                } else {
                    _messages.value = _messages.value + result
                    _hasMoreMessages.value = result.size >= 20
                }
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401) _error.value = "401"
            } catch (e: Exception) {
                _error.value = "Ошибка подключения"
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun sendMessage(text: String) {
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
                // после отправки загружаем свежие сообщения принудительно
                val result = RetrofitClient.api.getMessages(
                    currentToken, currentChannel, limit = 20, lastKnownId = "0"
                )
                _messages.value = result
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
                // игнорируем ошибки при выходе
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
}