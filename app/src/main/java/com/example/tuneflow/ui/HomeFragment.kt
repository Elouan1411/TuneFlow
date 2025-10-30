package com.example.tuneflow.ui

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.example.tuneflow.MainActivity
import com.example.tuneflow.player.MusicPlayerManager
import com.example.tuneflow.R
import com.example.tuneflow.data.Song
import com.example.tuneflow.db.TuneFlowDatabase
import com.example.tuneflow.ui.adapters.SwipeAdapter
import com.example.tuneflow.network.ApiClient
import kotlinx.coroutines.launch
import java.io.IOException

class HomeFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: SwipeAdapter

    private var fromMood: Boolean = false
    private var moodDiscover: String = ""
    private lateinit var loaderImage: ImageView
    private lateinit var layoutLoader: RelativeLayout
    private lateinit var layoutError: RelativeLayout
    private var loaderAnimator: ObjectAnimator? = null

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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)
        viewPager = view.findViewById<ViewPager2>(R.id.viewPager)

        layoutLoader = view.findViewById<RelativeLayout>(R.id.layoutLoader)
        loaderImage = view.findViewById<ImageView>(R.id.loaderImage)

        layoutError = view.findViewById<RelativeLayout>(R.id.layoutError)

        displayLoader(view)

        val adapter = SwipeAdapter(mutableListOf(), db, this, parentFragmentManager)
        viewPager.adapter = adapter

        val activity = requireActivity() as MainActivity
        activity.moodFromDiscover?.let { mood ->
            if (mood.isEmpty()) {
                fromMood = false
            } else {

                fromMood = true
                moodDiscover = mood
            }
            activity.moodFromDiscover = null // reset si besoin
        }

        fetchSongs(adapter)

        // listener on swipe
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)


                val song = adapter.getSongAt(position)
                song?.let {
                    MusicPlayerManager.playSong(it.previewUrl, it.trackId)
                    db.addListenedSong(song)
                }


                // Preload new songs when you get to 5 songs from the end
                val threshold = 5
                if (adapter.itemCount - position <= threshold) {
                    fetchSongs(adapter)
                }
            }
        })

        view.findViewById<AppCompatButton>(R.id.retryButton).setOnClickListener {
            (activity as? MainActivity)?.homeFragment?.let {
                (activity as MainActivity).showFragment(it)
            }
        }
        return view
    }


    /**
     * Fetches songs from the API and adds them via the adapter.
     *
     * Three different scenarios:
     * - Fetch songs based on the chosen mood if fromMood = true
     * - Fetch random songs if the user's preference database is not yet sufficiently populated
     * - Fetch songs according to the user's preferences otherwise
     *
     * Ensures that songs already listened to are not suggested again.
     * The selected songs are then shuffled and provided to the adapter.
     *
     * @param adapter The SwipeAdapter used by the ViewPager2 to display the songs.
     */

    private fun fetchSongs(adapter: SwipeAdapter) {
        val NB_FOR_EACH_SEARCH = 2
        val YEAR_GROUP_SIZE = 5
        val THRESHOLD_DISCOVER = 10
        // Fetch songs safely
        lifecycleScope.launch {
            try {
                var discover: Boolean = db.getLikedCount() < THRESHOLD_DISCOVER

                val search: List<String> = if (fromMood) {
                    buildSearchMood()
                } else if (discover) {
                    buildSearchTermsDiscover()
                } else {
                    buildSearchTerms()
                }


                var songsSelected = mutableListOf<Song>()

                // We do not scan the last element (the date)
                // It will be used for a second filter for the style chosen at random
                for (j in 0 until search.size - 1) {
                    val songs: List<Song> = try {
                        val response = ApiClient.api.getSongs(ApiClient.cleanUrlForApi(search[j]))
                        if (response.isSuccessful) {
                            response.body()?.results ?: emptyList()
                        } else {
                            Log.e("API", "Erreur serveur : ${response.code()}")
                            layoutError.visibility = View.VISIBLE
                            layoutLoader.visibility = View.GONE
                            viewPager.visibility = View.GONE
                            return@launch
                        }
                    } catch (e: IOException) {
                        Log.e("API", "Erreur réseau : ${e.message}")
                        layoutError.visibility = View.VISIBLE
                        layoutLoader.visibility = View.GONE
                        viewPager.visibility = View.GONE
                        return@launch
                    } catch (e: Exception) {
                        Log.e("API", "Erreur inconnue : ${e.message}")
                        layoutError.visibility = View.VISIBLE
                        layoutLoader.visibility = View.GONE
                        viewPager.visibility = View.GONE
                        return@launch
                    }


                    var find = 0
                    var i = 0
                    // we select two sounds that the user has never listened to
                    while (find != NB_FOR_EACH_SEARCH && i < songs.size) {
                        if (!isSongValid(songs[i])) {
                            i++
                            continue
                        }
                        if (!adapter.containsSong(songs[i]) && !db.soundAlreadyListened(songs[i].trackId)) {
                            // for le random style
                            if (j == search.size - 2 && search.size == 6) {
                                // we check that the year corresponds
                                val year = search.last().toInt() // the chosen year groupe
                                val releaseDate = songs[i].releaseDate
                                val yearSong = releaseDate?.substringBefore("-")
                                    ?.toIntOrNull() // safe conversion
                                if (yearSong != null && yearSong in (year - YEAR_GROUP_SIZE)..year) {
                                    songsSelected.add(songs[i])
                                    find++
                                }

                            } else {
                                songsSelected.add(songs[i])
                                find++
                            }

                        }
                        i++
                    }
                    if (find != NB_FOR_EACH_SEARCH) {
                        // Take enough random songs to complete the quota of 2
                        val needed = NB_FOR_EACH_SEARCH - find
                        val randomSongs = songs.shuffled().take(needed)
                        songsSelected.addAll(randomSongs)
                    }
                }

                val res = songsSelected.shuffled()
                // delete loader
                layoutLoader.visibility = View.GONE
                // we insert the selected sounds
                res.forEach { song ->
                    adapter.addPage(song)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("API", "API error: ${e.message}")
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

    /**
     * Generates a list of keywords based on the current mood
     * @return a list of 5 words synonymous with the current mood (moodDiscover)
     */
    private fun buildSearchMood(): List<String> {
        return moodKeywords[moodDiscover]!!.shuffled().take(5)
    }

    /**
     * This function is used until the user has liked enough of their content.
     * It replaces buildSearchTerms (because buildSearchTerms requires the user's database to be at least partially populated).
     * It is only used at the beginning, when the user has just installed the application.
     * @return A list of 5 keywords to be used in the fetchSongs function.
     */
    private fun buildSearchTermsDiscover(): List<String> {
        // take 5 different style
        // todo:ask style and author at first
        return globalStyles.shuffled().take(5)
    }

    /**
     * Algorithm that generates words to send to the API based on user preferences calculated with
     * likes (era, musical genre, artists) but also by proposing new styles so that
     * the user discovers new things and does not get stuck in one style
     * @return A list containing 5 keywords and 1 date, which will be used in the fetchSongs function
     */
    private fun buildSearchTerms(): List<String> {
        val styles = db.getTopStyles(5)
        val authors = db.getTopAuthors(5)


        val baseWeight = 0.2
        val weightPreferences = listOf(0.4, 0.3, 0.25, 0.25, 0.25)

        // List of main candidates (excluding random style)
        val candidates = mutableListOf<Pair<String, Double>>()

        // Add styles and authors
        for (s in styles.indices) {
            candidates.add(styles[s] to weightPreferences[s])
        }

        for (a in authors.indices) {
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


    /**
     * Checks if a Song has all required information.
     * @return true if no essential fields are empty, false otherwise
     */
    private fun isSongValid(song: Song): Boolean {
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

    /**
     * Get the currently playing mood
     */
    fun getMood(): String {
        return moodDiscover
    }

    /**
     * Remove search by mood
     */
    fun stopMood() {
        (activity as? MainActivity)?.moodFromDiscover = ""
        (activity as? MainActivity)?.discoverFragment?.let {
            (activity as MainActivity).showFragment(it)
        }
    }

    /**
     * Displays a rotating vinyl loader.
     * Sets the loader image, applies a color tint, fades it in,
     * and starts an infinite rotation animation.
     * @param view
     */
    fun displayLoader(view: View) {
        // display loader
        Glide.with(view.context)
            .load(R.drawable.ic_vinyl)
            .apply(RequestOptions().transform(CircleCrop()))
            .into(loaderImage)
        val color = ContextCompat.getColor(view.context, R.color.subtitle)
        ImageViewCompat.setImageTintList(loaderImage, ColorStateList.valueOf(color))
        loaderImage.apply {
            // Fade in
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(100).start()
        }

        loaderAnimator = ObjectAnimator.ofFloat(loaderImage, "rotation", 0f, 360f).apply {
            duration = 1500
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }


}