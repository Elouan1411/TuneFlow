package com.example.tuneflow

data class SongResponse(
    val resultCount: Int,
    val results: List<Song>
)

data class Song(
    val artistId: Long,
    val collectionId: Long,
    val trackId: Long,
    val artistName: String,
    val collectionName: String,
    val trackName: String,
    val artistViewUrl: String,
    val collectionViewUrl: String,
    val trackViewUrl: String,
    val previewUrl: String,
    val artworkUrl60: String,
    val artworkUrl100: String,
    val releaseDate: String,
    val trackTimeMillis: Long,
    val country: String,
    val primaryGenreName: String
)
