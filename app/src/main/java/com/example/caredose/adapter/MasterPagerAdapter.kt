package com.example.caredose.ui.notifications

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MasterPagerAdapter(
    fragment: Fragment,
    private val medicineFragment: MedicineListFragment,
    private val vitalFragment: VitalListFragment
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> medicineFragment
            1 -> vitalFragment
            else -> medicineFragment
        }
    }
}