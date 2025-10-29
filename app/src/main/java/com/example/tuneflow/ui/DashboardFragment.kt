package com.example.tuneflow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.tuneflow.MainActivity
import com.example.tuneflow.R
import com.example.tuneflow.player.MusicPlayerManager
import com.example.tuneflow.ui.utils.SwipeListener
import kotlin.collections.listOf

class DashboardFragment : Fragment(), SwipeListener {
    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var layoutStat1: LinearLayout
    private lateinit var layoutStat2: LinearLayout
    private lateinit var layoutStat3: LinearLayout
    private lateinit var layoutStat4: LinearLayout
    private lateinit var layoutStat5: LinearLayout
    private lateinit var statsLayouts : List<LinearLayout>


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

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


    }

    override fun onSwipeRight() {
        (activity as? MainActivity)?.showFragment(
            (activity as MainActivity).homeFragment
        )
    }

    override fun onSwipeLeft() {
        // do nothing
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

}