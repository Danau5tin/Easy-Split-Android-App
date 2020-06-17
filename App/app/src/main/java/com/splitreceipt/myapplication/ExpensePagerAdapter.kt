package com.splitreceipt.myapplication

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ExpensePagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    val pageNumber = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SplitExpenseManuallyFragment()
            1 -> SplitReceiptScanFragment()
            else -> SplitExpenseManuallyFragment()
        }
    }

    override fun getItemCount(): Int {
        return pageNumber
    }
}