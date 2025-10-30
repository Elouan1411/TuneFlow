package com.example.tuneflow.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.children
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.tuneflow.MainActivity
import com.example.tuneflow.R
import com.example.tuneflow.data.PlaylistInfo
import com.example.tuneflow.data.Song
import com.example.tuneflow.db.TuneFlowDatabase
import com.example.tuneflow.player.MusicPlayerManager
import com.example.tuneflow.ui.utils.generalTools

class PlaylistsFragment : Fragment() {
    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var gridLayoutPlaylist: GridLayout
    private lateinit var buttonWantCreatePlaylist: RelativeLayout
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
        titleTextView = view.findViewById(R.id.titleFragmentPlaylist)
        subtitleTextView = view.findViewById(R.id.subtitleFragmentPlaylist)
        gridLayoutPlaylist = view.findViewById(R.id.gridLayoutPlaylist)
        buttonWantCreatePlaylist = view.findViewById(R.id.buttonWantCreatePlaylist)

        displayPlaylist(view)

        // button create playlist
        view.findViewById<RelativeLayout>(R.id.buttonWantCreatePlaylist).setOnClickListener {
            PopupNewPlaylistFragment { playlistName ->
                db.createPlaylist(playlistName)
                displayPlaylist(view) // refresh
            }.show(parentFragmentManager, "popup_new_playlist")
        }



    }

    override fun onResume() {
        super.onResume()
        // stop music from home
        MusicPlayerManager.stopSong()
        animatePlaylistPage()
    }

    override fun onStop() {
        super.onStop()
        MusicPlayerManager.pauseSong(true)
    }

    /**
     * Displays the image with rounded edges and a gradient for better text readability
     *  @param playlist The playlist information containing the cover image URL.
     *  @param coverImage The ImageView where the cover image will be displayed.
     *  @param view The parent view
     */
    fun displayImage(playlist: PlaylistInfo, coverImage: ImageView, view: View){
        val radiusDp = 16f
        val radiusPx = radiusDp * view.context.resources.displayMetrics.density

        // laod image with border radius
        val coverUrl = playlist.lastSongCoverUrl.takeIf { it.isNotEmpty() }
            ?.replace("100x100", "600x600")
            ?: R.drawable.ic_cover


        Glide.with(this)
            .load(coverUrl)
            .placeholder(ColorDrawable(Color.TRANSPARENT))
            .apply(RequestOptions().transform(RoundedCorners(radiusPx.toInt())))
            .transition(DrawableTransitionOptions.withCrossFade())
            .skipMemoryCache(true)
            .into(coverImage)


        // gradient
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.TRANSPARENT, Color.parseColor("#CC000000"))
        ).apply {
            cornerRadius = radiusPx
        }

        // apply gradient
        coverImage.foreground = gradient
    }

    /**
     * Displays the different playlists in a grid format
     * @param view View
     */
    private fun displayPlaylist(view: View){
        val playlists = db.getPlaylistsInfo()
        var word = if (playlists.size <= 1) "playlist" else "playlists"
        view.findViewById<TextView>(R.id.subtitleFragmentPlaylist).text = "${playlists.size} $word"

        val gridLayout = view.findViewById<GridLayout>(R.id.gridLayoutPlaylist)
        gridLayout.removeAllViews()

        val columnCount = 2
        val rowCount = (playlists.size + columnCount - 1) / columnCount
        gridLayout.columnCount = columnCount
        gridLayout.rowCount = rowCount

        playlists.forEachIndexed { index, playlist ->
            val itemView = layoutInflater.inflate(R.layout.item_playlist, gridLayout, false)

            // Views
            val coverImage = itemView.findViewById<ImageView>(R.id.coverPlaylist)
            val nameText = itemView.findViewById<TextView>(R.id.namePlaylist)
            val nbSongsText = itemView.findViewById<TextView>(R.id.nbSongsPlaylist)

            nameText.text = playlist.name
            word = if (playlist.songCount <= 1) "morceau" else "morceaux"
            nbSongsText.text = "${playlist.songCount} $word"

            displayImage(playlist, coverImage, view)

            // LayoutParams
            val marginDp = 8
            val scale = view.context.resources.displayMetrics.density
            val marginPx = (marginDp * scale).toInt()
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(index % columnCount, 1f)
                rowSpec = GridLayout.spec(index / columnCount)
                setMargins(marginPx, marginPx, marginPx, marginPx)
            }
            itemView.layoutParams = params

            // add listener for open the playlist
            itemView.setOnClickListener {
                InPlaylistBottomSheet(playlist.name, playlist.songCount) {
                    // Callback after delete
                    displayPlaylist(view)
                    animatePlaylistPage()
                }.show(parentFragmentManager, "inPlaylistBottomSheet")
            }
            // long press
            itemView.setOnLongClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.inflate(R.menu.menu_options_playlist)

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.option_delete -> {
                            db.deletePlaylist(playlist.name)
                            displayPlaylist(requireView())
                            true
                        }
                        R.id.option_share -> {
                            generalTools.sharePlaylist(playlist.name, db, requireContext())
                            true
                        }
                        else -> false
                    }
                }

                popup.show()
                true
            }

            gridLayout.addView(itemView)
        }

        // Add an "empty element" if the number of playlists is odd.
        if (playlists.size % 2 != 0) {
            val emptyView = View(context)
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(1, 1f)
                rowSpec = GridLayout.spec(rowCount - 1)
            }
            emptyView.layoutParams = params
            gridLayout.addView(emptyView)
        }
    }


    /**
     * Animates the title, subtitle, and playlist grid items with fade, translation, and scale effects.
     */
    private fun animatePlaylistPage() {
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

        // Button animation
        buttonWantCreatePlaylist.alpha = 0f
        buttonWantCreatePlaylist.translationY = -translationY
        buttonWantCreatePlaylist.animate()
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

        // Animate grid items two by two
        val childrenPerRow = 2
        val rowDelay = (animDelay * 1.5).toLong()
        var currentRow = 0

        val gridChildren = gridLayoutPlaylist.children.toList()
        gridChildren.forEachIndexed { index, child ->
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
                .setStartDelay(delay)
                .start()

            if ((index + 1) % childrenPerRow == 0) currentRow++
        }
    }




}