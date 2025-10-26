package com.example.tuneflow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: SwipeAdapter

    private var pagesRemoved = 0


    companion object {
        private const val MAX_PAGES_HISTORY = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)

        val adapter = SwipeAdapter(mutableListOf())
        viewPager.adapter = adapter

        fetchSongs(adapter, viewPager)

        // listener on swipe
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val song = adapter.getSongAt(position)
                song?.let {
                    MusicPlayerManager.playSong(it.previewUrl)
                }

            }
        })


        return view
    }

    private fun fetchSongs(adapter: SwipeAdapter, viewPager: ViewPager2) {
        // Fetch songs safely
        lifecycleScope.launch {
            try {
                val songs = ApiClient.api.getSongs().results
                songs.forEach { song ->
                    adapter.addPage(song)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("HomeFragment", "API error: ${e.message}")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        MusicPlayerManager.pauseSong(true)
    }

    override fun onResume() {
        super.onResume()
        if (MusicPlayerManager.getIsRun()){
            MusicPlayerManager.resumeSong()
        }
    }




}
