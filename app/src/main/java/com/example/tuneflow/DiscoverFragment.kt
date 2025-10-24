package com.example.tuneflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class DiscoverFragment : Fragment(), SwipeListener {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_discover, container, false)
    }

    override fun onSwipeRight() {
        // do nothing
    }

    override fun onSwipeLeft() {
        (activity as? MainActivity)?.showFragment(
            (activity as MainActivity).homeFragment
        )
    }
}
