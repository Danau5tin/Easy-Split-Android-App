package com.splitreceipt.myapplication

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.databinding.ActivityCurrencySelectorBinding

class CurrencySelectorActivity : AppCompatActivity(), CurrencySelectorAdapter.onCureClick {

    private lateinit var binding: ActivityCurrencySelectorBinding
    private var currencyList: MutableList<String> = mutableListOf(
        "GBP - Great British Pound (£)", "EUR - Euro (€)", "USD - US Dollar ($)",
        "AUD - Australian Dollar ($)", "CAD - Canadian Dollar ($)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCurrencySelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = CurrencySelectorAdapter(currencyList, this)
        binding.currencyRecy.adapter = adapter
        binding.currencyRecy.layoutManager = LinearLayoutManager(this)
    }

    override fun onRowClick(pos: Int) {
        val selection = currencyList[pos]
        val countryCode = selection.substring(0, 3)
        val startIndex = selection.indexOf("(")
        val endIndex = selection.indexOf(")")
        val countrySymbol = selection.substring(startIndex + 1, endIndex)

        SplitReceiptManuallyFragment.currencyCode = countryCode
        SplitReceiptManuallyFragment.currencySymbol = countrySymbol

        setResult(Activity.RESULT_OK)
        finish()
    }
}
