package com.example.tuneflow

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.example.tuneflow.db.TuneFlowDatabase
import com.example.tuneflow.ui.DashboardFragment
import com.example.tuneflow.ui.DiscoverFragment
import com.example.tuneflow.ui.HomeFragment
import com.example.tuneflow.ui.PlaylistsFragment
import com.example.tuneflow.ui.utils.loadFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    lateinit var gestureDetector: GestureDetector
    lateinit var db: TuneFlowDatabase

    val homeFragment = HomeFragment()
    val dashboardFragment = DashboardFragment()
    val discoverFragment = DiscoverFragment()

    val playlistFragment = PlaylistsFragment()
    var moodFromDiscover: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        db = TuneFlowDatabase(this)


        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)


        // Removes automatic padding applied by insets
//        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { _, insets ->
//            insets
//        }

        // PreLoad fragment and display home
        bottomNavigation.selectedItemId = R.id.nav_home
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, homeFragment)
            .add(R.id.fragment_container, dashboardFragment).hide(dashboardFragment)
            .add(R.id.fragment_container, discoverFragment).hide(discoverFragment)
            .add(R.id.fragment_container, playlistFragment).hide(playlistFragment)
            .commit()

        // Bottom nav actions
        bottomNavigation.setOnItemSelectedListener { page ->
            when (page.itemId) {
                R.id.nav_home -> supportFragmentManager.loadFragment(
                    R.id.fragment_container,
                    HomeFragment()
                )

                R.id.nav_dashboard -> supportFragmentManager.loadFragment(
                    R.id.fragment_container,
                    DashboardFragment()
                )

                R.id.nav_discover -> supportFragmentManager.loadFragment(
                    R.id.fragment_container,
                    DiscoverFragment()
                )

                R.id.nav_playlist -> supportFragmentManager.loadFragment(
                    R.id.fragment_container,
                    PlaylistsFragment()
                )
            }
            true
        }


        // Apply the touch listener to the fragment_container (for horizontal swipe)
        val rootView = findViewById<View>(R.id.fragment_container)
        rootView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()  // call performClick for accessibility
            }
            true
        }
    }


    /**
     * Shows the specified fragment and hides the others,
     * then updates the BottomNavigationView to match.
     *
     * @param fragmentToShow The fragment to display.
     */
    fun showFragment(fragmentToShow: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()

        // Hide all fragments except the one to show
        for (f in listOf(homeFragment, dashboardFragment, discoverFragment, playlistFragment)) {
            if (f == fragmentToShow) transaction.show(f)
            else transaction.hide(f)
        }
        transaction.commit()

        // Update BottomNavigationView
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        when (fragmentToShow) {
            homeFragment -> bottomNavigation.selectedItemId = R.id.nav_home
            dashboardFragment -> bottomNavigation.selectedItemId = R.id.nav_dashboard
            discoverFragment -> bottomNavigation.selectedItemId = R.id.nav_discover
            playlistFragment -> bottomNavigation.selectedItemId = R.id.nav_playlist
        }
    }


}

