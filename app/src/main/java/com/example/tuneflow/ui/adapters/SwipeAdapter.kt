package com.example.tuneflow.ui.adapters

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.tuneflow.R
import com.example.tuneflow.data.Song
import com.example.tuneflow.db.TuneFlowDatabase
import com.example.tuneflow.player.MusicPlayerManager
import com.example.tuneflow.ui.HomeFragment
import com.example.tuneflow.ui.PlaylistBottomSheet
import com.example.tuneflow.ui.adapters.SwipeAdapter.SwipeViewHolder
import com.example.tuneflow.ui.utils.generalTools


class SwipeAdapter(val items: MutableList<Song>, private val db: TuneFlowDatabase, private val homeFragment: HomeFragment, private val fragmentManager: FragmentManager) :
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
        val frameLayoutPlaylist = view.findViewById<FrameLayout>(R.id.frameLayoutPlaylist)
        val buttonLike: ImageView = view.findViewById<ImageView>(R.id.button_like)

        var isLiked: Boolean = false
        var isAddedToPlaylist = false

        val buttonAddPlaylist: ImageView = view.findViewById<ImageView>(R.id.button_add_playlist)

        val buttonAppleMusic: FrameLayout = view.findViewById<FrameLayout>(R.id.frameLayoutLinkApple)
        val textAppleMusic: TextView = view.findViewById<TextView>(R.id.appleMusicText)

        val texteAuthor: TextView = view.findViewById<TextView>(R.id.textAuthor)

        val layoutInfoCurrentMood: RelativeLayout = view.findViewById<RelativeLayout>(R.id.layoutInfoCurrentMood)

        val rootLayout: FrameLayout = view.findViewById(R.id.rootLayout)



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


        // Set texts
        holder.textTitle.text = song.trackName
        holder.textAuthor.text = song.artistName

        val songYear = song.releaseDate?.substringBefore("-")

        // Build the description dynamically
        val parts = mutableListOf<String>()
        parts.add(song.collectionName)
        parts.add(song.primaryGenreName)
        if (!songYear.isNullOrEmpty()) {
            parts.add(songYear) // only add if songYear exists
        }

        holder.description.text = parts.joinToString("    •    ")


        holder.description.isSelected = true // activate scroll

        // Load cover art with borderRadius
        val radiusDp = 16f
        val radiusPx = (radiusDp * holder.view.context.resources.displayMetrics.density).toInt()
        Glide.with(holder.view.context)
            .load(updateImageUrl(song.artworkUrl100))
            .apply(RequestOptions().transform(RoundedCorners(radiusPx)))
            .transition(DrawableTransitionOptions.withCrossFade())
            .skipMemoryCache(true) // for transition
            .into(holder.coverImage)

        // Load vinyl
        Glide.with(holder.view.context)
            .load(song.artworkUrl60)
            .apply(RequestOptions().transform(CircleCrop()))
            .into(holder.vinyl_cover)
        Glide.with(holder.view.context)
            .load(R.drawable.ic_vinyl)
            .apply(RequestOptions().transform(CircleCrop()))
            .into(holder.vinyl_image)

        initAdapter(holder)

        // detect click on the button like
        holder.frameLayoutLike.setOnClickListener {
            holder.isLiked = !holder.isLiked
            if (holder.isLiked) {
                like(holder)
            }else{
                unLike(holder)
            }
            db.addLikedSong(song.trackId, holder.isLiked)
        }

        // detect click on the button playlist
        holder.frameLayoutPlaylist.setOnClickListener {
            val bottomSheet = PlaylistBottomSheet(
                song
            )
            bottomSheet.show(fragmentManager, "playlistBottomSheet")
        }

        // detect click remove mood
        holder.layoutInfoCurrentMood.setOnClickListener {
            homeFragment.stopMood()
        }

        // detect click want apple music
        holder.buttonAppleMusic.setOnClickListener {
            generalTools.redirectOnAppleMusic(holder.itemView.context,song.trackViewUrl)
        }
        holder.textAppleMusic.setOnClickListener {
            generalTools.redirectOnAppleMusic(holder.itemView.context,song.trackViewUrl)
        }

        // detect click on cover for stop music and do animation
        holder.coverImage.setOnClickListener {
            // tap play
            if (holder.overlayCover.isVisible) {
                playMusic(holder)
            } else {
                pauseMusic(holder)
            }
        }

        holder.vinyl.setOnClickListener {
            // tap play
            if (holder.overlayCover.isVisible) {
                playMusic(holder)
            } else {
                pauseMusic(holder)
            }
        }

        holder.textAuthor.setOnClickListener {
            generalTools.redirectOnAppleMusic(holder.itemView.context, song.artistViewUrl)
        }

        // double click for like
        // double click for like
        var lastClickTime = 0L
        val DOUBLE_CLICK_TIME_DELTA = 300 // 300 ms max between clicks

        holder.view.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                val clickTime = System.currentTimeMillis()
                if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                    // Double click detected
                    val x = event.x
                    val y = event.y
                    showHeart(x, y, holder.itemView.context, holder.rootLayout)

                    holder.isLiked = !holder.isLiked
                    if (holder.isLiked) {
                        like(holder)
                    } else {
                        unLike(holder)
                    }
                    db.addLikedSong(song.trackId, holder.isLiked)
                }
                lastClickTime = clickTime
            }
            return true
        })

    }


    /**
     * Initialize the adapter for a view holder.
     * @param holder the view holder to initialize
     */
    fun initAdapter(holder: SwipeViewHolder){
        playMusic(holder)
        unLike(holder)
        removeFromPlaylist(holder)
        holder.isLiked = false
        holder.isAddedToPlaylist = false

        val mood: String = homeFragment.getMood()
        if (mood.isNotEmpty()){
            val textView = holder.layoutInfoCurrentMood.getChildAt(1) as TextView
            val moodsTranslations = mapOf(
                "happy" to "joyeux",
                "chill" to "chill",
                "workout" to "sport",
                "romantic" to "romantique",
                "sad" to "triste",
                "focus" to "travail"
            )

            textView.text =  "Quitter le mood ${moodsTranslations[mood]}"
            holder.layoutInfoCurrentMood.visibility = View.VISIBLE
        }
    }

    /**
     * Change the color of a button (ImageView).
     * @param button the button to change color
     * @param color the color resource id
     */
    fun changeColorButton(button: ImageView, color: Int){
        ImageViewCompat.setImageTintList(
            button,
            ColorStateList.valueOf(ContextCompat.getColor(button.context, color))
        )
    }

    /**
     * Set the like button to "unliked" state (white color).
     * @param holder the view holder with the button
     */
    fun unLike(holder: SwipeViewHolder){
        changeColorButton(holder.buttonLike, R.color.icon)
    }

    /**
     * Set the like button to "liked" state (green color).
     * @param holder the view holder with the button
     */
    fun like(holder: SwipeViewHolder){
        changeColorButton(holder.buttonLike, R.color.green)
    }
    /**
     * Set the like button to "unliked" state (white color).
     * @param holder the view holder with the button
     */
    fun removeFromPlaylist(holder: SwipeViewHolder){
        changeColorButton(holder.buttonAddPlaylist, R.color.icon)
    }

    /**
     * Set the like button to "liked" state (green color).
     * @param holder the view holder with the button
     */
    fun addToPlaylist(holder: SwipeViewHolder){
        changeColorButton(holder.buttonAddPlaylist, R.color.green)
    }


    /**
     * Play the music for this view holder.
     * It also hides the overlay and the sound container, and starts the rotation.
     * @param holder the view holder with music controls
     */
    fun playMusic(holder: SwipeViewHolder){
        MusicPlayerManager.resumeSong()
        holder.overlayCover.visibility = View.GONE
        holder.soundContainer.visibility = View.GONE
        holder.rotation.resume()
    }

    /**
     * Pause the music for this view holder.
     * It also shows the overlay and the sound container, and stops the rotation.
     * @param holder the view holder with music controls
     */
    fun pauseMusic(holder: SwipeViewHolder){
        MusicPlayerManager.pauseSong()
        holder.overlayCover.visibility = View.VISIBLE
        holder.soundContainer.visibility = View.VISIBLE
        holder.rotation.pause()
    }


    override fun getItemCount(): Int = items.size

    /**
     * Adds a song to the list and updates the adapter.
     */
    fun addPage(song: Song) {
        items.add(song)
        notifyItemInserted(items.size - 1)
    }

    /**
     * Returns the song at the given position, or null if invalid.
     */
    fun getSongAt(position: Int): Song? {
        return if (position in 0 until items.size) items[position] else null
    }

    /**
     * Returns the image URL in higher resolution.
     */
    fun updateImageUrl(url: String): String {
        return url.replace("100x100", "600x600")
    }

    /**
     * Checks if the song already exists in the list.
     */
    fun containsSong(song: Song): Boolean {
        return items.any { it.trackId == song.trackId }
    }




    /**
     * Shows a heart animation at the given position in a FrameLayout.
     * The heart floats upward with a slight random horizontal drift and fades out.
     *
     * @param x The X coordinate where the heart should appear (in pixels).
     * @param y The Y coordinate where the heart should appear (in pixels).
     * @param context The context used to create the ImageView.
     * @param rootLayout The FrameLayout where the heart will be added and animated.
     */

    fun showHeart(x: Float, y: Float, context: Context, rootLayout: FrameLayout) {
        val randomOffsetX = (-50..50).random() // random horizontal
        val startX = (x - 50 + randomOffsetX).toInt()
        val startY = (y - 50).toInt()
        val endY = startY - 150 // move heart up by 150px

        val heart = ImageView(context).apply {
            setImageResource(R.drawable.ic_heart)
            layoutParams = FrameLayout.LayoutParams(100, 100).apply {
                leftMargin = startX
                topMargin = startY
            }
        }

        rootLayout.addView(heart)

        heart.animate()
            .translationYBy(-150f) // move up
            .translationXBy(randomOffsetX.toFloat()) // horizontal drift
            .alpha(0f)
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(600)
            .withEndAction { rootLayout.removeView(heart) }
            .start()
    }

}