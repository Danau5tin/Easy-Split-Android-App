package com.splitreceipt.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.splitreceipt.myapplication.databinding.ActivityItemizedBinding

class ItemizedActivity : AppCompatActivity() {

    /*

    Activity gives the user the ability to input a new receipt/ transaction

     */

    private lateinit var binding: ActivityItemizedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemizedBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun addNewItemizedReceiptButton(view: View) {
        val title =  binding.receiptTitleEditText.text
        val date = binding.dateSpinner // TODO: Get the correct spinner result
        val paidBy = binding.paidBySpinner // TODO: Get the correct spinner result
        // TODO: Take all the itemized results
    }
}
