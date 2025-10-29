package com.example.tuneflow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuneflow.MainActivity
import com.example.tuneflow.R
import com.example.tuneflow.data.Song
import com.example.tuneflow.db.TuneFlowDatabase
import com.example.tuneflow.ui.adapters.PlaylistAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlaylistBottomSheet(
    private val song: Song
) : BottomSheetDialogFragment() {

    private val db: TuneFlowDatabase
        get() = (activity as MainActivity).db

    private lateinit var playlistAdapter: PlaylistAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.bottom_sheet_playlist, container, false)

        // Displays the current song
        view.findViewById<TextView>(R.id.songInfo).text = "${song.trackName} • ${song.artistName}"

        // Button create playlist
        val btnCreate = view.findViewById<LinearLayout>(R.id.createPlaylist)
        btnCreate.setOnClickListener {
            PopupNewPlaylistFragment { playlistName ->
                db.createPlaylist(playlistName)
                refreshPlaylists()
            }.show(parentFragmentManager, "popup_new_playlist")
        }

        // RecyclerView for playlist
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvPlaylists)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // init adapter
        playlistAdapter = PlaylistAdapter(db.getPlaylistsInfo()) { playlist ->
             if (!db.addSongToPlaylist(song, playlist.name))  db.removeSongFromPlaylist(song, playlist.name)
            refreshPlaylists()
        }
        recyclerView.adapter = playlistAdapter

        // Button close BottomSheet
        val closeButton: ImageView = view.findViewById(R.id.closeButtonBottomSheet)
        closeButton.setOnClickListener {
            dismiss()
        }

        return view
    }

    /**
     * Refresh the playlist display
     */
    fun refreshPlaylists() {
        val updatedPlaylists = db.getPlaylistsInfo()
        playlistAdapter.updateData(updatedPlaylists)
    }
}
