package com.example.tuneflow.ui.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.ImageViewCompat
import com.example.tuneflow.R
import com.example.tuneflow.data.Song
import com.example.tuneflow.db.TuneFlowDatabase
import com.example.tuneflow.player.MusicPlayerManager
import kotlin.sequences.forEach

object generalTools {
    /**
     * Updates a song's like icon.
     * @param songId
     * @param icon the ImageButton button to update
     * @param init if true, just initialize the color according to the DB, otherwise toggle
     */
    fun updateLikeIcon(song: Song, icon: ImageButton, db: TuneFlowDatabase, init: Boolean = false) {
        var liked = db.isSongLiked(song.trackId)

        if (!init) {
            // add in db
            db.addListenedSong(song)
            // toggle if not just for initialization
            db.addLikedSong(song.trackId, !liked)
            liked = !liked
        }

        // met la couleur correcte
        val colorRes = if (liked) R.color.green else R.color.icon
        ImageViewCompat.setImageTintList(
            icon,
            ColorStateList.valueOf(ContextCompat.getColor(icon.context, colorRes))
        )
    }

    /**
     * Manages the transition between play and pause modes, ensuring that when a new song starts,
     * the currently playing song is automatically paused
     * @param db Database instance for storing listening history.
     * @param song The selected song to play or pause.
     * @param container The layout containing all song items.
     * @param playButton The play/pause button of the selected song.
     */
    fun toggleSong(
        db: TuneFlowDatabase,
        song: Song,
        container: LinearLayout,
        playButton: ImageButton
    ) {
        db.addListenedSong(song)
        val currentSongId = MusicPlayerManager.getCurrentSong()
        val isPlaying = MusicPlayerManager.getIsRun()
        // Reset buttons of other songs
        container.children.forEach { child ->
            val btn = child.findViewById<ImageButton>(R.id.buttonPlay)
            if (btn != playButton) {
                btn?.setImageResource(R.drawable.ic_play)
            }
        }

        if (currentSongId == song.trackId) {
            if (isPlaying) {
                // Same song → pause
                MusicPlayerManager.pauseSong()
                playButton.setImageResource(R.drawable.ic_play)
            } else {
                // Same song → repeat
                MusicPlayerManager.resumeSong()
                playButton.setImageResource(R.drawable.ic_pause)
            }
        } else {
            // New song → play
            MusicPlayerManager.playSong(song.previewUrl, song.trackId)
            playButton.setImageResource(R.drawable.ic_pause)
        }
    }

    /**
     * Opens the song in Apple Music if installed, otherwise in a web browser.
     */
    fun redirectOnAppleMusic(context: Context, trackUrl: String) {
        // Intent pour Apple Music
        val appIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(trackUrl)
            setPackage("com.apple.android.music") // official package android for apple music
        }

        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(trackUrl))

        try {
            context.startActivity(appIntent)
        } catch (e: ActivityNotFoundException) {
            // if doesn't have application
            context.startActivity(webIntent)
        }
    }

    /**
     * Share a song with a personalized message including the Apple Music link.
     *
     * @param context the context from which the Intent will be launched
     * @param message the message that will be sent
     */
    fun shareMessage(context: Context, message: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(intent, "Partager via"))
    }


    /**
     * Share playlist with the titles of each song
     *  @param name Name of the playlist
     *  @param db TuneFlowDatabase
     *  @param context Context
     */
    fun sharePlaylist(name: String, db: TuneFlowDatabase, context: Context) {
        val songs: List<Song> = db.getSongsFromPlaylist(name)
        val songsList =
            songs.joinToString(separator = "\n") { "   🎵 ${it.trackName} - ${it.artistName}" }

        val message = """
🎶 J'ai créé ma playlist "${name}" grâce à TuneFlow !
Découvre les titres que j'y ai ajoutés :
$songsList
""".trimIndent()
        shareMessage(context, message)
    }

}