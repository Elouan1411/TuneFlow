package com.example.tuneflow.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.tuneflow.MainActivity
import com.example.tuneflow.R
import com.example.tuneflow.ui.utils.SwipeListener

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_discover, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleTextView = view.findViewById(R.id.title_discover)
        subtitleTextView = view.findViewById(R.id.subtitle_discover)
        searchBar = view.findViewById(R.id.search_bar)
        gridLayout = view.findViewById(R.id.gridLayoutDiscover)

        makeGridItemsSquare(gridLayout)
    }

    override fun onResume() {
        super.onResume()
        // Reset for animation
        resetImages()
        loadImages()
        // start animation
        animateAllElements()
    }

    override fun onPause() {
        super.onPause()
        resetImages()
    }



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

    private fun loadRoundedImage(context: Context, imageView: ImageView, imageRes: Int, radiusDp: Float = 16f) {
        val radiusPx = (radiusDp * context.resources.displayMetrics.density).toInt()
        Glide.with(context)
            .load(imageRes)
            .apply(RequestOptions().transform(RoundedCorners(radiusPx)))
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(imageView)
    }

    private fun loadImages() {
        for (i in imageIds.indices) {
            val imageView = view?.findViewById<ImageView>(imageIds[i])
            imageView?.let { loadRoundedImage(requireContext(), it, drawables[i]) }
        }
    }

    private fun resetImages() {
        for (id in imageIds) {
            val imageView = view?.findViewById<ImageView>(id)
            imageView?.let {
                Glide.with(this).clear(it)
                it.setImageDrawable(null)
            }
        }
    }

    private fun animateAllElements() {
        val animDuration = 400L
        val animDelay = 150L
        val translationY = 50f

        titleTextView.alpha = 0f
        titleTextView.translationY = -translationY
        titleTextView.animate().alpha(1f).translationY(0f).setDuration(animDuration).setStartDelay(0).start()

        subtitleTextView.alpha = 0f
        subtitleTextView.translationY = -translationY
        subtitleTextView.animate().alpha(1f).translationY(0f).setDuration(animDuration).setStartDelay(animDelay).start()

        searchBar.alpha = 0f
        searchBar.translationY = -translationY
        searchBar.animate().alpha(1f).translationY(0f).setDuration(animDuration).setStartDelay(animDelay*2).start()

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
}
