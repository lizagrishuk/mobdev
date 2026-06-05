package io.github.mobdev.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE channelName = :channel ORDER BY id ASC")
    suspend fun getMessages(channel: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE channelName = :channel")
    suspend fun deleteMessages(channel: String)
}