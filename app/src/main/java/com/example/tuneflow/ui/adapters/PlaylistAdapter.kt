package com.example.tuneflow.ui.adapters

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.tuneflow.R
import com.example.tuneflow.data.PlaylistInfo

class PlaylistAdapter(
    private var playlists: List<PlaylistInfo>,
    private val onClick: (PlaylistInfo) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {
    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverImage: ImageView = itemView.findViewById(R.id.imagePlaylistCover)
        val nameText: TextView = itemView.findViewById(R.id.textPlaylistName)
        val countText: TextView = itemView.findViewById(R.id.textPlaylistCount)


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist_bottomsheet, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlists[position]

        holder.nameText.text = playlist.name
        val word = if (playlist.songCount <= 1) "morceau" else "morceaux"
        holder.countText.text = "${playlist.songCount} $word"

        val radiusPx = (12 * holder.itemView.context.resources.displayMetrics.density).toInt()

        if (playlist.lastSongCoverUrl.isNotEmpty()) {
            Glide.with(holder.itemView)
                .load(playlist.lastSongCoverUrl.replace("100x100", "600x600"))
                .placeholder(ColorDrawable(Color.TRANSPARENT))
                .apply(RequestOptions().transform(RoundedCorners(radiusPx)))
                .into(holder.coverImage)
        } else {
            Glide.with(holder.itemView)
                .load(R.drawable.ic_cover)
                .placeholder(ColorDrawable(Color.TRANSPARENT))
                .apply(RequestOptions().transform(RoundedCorners(radiusPx)))
                .into(holder.coverImage)
        }



        holder.itemView.setOnClickListener {
            onClick(playlist)
        }


    }

    override fun getItemCount(): Int = playlists.size

    fun updateData(newPlaylists: List<PlaylistInfo>) {
        playlists = newPlaylists
        notifyDataSetChanged()
    }
}
