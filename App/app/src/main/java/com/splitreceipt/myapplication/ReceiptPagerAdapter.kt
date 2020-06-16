package com.splitreceipt.myapplication

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class ReceiptPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    val pageNumber = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SplitReceiptManuallyFragment()
            1 -> SplitReceiptScanFragment()
            else -> SplitReceiptManuallyFragment()
        }
    }

    override fun getItemCount(): Int {
        return pageNumber
    }
}