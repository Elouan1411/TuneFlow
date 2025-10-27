package com.example.tuneflow.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SearchEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.tuneflow.MainActivity
import com.example.tuneflow.player.MusicPlayerManager
import com.example.tuneflow.R
import com.example.tuneflow.data.Song
import com.example.tuneflow.db.TuneFlowDatabase
import com.example.tuneflow.ui.adapters.SwipeAdapter
import com.example.tuneflow.network.ApiClient
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class HomeFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: SwipeAdapter

    private var pagesRemoved = 0
    private val globalStyles = listOf(
        "pop", "rock", "rap", "hip hop", "electro", "indie", "jazz",
        "classical", "metal", "rnb", "reggae", "techno", "house", "funk",
        "country", "soul", "punk", "ambient", "blues", "trap", "afrobeat",
        "kpop", "dance", "disco", "latin"
    )


    private val moodKeywords: Map<String, List<String>> = mapOf(
        "chill" to listOf(
            "chill", "relax", "mellow", "calm", "smooth", "ambient",
            "focus", "study", "soft", "easy", "peaceful", "serene",
            "meditation", "lounge", "gentle", "deep"
        ),
        "workout" to listOf(
            "workout", "gym", "training", "pump", "energy", "upbeat",
            "dance", "motivation", "cardio", "fitness", "hype", "party",
            "adrenaline", "active", "power", "run", "move", "intense", "sport"
        ),
        "romantic" to listOf(
            "romantic", "love", "lover", "date", "passion", "intimate",
            "slow", "heart", "dreamy", "cozy", "cuddle", "affection",
            "sweetheart", "couple", "devotion", "romance", "tender", "desire"
        ),
        "sad" to listOf(
            "sad", "melancholy", "heartbreak", "lonely", "blue", "tear",
            "emotional", "sorrow", "nostalgia", "reflection", "gloomy",
            "moody", "bittersweet", "despair", "regret", "longing",
            "melancholic", "aching"
        ),
        "happy" to listOf(
            "happy", "joyful", "fun", "upbeat", "cheerful", "sunny",
            "dance", "party", "smile", "positive", "energetic", "carefree",
            "bright", "lively", "playful", "celebration", "good-vibes"
        ),
        "focus" to listOf(
            "focus", "study", "concentration", "calm", "ambient",
            "instrumental", "brain", "productivity", "work", "deep",
            "minimal", "coding", "quiet", "thinking", "reading",
            "meditation", "soft", "relaxed"
        )
    )

    private val db: TuneFlowDatabase
        get() = (activity as MainActivity).db


    companion object {
        private const val MAX_PAGES_HISTORY = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)

        val adapter = SwipeAdapter(mutableListOf(), db)
        viewPager.adapter = adapter

        fetchSongs(adapter, viewPager)
        //TODO add loader animate

        // listener on swipe
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val song = adapter.getSongAt(position)
                song?.let {
                    MusicPlayerManager.playSong(it.previewUrl, it.trackId)
                    db.addListenedSong(song)
                }
                // increment db
                db.incrementDiscoverSongs()


                // Preload new songs when you get to 5 songs from the end
                val threshold = 5
                if (adapter.itemCount - position <= threshold) {
                    fetchSongs(adapter, viewPager)
                }
            }
        })
        return view
    }

    private fun fetchSongs(adapter: SwipeAdapter, viewPager: ViewPager2) {
        val NB_FOR_EACH_SEARCH = 2
        val YEAR_GROUP_SIZE = 5
        val THRESHOLD_DISCOVER = 10
        // Fetch songs safely
        lifecycleScope.launch {
            try {
                var discover:Boolean = db.getLikedCount() < THRESHOLD_DISCOVER
                val search: List<String> = if (discover) {
                    buildSearchTermsDiscover()
                } else {
                    buildSearchTerms()
                }




                var songsSelected = mutableListOf<Song>()

                // We do not scan the last element (the date)
                // It will be used for a second filter for the style chosen at random
                for (j in 0 until search.size - 1) {
                    val songs = ApiClient.api.getSongs(cleanUrlForApi(search[j])).results


                    var find = 0
                    var i = 0
                    // we select two sounds that the user has never listened to
                    while (find != NB_FOR_EACH_SEARCH && i < songs.size) {
                        if (!isSongValid(songs[i])){
                            i++
                            continue
                        }
                        if (!adapter.containsSong(songs[i]) && !db.soundAlreadyListened(songs[i].trackId)) {
                            // for le random style
                            if (j == search.size - 2 && !discover){
                                // we check that the year corresponds
                                val year = search.last().toInt() // the chosen year groupe
                                val releaseDate = songs[i].releaseDate
                                val yearSong = releaseDate?.substringBefore("-")?.toIntOrNull() // safe conversion
                                if (yearSong != null && yearSong in (year - YEAR_GROUP_SIZE)..year) {
                                    songsSelected.add(songs[i])
                                    find++
                                }

                            }else{
                                songsSelected.add(songs[i])
                                find++
                            }

                        }
                        i++
                    }
                    if (find != NB_FOR_EACH_SEARCH){
                        // Take enough random songs to complete the quota of 2
                        val needed = NB_FOR_EACH_SEARCH - find
                        val randomSongs = songs.shuffled().take(needed)
                        songsSelected.addAll(randomSongs)
                    }
                }

                val res = songsSelected.shuffled()

                // we insert the selected sounds
                res.forEach { song ->
                    adapter.addPage(song)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("myDebug", "API error: ${e.message}")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        MusicPlayerManager.pauseSong(true)
    }

    override fun onResume() {
        super.onResume()
        if (MusicPlayerManager.getIsRun()) {
            MusicPlayerManager.resumeSong()
        }
    }

    // To fill the database at the beginning
    fun buildSearchTermsDiscover(): List<String> {
        // take 5 different style
        // todo:ask style and author at first
        return globalStyles.shuffled().take(5)
    }

    // suggestion algorithm
    fun buildSearchTerms(): List<String> {
        val styles = db.getTopStyles(5)
        val authors = db.getTopAuthors(5)


        val baseWeight = 0.2
        val weightPreferences = listOf(0.4, 0.3, 0.25, 0.25, 0.25)

        // List of main candidates (excluding random style)
        val candidates = mutableListOf<Pair<String, Double>>()

        // Add styles and authors
        for (s in styles.indices){
            candidates.add(styles[s] to weightPreferences[s])
        }

        for (a in authors.indices){
            candidates.add(authors[a] to weightPreferences[a])
        }

        // Adding 3 styles and 3 authors that the user has already liked
        val fiveStylesLiked = db.getTopStyles(-1).shuffled().take(3)
        for (s in fiveStylesLiked) candidates.add(s to baseWeight)
        val fiveAuthorsLiked = db.getTopAuthors(-1).shuffled().take(3)
        for (s in fiveAuthorsLiked) candidates.add(s to baseWeight)


        // Slight random variation for diversity
        val mixed = candidates.map { (term, w) ->
            val variation = 1 + (Math.random() - 0.5) * 0.2
            term to w * variation
        }

        // Select 3 distinct element
        var selected = mixed
            .sortedByDescending { it.second }
            .map { it.first.replace(" ", "+") }
            .distinct()
            .take(4)
            .toMutableList()

        selected.shuffle()

        // Add one randomStyle
        val randomStyle = globalStyles.random()
        selected.add(randomStyle)


        // take the best year
        val selectedYear = db.getTopYearGroups(1).random().toString()


        selected.add(selectedYear)
        return selected
    }


    fun cleanUrlForApi(term: String): String {
        // replace space with +
        val withPluses = term.replace(" ", "+")
        // Removes all unauthorized characters
        return withPluses.replace(Regex("[^a-zA-Z0-9.+\\-_*]"), "")
    }

    /**
     * Checks if a Song has all required information.
     * @return true if no essential fields are empty, false otherwise
     */
    fun isSongValid(song: Song): Boolean {
        return listOf(
            song.artistName,
            song.collectionName,
            song.trackName,
            song.artistViewUrl,
            song.collectionViewUrl,
            song.trackViewUrl,
            song.previewUrl,
            song.artworkUrl60,
            song.artworkUrl100,
            song.releaseDate,
            song.country,
            song.primaryGenreName
        ).all { !it.isNullOrBlank() } // true if is valid
    }



}