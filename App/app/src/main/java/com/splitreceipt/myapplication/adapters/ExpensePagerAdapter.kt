package com.splitreceipt.myapplication.adapters

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.splitreceipt.myapplication.SplitExpenseManuallyFragment
import com.splitreceipt.myapplication.SplitReceiptScanFragment

class ExpensePagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    private val pageNumber = 2

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