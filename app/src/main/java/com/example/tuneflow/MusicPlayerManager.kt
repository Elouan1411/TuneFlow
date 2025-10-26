package com.example.tuneflow


import android.media.MediaPlayer
import android.util.Log

object MusicPlayerManager {

    private var mediaPlayer: MediaPlayer? = null
    private var isRun: Boolean = true

    fun playSong(url: String) {
        stopSong()

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
    fun stopSong() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.stop()
            }
            mp.release()
        }
        mediaPlayer = null
    }

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

    fun resumeSong(){
        mediaPlayer?.let {
            if (!it.isPlaying){
                it.start()
                isRun = true
            }
        }
    }

    fun getIsRun():Boolean{
        return isRun
    }

    fun setIsRun(musicRun: Boolean){
        isRun = musicRun
    }



}
