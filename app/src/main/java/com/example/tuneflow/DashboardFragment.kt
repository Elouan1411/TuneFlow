package com.example.tuneflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class DashboardFragment : Fragment(), SwipeListener {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)


    }

    override fun onSwipeRight() {
        (activity as? MainActivity)?.showFragment(
            (activity as MainActivity).homeFragment
        )
    }

    override fun onSwipeLeft() {
        // do nothing
    }
}
