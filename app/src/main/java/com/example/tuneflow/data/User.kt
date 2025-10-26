package com.example.tuneflow.data

data class User(
    var id: Int,
    var totalListeningTime: Long,
    var discoveredSongs: Int,
    var likedSongs: Int,
    var playlistsCount: Int,
    var discoveredArtists: Int
)