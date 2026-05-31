package io.github.mobdev.network

import retrofit2.http.*

interface ApiService {

    @POST("login")
    suspend fun login(@Body request: LoginRequest): retrofit2.Response<okhttp3.ResponseBody>

    @GET("channels")
    suspend fun getChannels(@Header("X-Auth-Token") token: String): List<String>

    @GET("channel/{name}")
    suspend fun getMessages(
        @Header("X-Auth-Token") token: String,
        @Path("name") channelName: String,
        @Query("limit") limit: Int = 20,
        @Query("lastKnownId") lastKnownId: String = "0"
    ): List<Message>

    @POST("messages")
    suspend fun sendMessage(
        @Header("X-Auth-Token") token: String,
        @Body message: SendMessageRequest
    ): retrofit2.Response<okhttp3.ResponseBody>

    @POST("logout")
    suspend fun logout(@Header("X-Auth-Token") token: String)
}