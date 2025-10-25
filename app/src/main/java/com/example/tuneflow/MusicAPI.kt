package com.example.tuneflow

import retrofit2.http.GET

interface MusicAPI {
    @GET("search?term=biga+ranx&media=music&limit=20")
    // suspend for use coroutines (async)
    suspend fun getSongs(): SongResponse
}