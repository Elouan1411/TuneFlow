package com.example.tuneflow.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tuneflow.MainActivity
import com.example.tuneflow.R
import com.example.tuneflow.db.TuneFlowDatabase
import com.example.tuneflow.player.MusicPlayerManager
import com.example.tuneflow.ui.adapters.InPlaylistAdapter
import com.example.tuneflow.ui.utils.generalTools
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class InPlaylistBottomSheet(
    private val playlistName: String,
    private val nbSongs: Int,
    private val onPlaylistChanged: () -> Unit
) : BottomSheetDialogFragment() {

    private val db: TuneFlowDatabase
        get() = (activity as MainActivity).db

    private lateinit var adapter: InPlaylistAdapter
    private lateinit var textNbSong: TextView
    private var nbCurrentSongs: Int = nbSongs

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.bottom_sheet_in_playlist, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvInPlaylists)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val songs = db.getSongsFromPlaylist(playlistName)

        view.findViewById<TextView>(R.id.titleBottomSheetInPlaylist).text = playlistName
        textNbSong = view.findViewById<TextView>(R.id.nbSongsInPlaylist)
        textNbSong.text = if (nbSongs <= 1) "$nbSongs morceau" else "$nbSongs morceaux"

        adapter = InPlaylistAdapter(
            songs,
            db,
            onSongChanged = { refreshSongs() },
            fragmentManagerProvider = { parentFragmentManager },
            playlistName
        )

        recyclerView.adapter = adapter

        val closeButton: ImageView = view.findViewById(R.id.closeButtonBottomSheetInPlaylist)
        closeButton.setOnClickListener { dismiss() }

        val deleteButton: ImageView = view.findViewById(R.id.deleteButtonBottomSheetInPlaylist)
        deleteButton.setOnClickListener {
            val context = it.context
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Supprimer la playlist")
                .setMessage("Êtes-vous sûr de vouloir supprimer \"$playlistName\" ?")
                .setPositiveButton("Oui") { dialog, _ ->
                    db.deletePlaylist(playlistName)
                    dialog.dismiss()
                    onPlaylistChanged()
                    dismiss()
                }
                .setNegativeButton("Non") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        // listener for share playlist
        view.findViewById<TextView>(R.id.sharePlaylistText).setOnClickListener {
            generalTools.sharePlaylist(playlistName, db, requireContext())
        }

        return view
    }

    /**
     * Updates data following a deletion
     */
    private fun refreshSongs() {
        val updatedSongs = db.getSongsFromPlaylist(playlistName)
        adapter.updateData(updatedSongs)
        nbCurrentSongs -= 1
        textNbSong.text = if (nbCurrentSongs <= 1) "$nbCurrentSongs morceau" else "$nbCurrentSongs morceaux"
        onPlaylistChanged()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        MusicPlayerManager.stopSong()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        MusicPlayerManager.stopSong()
    }
}
