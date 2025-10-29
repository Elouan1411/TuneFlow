package com.example.tuneflow.network

import com.example.tuneflow.data.SongResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicAPI {

    @GET("search?media=music")
    suspend fun getSongs(
        @Query("term") searchTerm: String,
        @Query("limit") limit: Int = 200 // default value
    ): Response<SongResponse>
}
