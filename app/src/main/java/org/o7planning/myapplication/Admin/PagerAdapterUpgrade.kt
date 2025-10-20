package org.o7planning.myapplication.admin

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.o7planning.myapplication.Admin.RvStepTwoFragment

class PagerAdapterUpgrade(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RvStepOneFragment()
            1 -> RvStepTwoFragment()
            2 -> RvStepThreeFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}