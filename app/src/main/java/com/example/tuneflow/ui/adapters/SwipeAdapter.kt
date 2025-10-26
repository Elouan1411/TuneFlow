package com.example.tuneflow.ui.adapters

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.tuneflow.MainActivity
import com.example.tuneflow.R
import com.example.tuneflow.data.Song
import com.example.tuneflow.db.TuneFlowDatabase
import com.example.tuneflow.player.MusicPlayerManager
import com.example.tuneflow.ui.adapters.SwipeAdapter.SwipeViewHolder


class SwipeAdapter(val items: MutableList<Song>, private val db: TuneFlowDatabase,) :
    RecyclerView.Adapter<SwipeViewHolder>() {


    inner class SwipeViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val description: TextView = view.findViewById(R.id.description)           // Artist - Album
        val coverImage: ImageView = view.findViewById(R.id.covert_art)            // Cover art
        val overlayCover: View = view.findViewById(R.id.overlay)

        val arrow: ImageView = view.findViewById(R.id.arrow_bottom)               // Animated arrow
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val textAuthor: TextView = view.findViewById(R.id.textAuthor)

        val vinyl: FrameLayout = view.findViewById(R.id.vinyl)
        val vinyl_image: ImageView = view.findViewById(R.id.vinyl_image)
        val vinyl_cover: ImageView = view.findViewById(R.id.vinyl_cover)


        val soundContainer = view.findViewById<FrameLayout>(R.id.sound_container)

        val frameLayoutLike = view.findViewById<FrameLayout>(R.id.frameLayoutLike)

        var isLiked: Boolean = false


        // Animator pour la vinyle
        val rotation: ObjectAnimator = ObjectAnimator.ofFloat(vinyl, "rotation", 0f, 360f).apply {
            duration = 5000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }


        // Animate arrow
        val animator = ObjectAnimator.ofFloat(arrow, "translationY", 0f, 20f).apply {
            duration = 500
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_page_home, parent, false)


        return SwipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: SwipeViewHolder, position: Int) {
        val song = items[position]
        // keep only year
        val songYear = song.releaseDate.substringBefore("-")

        // Set texts
        holder.textTitle.text = song.trackName
        holder.textAuthor.text = song.artistName
        "${song.collectionName}    •    ${song.primaryGenreName}    •    $songYear".also {
            holder.description.text = it
        }
        holder.description.isSelected = true // activate scroll

        // Load cover art with borderRadius
        val radiusDp = 16f
        val radiusPx = (radiusDp * holder.view.context.resources.displayMetrics.density).toInt()
        Glide.with(holder.view.context)
            .load(updateImageUrl(song.artworkUrl100))
            .apply(RequestOptions().transform(RoundedCorners(radiusPx)))
            .into(holder.coverImage)

        // Load vinyl //todo: change with wave ?
        Glide.with(holder.view.context)
            .load(song.artworkUrl60)
            .apply(RequestOptions().transform(CircleCrop()))
            .into(holder.vinyl_cover)
        Glide.with(holder.view.context)
            .load(R.drawable.ic_vinyl)
            .apply(RequestOptions().transform(CircleCrop()))
            .into(holder.vinyl_image)


        // detect click on kie button
        holder.frameLayoutLike.setOnClickListener {
            holder.isLiked = !holder.isLiked
            if (holder.isLiked) {
                // TODO:remove color
            }
            db.addLikedSong(song.trackId, holder.isLiked)
        }

        // detect click on cover for stop music and do animation
        holder.coverImage.setOnClickListener {
            // tap play
            if (holder.overlayCover.isVisible) {
                holder.overlayCover.visibility = View.GONE
                holder.soundContainer.visibility = View.GONE
                MusicPlayerManager.resumeSong()
            } else {
                // tap pause
                holder.overlayCover.visibility = View.VISIBLE
                holder.soundContainer.visibility = View.VISIBLE
                MusicPlayerManager.pauseSong()
            }

            if (holder.rotation.isPaused) holder.rotation.resume() else holder.rotation.pause()


        }
    }

    override fun getItemCount(): Int = items.size

    fun addPage(song: Song) {
        items.add(song)
        notifyItemInserted(items.size - 1)
    }

    fun removeFirstPage() {
        if (items.isNotEmpty()) {
            items.removeAt(0)
            notifyItemRemoved(0)
        }
    }

    fun getSongAt(position: Int): Song? {
        return if (position in 0 until items.size) {
            items[position]
        } else {
            null
        }
    }

    fun updateImageUrl(url: String): String {
        return url.replace("100x100", "600x600")
    }

    fun containsSong(song: Song): Boolean {
        return items.any { it.trackId == song.trackId }
    }



}