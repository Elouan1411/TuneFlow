package com.example.tuneflow

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.RoundedCorner
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import androidx.core.view.isVisible

class SwipeAdapter(val items: MutableList<Song>) :
    RecyclerView.Adapter<SwipeAdapter.SwipeViewHolder>() {


    inner class SwipeViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val description: TextView = view.findViewById(R.id.description)           // Artist - Album
        val coverImage: ImageView = view.findViewById(R.id.covert_art)            // Cover art
        val overlayCover: View = view.findViewById(R.id.overlay)

        val arrow: ImageView = view.findViewById(R.id.arrow_bottom)               // Animated arrow
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val textAuthor: TextView = view.findViewById(R.id.textAuthor)

        val vinyl: ImageView = view.findViewById(R.id.vinyl)

        val soundContainer = view.findViewById<FrameLayout>(R.id.sound_container)


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_page_home, parent, false)
        return SwipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: SwipeViewHolder, position: Int) {
        val song = items[position]

        // Set texts
        holder.textTitle.text = song.trackName
        holder.textAuthor.text = song.artistName
        holder.description.text = "${song.artistName} - ${song.collectionName}"
        holder.description.isSelected = true // activate scroll

        // Load cover art with borderRadius
        val radiusDp = 16f
        val radiusPx = (radiusDp * holder.view.context.resources.displayMetrics.density).toInt()
        Glide.with(holder.view.context)
            .load(song.artworkUrl100)
            .apply(RequestOptions().transform(RoundedCorners(radiusPx)))
            .into(holder.coverImage)

        // Load vinyl
        Glide.with(holder.view.context)
            .load(song.artworkUrl60)
            .apply(RequestOptions().transform(CircleCrop()))
            .into(holder.vinyl)

        // Animate arrow
        val animator = ObjectAnimator.ofFloat(holder.arrow, "translationY", 0f, 20f)
        animator.duration = 500
        animator.repeatMode = ValueAnimator.REVERSE
        animator.repeatCount = ValueAnimator.INFINITE
        animator.start()

        // Animate vinyl
        val rotation = ObjectAnimator.ofFloat(holder.vinyl, "rotation", 0f, 360f)
        rotation.duration = 5000
        rotation.repeatCount = ValueAnimator.INFINITE
        rotation.interpolator = LinearInterpolator()
        rotation.start()

        // animate sound mute
        // TODO?



        // detect click on cover for stop music and do animation
        holder.coverImage.setOnClickListener {
            // tap play
            if (holder.overlayCover.isVisible) {
                holder.overlayCover.visibility = View.GONE
                holder.soundContainer.visibility = View.GONE
                MusicPlayerManager.resumeSong()
                rotation.resume()
            } else {
                // tap pause
                holder.overlayCover.visibility = View.VISIBLE
                holder.soundContainer.visibility = View.VISIBLE
                MusicPlayerManager.pauseSong()
                rotation.pause()
            }


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








}

