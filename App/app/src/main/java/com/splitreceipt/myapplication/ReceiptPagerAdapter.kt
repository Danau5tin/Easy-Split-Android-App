package com.splitreceipt.myapplication

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class ReceiptPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    val pageNumber = 2

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> SplitReceiptManuallyFragment()
            1 -> SplitReceiptScanFragment()
            else -> SplitReceiptManuallyFragment()
        }
    }

    override fun getCount(): Int {
        return pageNumber
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when(position) {
            0 -> "Split Manually"
            1 -> "Scan Receipt"
            else -> ""
        }
    }

}