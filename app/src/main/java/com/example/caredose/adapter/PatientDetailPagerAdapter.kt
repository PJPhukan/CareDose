package com.example.caredose.ui.patient

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.caredose.DoseScheduleFragment
import com.example.caredose.ui.patient.tabs.MedicineStockFragment
import com.example.caredose.ui.patient.tabs.VitalsFragment

class PatientDetailPagerAdapter(
    activity: FragmentActivity,
    private val patientId: Long
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DoseScheduleFragment.newInstance(patientId)
            1 -> MedicineStockFragment.newInstance(patientId)
            2 -> VitalsFragment.newInstance(patientId)
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}