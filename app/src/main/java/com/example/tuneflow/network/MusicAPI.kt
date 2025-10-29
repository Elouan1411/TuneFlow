package com.example.tuneflow.network

import com.example.tuneflow.data.SongResponse
import retrofit2.http.GET
import retrofit2.http.Query
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

interface MusicAPI {

    @GET("search?media=music")
    suspend fun getSongs(
        @Query("term") searchTerm: String,
        @Query("limit") limit: Int = 200 // default value
    ): SongResponse
}
