package com.example.tuneflow.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.tuneflow.MainActivity
import com.example.tuneflow.R
import com.example.tuneflow.ui.utils.SwipeListener
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.tuneflow.data.Song
import com.example.tuneflow.db.TuneFlowDatabase
import com.example.tuneflow.network.ApiClient
import com.example.tuneflow.player.MusicPlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class DiscoverFragment : Fragment(), SwipeListener {

    private val imageIds = listOf(
        R.id.image_happy,
        R.id.image_chill,
        R.id.image_workout,
        R.id.image_romantic,
        R.id.image_sad,
        R.id.image_focus
    )

    private val drawables = listOf(
        R.drawable.bg_happy_image,
        R.drawable.bg_chill_image,
        R.drawable.bg_workout_image,
        R.drawable.bg_romantic_image,
        R.drawable.bg_sad_image,
        R.drawable.bg_focus_image
    )

    private lateinit var gridLayout: GridLayout
    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var searchBar: LinearLayout
    private lateinit var noResultsText: TextView
    private lateinit var layoutResult: LinearLayout
    private lateinit var layoutResultWithArrow: LinearLayout
    private lateinit var arrowBottomSearch: ImageView
    private val MAX_RESULT: Int = 200

    private var scrollViewOpen: Boolean = false

    private lateinit var musicLoader: ImageView
    private var loaderAnimator: ObjectAnimator? = null

    private lateinit var closeButton: ImageView
    private lateinit var searchEditText: EditText

    private val db: TuneFlowDatabase
        get() = (activity as MainActivity).db


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_discover, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        searchEditText = view.findViewById<EditText>(R.id.editTextSearchBar)
        var searchJob: Job? = null

        searchEditText.addTextChangedListener { editable ->
            val query = ApiClient.cleanUrlForApi(editable.toString().trim())
            MusicPlayerManager.stopSong()

            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(100) // debounce to avoid too many requests
                if (query.isNotEmpty()) {
                    startMusicLoader()
                    val results = ApiClient.api.getSongs(query, limit = MAX_RESULT).results
                    stopMusicLoader()
                    displaySearchResults(results, query.isEmpty())
                } else {
                    displaySearchResults(emptyList(), query.isEmpty())
                    closeButton.visibility = View.GONE
                }
            }
        }


        titleTextView = view.findViewById(R.id.title_discover)
        subtitleTextView = view.findViewById(R.id.subtitle_discover)
        searchBar = view.findViewById(R.id.search_bar)
        gridLayout = view.findViewById(R.id.gridLayoutDiscover)
        noResultsText = view.findViewById(R.id.noResultsText)
        layoutResult = view.findViewById(R.id.searchResultsContainer)
        layoutResultWithArrow = view.findViewById(R.id.layoutResult)
        arrowBottomSearch = view.findViewById(R.id.arrowBottomSearch)
        musicLoader = view.findViewById(R.id.musicLoader)
        Glide.with(view.context)
            .load(R.drawable.ic_vinyl)
            .apply(RequestOptions().transform(CircleCrop()))
            .into(musicLoader)
        musicLoader.visibility = View.GONE
        val color = ContextCompat.getColor(view.context, R.color.subtitle)
        ImageViewCompat.setImageTintList(musicLoader, ColorStateList.valueOf(color))
        closeButton = view.findViewById(R.id.clear_text)



        makeGridItemsSquare(gridLayout)

        val moods = listOf("happy", "chill", "workout", "romantic", "sad", "focus")

        // add listener on each tile
        for (i in 0 until gridLayout.childCount) {
            val tile = gridLayout.getChildAt(i)
            tile.isClickable = true
            tile.isFocusable = true
            tile.setOnClickListener {
                val mood = moods[i]
                (activity as? MainActivity)?.moodFromDiscover = mood
                (activity as? MainActivity)?.homeFragment?.let {
                    (activity as MainActivity).showFragment(it)
                }
            }

        }
    }

    override fun onResume() {
        super.onResume()
        // Reset for animation
        resetImages()
        loadImages()
        // start animation
        animateAllElements()
        // stop music from home
        MusicPlayerManager.stopSong()
    }

    override fun onPause() {
        super.onPause()
        resetImages()
    }

    override fun onStop() {
        super.onStop()
        MusicPlayerManager.pauseSong(true)
    }


    /**
     * Makes each child of the GridLayout square by setting height = width.
     */
    private fun makeGridItemsSquare(gridLayout: GridLayout) {
        gridLayout.post {
            for (i in 0 until gridLayout.childCount) {
                val child = gridLayout.getChildAt(i)
                val params = child.layoutParams
                params.height = child.width
                child.layoutParams = params
            }
        }
    }

    /**
     * Loads an image with rounded corners into the given ImageView.
     */
    private fun loadRoundedImage(
        context: Context,
        imageView: ImageView,
        imageRes: Int,
        radiusDp: Float = 16f
    ) {
        val radiusPx = (radiusDp * context.resources.displayMetrics.density).toInt()
        Glide.with(context)
            .load(imageRes)
            .apply(RequestOptions().transform(RoundedCorners(radiusPx)))
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(imageView)
    }


    /**
     * Loads all images into their corresponding ImageViews.
     */
    private fun loadImages() {
        for (i in imageIds.indices) {
            val imageView = view?.findViewById<ImageView>(imageIds[i])
            imageView?.let { loadRoundedImage(requireContext(), it, drawables[i]) }
        }
    }


    /**
     * Clears all images from the ImageViews and releases Glide resources.
     */
    private fun resetImages() {
        for (id in imageIds) {
            val imageView = view?.findViewById<ImageView>(id)
            imageView?.let {
                Glide.with(this).clear(it)
                it.setImageDrawable(null)
            }
        }
    }

    /**
     * Animates the title, subtitle, search bar, and grid items with fade and translation effects.
     */
    private fun animateAllElements() {
        val animDuration = 400L
        val animDelay = 150L
        val translationY = 50f

        titleTextView.alpha = 0f
        titleTextView.translationY = -translationY
        titleTextView.animate().alpha(1f).translationY(0f).setDuration(animDuration)
            .setStartDelay(0).start()

        subtitleTextView.alpha = 0f
        subtitleTextView.translationY = -translationY
        subtitleTextView.animate().alpha(1f).translationY(0f).setDuration(animDuration)
            .setStartDelay(animDelay).start()

        searchBar.alpha = 0f
        searchBar.translationY = -translationY
        searchBar.animate().alpha(1f).translationY(0f).setDuration(animDuration)
            .setStartDelay(animDelay * 2).start()

        // GridLayout : 2 by 2
        val childrenPerRow = 2
        val rowDelay = animDelay * 1.5 // delay between line
        var currentRow = 0

        for (i in 0 until gridLayout.childCount) {
            val child = gridLayout.getChildAt(i)
            child.alpha = 0f
            child.translationY = -translationY
            child.scaleX = 0.8f
            child.scaleY = 0.8f

            val delay = animDelay * 3 + (currentRow * rowDelay)

            child.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(animDuration)
                .setStartDelay(delay.toLong())
                .start()

            // Move to the next line after 2 tiles
            if ((i + 1) % childrenPerRow == 0) {
                currentRow++
            }
        }
    }


    override fun onSwipeRight() {
        // Rien
    }

    override fun onSwipeLeft() {
        (activity as? MainActivity)?.showFragment(
            (activity as MainActivity).homeFragment
        )
    }

    /**
     * Displays the search results in the LinearLayout.
     *
     * @param results List of songs to display.
     * @param isEmpty Boolean indicating if the search field is empty.
     *
     * For each song:
     * - Inflates a view from item_search_result layout.
     * - Sets the title, artist, album, and cover image.
     * - Configures play/pause and like buttons with their behavior.
     * - Adds a gray divider between items (except the last one).
     *
     * Also handles showing the "no results" text and animating the scroll view expansion/collapse.
     */

    private fun displaySearchResults(results: List<Song>, isEmpty: Boolean) {
        val container = view?.findViewById<LinearLayout>(R.id.searchResultsContainer)
        container?.removeAllViews() // reset old results

        results.forEachIndexed { index, song ->
            val itemView = layoutInflater.inflate(R.layout.item_search_result, container, false)

            val titleView = itemView.findViewById<TextView>(R.id.titleSearch)
            val artistView = itemView.findViewById<TextView>(R.id.authorSearch)
            val albumView = itemView.findViewById<TextView>(R.id.albumSearch)
            val coverView = itemView.findViewById<ImageView>(R.id.imageCoverSearch)
            val playButton = itemView.findViewById<ImageButton>(R.id.buttonPlay)
            val likeButton = itemView.findViewById<ImageButton>(R.id.buttonLikeSearch)



            updateLikeIcon(song, likeButton, true)


            fun toggleSong() {
                db.addListenedSong(song)
                val currentSongId = MusicPlayerManager.getCurrentSong()
                val isPlaying = MusicPlayerManager.getIsRun()

                // Reset buttons of other songs
                container?.children?.forEach { child ->
                    val btn = child.findViewById<ImageButton>(R.id.buttonPlay)
                    if (btn != playButton) btn?.setImageResource(R.drawable.ic_play)
                }

                if (currentSongId == song.trackId) {
                    if (isPlaying) {
                        // Same song → pause
                        MusicPlayerManager.pauseSong()
                        playButton.setImageResource(R.drawable.ic_play)
                    } else {
                        // Same song → repeat
                        MusicPlayerManager.resumeSong()
                        playButton.setImageResource(R.drawable.ic_pause)
                    }
                } else {
                    // New song → play
                    MusicPlayerManager.playSong(song.previewUrl, song.trackId)
                    playButton.setImageResource(R.drawable.ic_pause)
                }
            }


            titleView.text = song.trackName
            artistView.text = song.artistName
            albumView.text = song.collectionName

            // load image with border radius
            val radiusDp = 16f
            val radiusPx = (radiusDp * requireContext().resources.displayMetrics.density).toInt()
            Glide.with(requireContext())
                .load(song.artworkUrl100)
                .apply(RequestOptions().transform(RoundedCorners(radiusPx)))
                .transition(DrawableTransitionOptions.withCrossFade())
                .skipMemoryCache(true)
                .into(coverView)

            // Assign listeners
            itemView.setOnClickListener { toggleSong() }
            playButton.setOnClickListener { toggleSong() }
            likeButton.setOnClickListener { updateLikeIcon(song, likeButton) }
            closeButton.setOnClickListener { searchEditText.text.clear() }

            arrowBottomSearch.setOnClickListener {
                // change size
                val heightDp = if (!scrollViewOpen) 400 else 200
                val newHeightPx = (heightDp * resources.displayMetrics.density).toInt()

                // get current height
                val startHeight = layoutResultWithArrow.height

                // create animation
                val animator = ValueAnimator.ofInt(startHeight, newHeightPx)
                animator.duration = 300


                animator.addUpdateListener { animation ->
                    val value = animation.animatedValue as Int
                    val params = layoutResultWithArrow.layoutParams
                    params.height = value
                    layoutResultWithArrow.layoutParams = params
                }

                animator.start()

                // change direction
                arrowBottomSearch.animate().rotation(if (!scrollViewOpen) 180f else 0f)
                    .setDuration(300).start()

                scrollViewOpen = !scrollViewOpen
            }




            container?.addView(itemView)

            // add grey line except for the last item
            if (index < results.size - 1) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1 // height
                    ).apply {
                        setMargins(0, 4, 0, 4)
                    }
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.line))
                }
                container?.addView(divider)
            }
        }

        if (results.isEmpty() && !isEmpty) {
            noResultsText.visibility = View.VISIBLE
            layoutResultWithArrow.visibility = View.GONE
        } else if (results.isNotEmpty()) {
            layoutResultWithArrow.visibility = View.VISIBLE
            noResultsText.visibility = View.GONE
        } else {
            layoutResultWithArrow.visibility = View.GONE
            noResultsText.visibility = View.GONE
        }
    }


    /**
     * Updates a song's like icon.
     * @param songId
     * @param icon the ImageButton button to update
     * @param init if true, just initialize the color according to the DB, otherwise toggle
     */
    private fun updateLikeIcon(song: Song, icon: ImageButton, init: Boolean = false) {
        var liked = db.isSongLiked(song.trackId)

        if (!init) {
            // add in db
            db.addListenedSong(song)
            // toggle if not just for initialization
            db.addLikedSong(song.trackId, !liked)
            liked = !liked
        }

        // met la couleur correcte
        val colorRes = if (liked) R.color.green else R.color.icon
        ImageViewCompat.setImageTintList(
            icon,
            ColorStateList.valueOf(ContextCompat.getColor(icon.context, colorRes))
        )
    }


    /**
     * Hides the close button
     * Starts the music loader animation
     */
    private fun startMusicLoader() {
        closeButton.visibility = View.GONE
        musicLoader.apply {
            // Fade in
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(100).start()
        }

        loaderAnimator = ObjectAnimator.ofFloat(musicLoader, "rotation", 0f, 360f).apply {
            duration = 1500
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    /**
     * Stops the music loader animation
     * Shows the close button after the fade-out
     */
    private fun stopMusicLoader() {
        // Fade out
        musicLoader.animate().alpha(0f).setDuration(100).withEndAction {
            loaderAnimator?.cancel()
            musicLoader.visibility = View.GONE
            closeButton.postDelayed({
                closeButton.visibility = View.VISIBLE
            }, 0)
        }.start()

    }

}
