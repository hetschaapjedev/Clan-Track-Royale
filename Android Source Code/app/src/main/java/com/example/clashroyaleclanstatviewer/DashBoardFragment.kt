package com.example.clashroyaleclanstatviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DashboardFragment : Fragment(), MainActivity.ScrollSyncable {
    private lateinit var adapter: GlassyAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_list, container, false)
        recyclerView = view.findViewById(R.id.fragRecyclerView)

        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager

        // Mode 0 = Dashboard
        adapter = GlassyAdapter(0)
        recyclerView.adapter = adapter
        return view
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    fun refresh() {
        if (::adapter.isInitialized) {
            adapter.submitList(MainActivity.sharedMemberList)
        }
    }

    // This is called by MainActivity when you swipe to this tab
    override fun scrollToStoredPosition() {
        if (::recyclerView.isInitialized) {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(MainActivity.currentScrollIndex, MainActivity.currentScrollOffset)
        }
    }
}