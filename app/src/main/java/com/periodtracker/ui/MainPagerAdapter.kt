package com.periodtracker.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.periodtracker.ui.fragments.CalendarFragment
import com.periodtracker.ui.fragments.StatsFragment
import com.periodtracker.ui.fragments.HistoryFragment

class MainPagerAdapter(fragmentActivity: FragmentActivity) : 
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CalendarFragment()
            1 -> StatsFragment()
            2 -> HistoryFragment()
            else -> CalendarFragment()
        }
    }
}
