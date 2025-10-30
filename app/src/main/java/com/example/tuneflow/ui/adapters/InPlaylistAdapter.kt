package com.example.tuneflow.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.tuneflow.R
import com.example.tuneflow.data.Song
import com.example.tuneflow.db.TuneFlowDatabase
import com.example.tuneflow.player.MusicPlayerManager
import com.example.tuneflow.ui.PlaylistBottomSheet
import com.example.tuneflow.ui.utils.generalTools

class InPlaylistAdapter(
    private var songs: List<Song>,
    private val db: TuneFlowDatabase,
    private val onSongChanged: () -> Unit, // To refresh if modified, use the add playlist menu.
    private val fragmentManagerProvider: () -> androidx.fragment.app.FragmentManager,
    private val playlistName: String
) : RecyclerView.Adapter<InPlaylistAdapter.SongViewHolder>() {

    private var currentPlayingId: Long? = null
    private var previousPlayingHolder: SongViewHolder? = null


    inner class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layout: LinearLayout = view.findViewById(R.id.layoutOneResult)
        val cover: ImageView = view.findViewById(R.id.imageCoverSearch)
        val title: TextView = view.findViewById(R.id.titleSearch)
        val artist: TextView = view.findViewById(R.id.authorSearch)
        val album: TextView = view.findViewById(R.id.albumSearch)
        val playButton: ImageButton = view.findViewById(R.id.buttonPlay)
        val deleteButton: ImageButton = view.findViewById(R.id.buttonLikeSearch)
        val moreButton: ImageButton = view.findViewById(R.id.buttonMore)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        val context = holder.itemView.context
        holder.title.text = song.trackName
        holder.artist.text = song.artistName
        holder.album.text = song.collectionName


        val radiusDp = 16f
        val radiusPx = (radiusDp * context.resources.displayMetrics.density).toInt()
        Glide.with(context)
            .load(song.artworkUrl100)
            .apply(RequestOptions().transform(RoundedCorners(radiusPx)))
            .transition(DrawableTransitionOptions.withCrossFade())
            .skipMemoryCache(true)
            .placeholder(R.drawable.ic_cover)
            .into(holder.cover)

        // button delete
        holder.deleteButton.setImageResource(R.drawable.ic_delete)
        holder.deleteButton.setOnClickListener {
            val context = holder.itemView.context
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Supprimer la chanson")
                .setMessage("Voulez-vous vraiment retirer \"${song.trackName}\" de la playlist ?")
                .setPositiveButton("Oui") { dialog, _ ->
                    db.removeSongFromPlaylist(song, playlistName)
                    onSongChanged()
                    dialog.dismiss()
                }
                .setNegativeButton("Non") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }


        val isPlaying = song.trackId == currentPlayingId
        holder.playButton.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )


        // play button
        holder.itemView.setOnClickListener {
            changePlayPause(song, holder)
        }
        holder.playButton.setOnClickListener {
            changePlayPause(song, holder)
        }


        // popupMenu
        val popup = PopupMenu(context, holder.moreButton)
        popup.inflate(R.menu.menu_options_song)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.addToPlaylist -> {
                    PlaylistBottomSheet(song)
                        .show(fragmentManagerProvider(), "playlistBottomSheet")
                    true
                }

                R.id.listenOnAppleMusic -> {
                    generalTools.redirectOnAppleMusic(context, song.trackViewUrl)
                    true
                }

                R.id.share -> {
                    generalTools.shareMessage(
                        context,
                        "🎵 J'ai découvert \"${song.trackName}\" de ${song.artistName} et je te la recommande ! Écoute-la ici : ${song.trackViewUrl}"
                    )
                    true
                }

                else -> false
            }
        }
        holder.moreButton.setOnClickListener { popup.show() }
    }

    override fun getItemCount(): Int = songs.size


    /**
     * Update songs
     */
    fun updateData(newSongs: List<Song>) {
        songs = newSongs
        notifyDataSetChanged()
    }

    /**
     * Start or stop music by toggling the button.
     * It's not possible to use generalTools.toggleSong() because we're in a RecyclerView.
     * @param song the sound that was clicked
     * @param holder
     */
    private fun changePlayPause(song: Song, holder: SongViewHolder) {
        if (currentPlayingId == song.trackId) {
            // Pause
            MusicPlayerManager.pauseSong()
            currentPlayingId = null
            holder.playButton.setImageResource(R.drawable.ic_play)
        } else {
            MusicPlayerManager.playSong(song.previewUrl, song.trackId)

            previousPlayingHolder?.playButton?.setImageResource(R.drawable.ic_play)

            currentPlayingId = song.trackId
            holder.playButton.setImageResource(R.drawable.ic_pause)
            previousPlayingHolder = holder
        }
    }
}
