package com.example.tuneflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
class HomeFragment : Fragment() {

    private var nextPageNumber = 1  // temp -> just for test
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: SwipeAdapter

    companion object {
        private const val MAX_PAGES_HISTORY = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        viewPager = view.findViewById(R.id.viewPager)

        // initial list
        val items = mutableListOf<String>()
        adapter = SwipeAdapter(items)
        viewPager.adapter = adapter

        // add 3 first pages
        repeat(3) { addNextPage() }

        // callback for add new pages
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // if we are on the last page, we add a new page
                if (position == adapter.itemCount - 1) {
                    addNextPage()
                }

                // delete first page for storage
                if (adapter.itemCount > MAX_PAGES_HISTORY) {
                    adapter.removeFirstPage()
                    viewPager.setCurrentItem(position - 1, false)
                }
            }
        })

        return view
    }

    private fun addNextPage() {
        val nextPage = "Page $nextPageNumber"
        adapter.addPage(nextPage)
        nextPageNumber++
    }
}
