package io.github.mobdev.network


data class LoginRequest(
    val name: String,
    val pwd: String
)


data class MessageData(
    val Text: TextData? = null,
    val Image: ImageData? = null
)

data class TextData(
    val text: String
)

data class ImageData(
    val link: String? = null
)

data class Message(
    val id: Long,
    val from: String,
    val to: String? = null,
    val data: MessageData,
    val time: Long? = null
)

data class SendMessageRequest(
    val from: String,
    val to: String = "1@channel",
    val data: MessageData
)