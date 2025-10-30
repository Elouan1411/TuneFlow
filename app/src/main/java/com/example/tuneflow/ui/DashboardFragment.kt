package com.example.tuneflow.ui

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.tuneflow.MainActivity
import com.example.tuneflow.R
import com.example.tuneflow.data.Song
import com.example.tuneflow.db.TuneFlowDatabase
import com.example.tuneflow.player.MusicPlayerManager
import com.example.tuneflow.ui.utils.generalTools
import java.text.DecimalFormat
import kotlin.collections.listOf
import kotlin.sequences.forEach

class DashboardFragment : Fragment() {
    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var layoutStat1: LinearLayout
    private lateinit var layoutStat2: LinearLayout
    private lateinit var layoutStat3: LinearLayout
    private lateinit var layoutStat4: LinearLayout
    private lateinit var layoutStat5: LinearLayout
    private lateinit var statsLayouts: List<LinearLayout>

    private val db: TuneFlowDatabase
        get() = (activity as MainActivity).db


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleTextView = view.findViewById(R.id.title_discover)
        subtitleTextView = view.findViewById(R.id.subtitle_discover)
        layoutStat1 = view.findViewById(R.id.layoutStat1)
        layoutStat2 = view.findViewById(R.id.layoutStat2)
        layoutStat3 = view.findViewById(R.id.layoutStat3)
        layoutStat4 = view.findViewById(R.id.layoutStat4)
        layoutStat5 = view.findViewById(R.id.layoutStat5)
        statsLayouts = listOf(layoutStat1, layoutStat2, layoutStat3, layoutStat4, layoutStat5)

        // display stats
        // --- Total listened ---
        val totalListened = db.getTotalListeningCount()
        val textNbSongs = view.findViewById<TextView>(R.id.textNbSongs)
        val textTitleListened = view.findViewById<TextView>(R.id.textTitleListened)

        textNbSongs.text = formatNumber(totalListened)
        textTitleListened.text = if (totalListened < 2) "Titre écouté" else "Titres écoutés"

        // --- Total liked ---
        val totalLiked = db.getLikedCount()
        val textNbLiked = view.findViewById<TextView>(R.id.textNbLiked)
        textNbLiked.text = formatNumber(totalLiked)
        val textSongLiked = view.findViewById<TextView>(R.id.textSongLiked)
        textSongLiked.text = if (totalLiked < 2) "Morceau liké" else "Morceaux likés"

        // --- Artist discovered ---
        val totalAuthors = db.getDistinctArtistsCount()
        val textNbAuthor = view.findViewById<TextView>(R.id.textNbAuthor)
        textNbAuthor.text = formatNumber(totalAuthors)
        val textAuthorDiscover = view.findViewById<TextView>(R.id.textAuthorDiscover)
        textAuthorDiscover.text =
            if (totalAuthors < 2) "Artiste découvert" else "Artistes découverts"

        // --- Top artiste ---
        val textAuthorName = view.findViewById<TextView>(R.id.textAuthorName)
        textAuthorName.text = db.getTopOneArtist()


