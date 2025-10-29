package com.example.tuneflow.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.tuneflow.MainActivity
import com.example.tuneflow.R
import com.example.tuneflow.db.TuneFlowDatabase
import com.example.tuneflow.player.MusicPlayerManager

class PlaylistsFragment : Fragment() {

    private val db: TuneFlowDatabase
        get() = (activity as MainActivity).db

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_playlists, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("myDebug", "onViewCreated")

        val playlists = db.getPlaylistsInfo() // get playlists
        var word = if (playlists.size <= 1) "playlist" else "playlists"
        view.findViewById<TextView>(R.id.subtitleFragmentPlaylist).text = "${playlists.size} $word"

        val gridLayout = view.findViewById<GridLayout>(R.id.gridLayoutPlaylist)
        gridLayout.removeAllViews()


        val columnCount = 2
        gridLayout.columnCount = columnCount

        playlists.forEachIndexed { index, playlist ->
            val itemView = layoutInflater.inflate(R.layout.item_playlist, gridLayout, false)

            // get view of each item
            val coverImage = itemView.findViewById<ImageView>(R.id.coverPlaylist)
            val nameText = itemView.findViewById<TextView>(R.id.namePlaylist)
            val nbSongsText = itemView.findViewById<TextView>(R.id.nbSongsPlaylist)

            // fill data
            nameText.text = playlist.name
            word = if (playlist.songCount <= 1) "morceau" else "morceaux"
            nbSongsText.text = "${playlist.songCount} $word"


            // Load cover art with borderRadius
            val radiusDp = 16f
            val radiusPx = (radiusDp * view.context.resources.displayMetrics.density).toInt()
            Glide.with(this)
                .load(playlist.lastSongCoverUrl.replace("100x100", "600x600"))
                .placeholder(ColorDrawable(Color.TRANSPARENT))
                .apply(RequestOptions().transform(RoundedCorners(radiusPx)))
                .transition(DrawableTransitionOptions.withCrossFade())
                .skipMemoryCache(true) // for transition
                .into(coverImage)


            // configuring LayoutParams for GridLayout
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(index % columnCount, 1f)
                rowSpec = GridLayout.spec(index / columnCount)
                setMargins(8, 8, 8, 8)
            }
            itemView.layoutParams = params

            // add item in the grid
            gridLayout.addView(itemView)
        }

        // Add an "empty element" if the number of playlists is odd.
        if (playlists.size == 1) {
            val emptyView = View(context)
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(1, 1f) // 2nd column
                rowSpec = GridLayout.spec(0)        // 1st row
            }
            emptyView.layoutParams = params
            gridLayout.addView(emptyView)
        }

        // adjust number of row
        val rowCount = (playlists.size + columnCount - 1) / columnCount
        gridLayout.rowCount = rowCount
    }

    override fun onResume() {
        super.onResume()
        // stop music from home
        MusicPlayerManager.stopSong()
    }

    override fun onStop() {
        super.onStop()
        MusicPlayerManager.pauseSong(true)
    }


}