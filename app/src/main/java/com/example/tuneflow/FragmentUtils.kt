package com.example.tuneflow

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

fun FragmentManager.loadFragment(containerId: Int, fragment: Fragment) {
    beginTransaction()
        .replace(containerId, fragment)
        .commit()
}
