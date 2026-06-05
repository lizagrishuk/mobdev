package io.github.mobdev.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: Long,
    val channelName: String,
    val fromUser: String,
    val textContent: String?,
    val imageLink: String?,
    val time: Long?
)