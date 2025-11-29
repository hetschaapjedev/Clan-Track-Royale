package com.example.clashroyaleclanstatviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WarHistoryFragment : Fragment(), MainActivity.ScrollSyncable {
    private lateinit var adapter: GlassyAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_list, container, false)
        recyclerView = view.findViewById(R.id.fragRecyclerView)

        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager

        // Mode 2 = War History
        adapter = GlassyAdapter(2)
        recyclerView.adapter = adapter
        return view
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    fun refresh() {
        if (::adapter.isInitialized) {
            // Sort by lives descending (same as dashboard)
            val sorted = MainActivity.sharedMemberList.sortedByDescending { it.lives }
            adapter.submitList(sorted)
        }
    }

    override fun scrollToStoredPosition() {
        if (::recyclerView.isInitialized) {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(MainActivity.currentScrollIndex, MainActivity.currentScrollOffset)
        }
    }
}