        // display recent like
        displayRecentLike()
    }

    override fun onResume() {
        super.onResume()
        // start animation
        animateAllElements()
        // stop music from home
        MusicPlayerManager.stopSong()
    }

    override fun onStop() {
        super.onStop()
        MusicPlayerManager.pauseSong(true)
    }


    /**
     * Animates the title, subtitle, and grid items with fade and translation effects.
     */
    private fun animateAllElements() {
        val animDuration = 400L
        val animDelay = 150L
        val translationY = 50f

        // Title animation
        titleTextView.alpha = 0f
        titleTextView.translationY = -translationY
        titleTextView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(animDuration)
            .setStartDelay(0)
            .start()

        // Subtitle animation
        subtitleTextView.alpha = 0f
        subtitleTextView.translationY = -translationY
        subtitleTextView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(animDuration)
            .setStartDelay(animDelay)
            .start()

        // Stats animation two by two
        val childrenPerRow = 2
        val rowDelay = animDelay * 1.5
        var currentRow = 0

        statsLayouts.forEachIndexed { index, child ->
            child.alpha = 0f
            child.translationY = -translationY
            child.scaleX = 0.8f
            child.scaleY = 0.8f

            val delay = animDelay * 2 + (currentRow * rowDelay)

            child.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(animDuration)
                .setStartDelay(delay.toLong())
                .start()

            // Next row after 2 items
            if ((index + 1) % childrenPerRow == 0) currentRow++
        }
    }


    /**
     * Formats a number string with commas as thousands separators.
     * @param numberStr The input number as a string ("1247")
     * @return Formatted number with commas ("1,247")
     */
    fun formatNumber(number: Int): String {
        return try {
            val formatter = DecimalFormat("#,###")
            formatter.format(number)
        } catch (e: NumberFormatException) {
            number.toString() // return original if input is not a valid number
        }
    }

    /**
     * Displays the search results in the LinearLayout.
     *
     * @param results List of songs to display.
     *
     * For each song:
     * - Sets the title, artist, album, and cover image.
     * - Configures play/pause and like buttons with their behavior.
     * - Adds a gray divider between items (except the last one).
     */
    private fun displaySearchResults(results: List<Song>) {
        val container = requireView().findViewById<LinearLayout>(R.id.lastLikeContainer)
        container.removeAllViews() // reset old results
        results.forEachIndexed { index, song ->
            val itemView = layoutInflater.inflate(R.layout.item_search_result, container, false)

            val titleView = itemView.findViewById<TextView>(R.id.titleSearch)
            val artistView = itemView.findViewById<TextView>(R.id.authorSearch)
            val albumView = itemView.findViewById<TextView>(R.id.albumSearch)
            val coverView = itemView.findViewById<ImageView>(R.id.imageCoverSearch)
            val playButton = itemView.findViewById<ImageButton>(R.id.buttonPlay)
            val likeButton = itemView.findViewById<ImageButton>(R.id.buttonLikeSearch)
            val moreButton = itemView.findViewById<ImageButton>(R.id.buttonMore)

            // create popupMenu on moreButton
            val popup = PopupMenu(moreButton.context, moreButton)
            popup.inflate(R.menu.menu_options_song)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.addToPlaylist -> {
                        val bottomSheet = PlaylistBottomSheet(
                            song
                        )
                        bottomSheet.show(parentFragmentManager, "playlistBottomSheet")
                        true
                    }

                    R.id.listenOnAppleMusic -> {
                        generalTools.redirectOnAppleMusic(requireContext(), song.trackViewUrl)
                        true
                    }

                    R.id.share -> {
                        generalTools.shareMessage(
                            requireContext(),
                            "🎵 J'ai découvert \"${song.trackName}\" de ${song.artistName} et je te la recommande ! Écoute-la ici : ${song.trackViewUrl}"
                        )
                        true
                    }

                    else -> false
                }
            }

            moreButton.setOnClickListener {
                popup.show()
            }


            generalTools.updateLikeIcon(song, likeButton, db, true)


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
            itemView.setOnClickListener { generalTools.toggleSong(db, song, container, playButton) }
            playButton.setOnClickListener {
                generalTools.toggleSong(
                    db,
                    song,
                    container,
                    playButton
                )
            }
            likeButton.setOnClickListener { generalTools.updateLikeIcon(song, likeButton, db) }





            container.addView(itemView)

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
                container.addView(divider)
            }
        }


    }

    /**
     * Displays the 10 most recently liked songs by fetching their IDs and showing the corresponding results.
     */
    private fun displayRecentLike() {
        // get top 10 songId liked
        val listSongId: List<Long> = db.getRecentLikedSongs()
        val listSong = listSongId.mapNotNull { db.getSongFromDb(it) }.toMutableList()
        displaySearchResults(listSong)
    }

}