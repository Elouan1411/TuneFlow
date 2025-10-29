package com.example.tuneflow.network

import com.example.tuneflow.network.MusicAPI
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://itunes.apple.com/"

    val api: MusicAPI by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(
                MusicAPI::class.java
            )
    }

    /**
     * Removing characters not accepted by the API from the URL
     * @param term the words that will be transmitted to the API in the URL
     */
    fun cleanUrlForApi(term: String): String {
        // change accented characters to unaccented characters
        val normalized = java.text.Normalizer.normalize(term, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        // replace space with +
        val withPluses = normalized.replace(" ", "+")
        // Removes all unauthorized characters
        return withPluses.replace(Regex("[^a-zA-Z0-9.+\\-_*]"), "")
    }
}