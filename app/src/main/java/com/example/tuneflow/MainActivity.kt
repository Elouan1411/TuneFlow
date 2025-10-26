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
import com.example.tuneflow.ui.utils.SwipeListener
import com.example.tuneflow.ui.utils.loadFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
class MainActivity : AppCompatActivity() {

    lateinit var gestureDetector: GestureDetector
    lateinit var db: TuneFlowDatabase

    val homeFragment = HomeFragment()
    val dashboardFragment = DashboardFragment()
    val discoverFragment = DiscoverFragment()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        db = TuneFlowDatabase(this)
        db.initializeDb()






        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)


        // Removes automatic padding applied by insets
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { _, insets ->
            insets
        }

        // PreLoad fragment and display home
        bottomNavigation.selectedItemId = R.id.nav_home
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, homeFragment)
            .add(R.id.fragment_container, dashboardFragment).hide(dashboardFragment)
            .add(R.id.fragment_container, discoverFragment).hide(discoverFragment)
            .commit()

        // Bottom nav actions
        bottomNavigation.setOnItemSelectedListener { page ->
            when(page.itemId){
                R.id.nav_home -> supportFragmentManager.loadFragment(R.id.fragment_container,
                    HomeFragment()
                )
                R.id.nav_dashboard -> supportFragmentManager.loadFragment(R.id.fragment_container,
                    DashboardFragment()
                )
                R.id.nav_discover -> supportFragmentManager.loadFragment(R.id.fragment_container,
                    DiscoverFragment()
                )
            }
            true
        }

        // GestureDetector for detect horizontal swipe
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null || e2 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (Math.abs(diffX) > Math.abs(diffY) &&
                    Math.abs(diffX) > SWIPE_THRESHOLD &&
                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffX > 0) onSwipeRight() else onSwipeLeft()
                    return true
                }
                return false
            }
        })



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

    private fun onSwipeRight() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is SwipeListener) fragment.onSwipeRight()
    }

    private fun onSwipeLeft() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is SwipeListener) fragment.onSwipeLeft()
    }

    fun showFragment(fragmentToShow: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()

        // Hide all fragments except the one to show
        for (f in listOf(homeFragment, dashboardFragment, discoverFragment)) {
            if (f == fragmentToShow) transaction.show(f)
            else transaction.hide(f)
        }
        transaction.commit()

        // Update BottomNavigationView
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        when(fragmentToShow) {
            homeFragment -> bottomNavigation.selectedItemId = R.id.nav_home
            dashboardFragment -> bottomNavigation.selectedItemId = R.id.nav_dashboard
            discoverFragment -> bottomNavigation.selectedItemId = R.id.nav_discover
        }
    }






}

