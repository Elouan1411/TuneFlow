package com.example.tuneflow.player

import android.media.MediaPlayer
import android.util.Log

object MusicPlayerManager {

    private var mediaPlayer: MediaPlayer? = null
    private var isRun: Boolean = true

    private var currentSong: Long = 0

    /**
     * Plays the song from the given URL and loops it.
     * Stops any current playback before starting the new one.
     *
     * @param url Song preview URL.
     * @param id Song ID.
     */
    fun playSong(url: String, id:Long) {
        stopSong()
        isRun = true
        currentSong = id

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(url)
                setOnPreparedListener { mp ->
                    mp.isLooping = true
                    mp.start()
                }
                prepareAsync()
            } catch (e: Exception) {
                Log.e("MusicPlayerManager", "Error playing song: ${e.message}")
            }
        }
    }

    /**
     * Stops and releases the current song if one is playing.
     */
    fun stopSong() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.stop()
            }
            mp.release()
        }
        mediaPlayer = null
    }

    /**
     * Pauses the current song.
     * If called when closing the app, keeps the state active.
     */
    fun pauseSong(onStop: Boolean = false) {
        mediaPlayer?.let {
            if (it.isPlaying){
                it.pause()
                // when we close the app
                if (!onStop){
                    isRun = false
                }

            }
        }
    }

    /**
     * Resumes the song if it was paused.
     */
    fun resumeSong(){
        mediaPlayer?.let {
            if (!it.isPlaying){
                it.start()
                isRun = true
            }
        }
    }

    /**
     * @return true if a song is currently playing.
     */
    fun getIsRun(): Boolean {
        return isRun
    }

    /**
     * Updates the current play state.
     */
    fun setIsRun(musicRun: Boolean) {
        isRun = musicRun
    }

    /**
     * @return the ID of the currently playing song.
     */
    fun getCurrentSong(): Long {
        return currentSong
    }



}