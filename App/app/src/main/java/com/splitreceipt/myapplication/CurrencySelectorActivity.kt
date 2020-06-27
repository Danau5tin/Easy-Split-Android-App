package com.splitreceipt.myapplication

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_ACCOUNT_CURRENCY_CODE
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_NAME
import com.splitreceipt.myapplication.data.SqlDbHelper
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
        val currencyCode = selection.substring(0, 3)
        val startIndex = selection.indexOf("(")
        val endIndex = selection.indexOf(")")
        val countrySymbol = selection.substring(startIndex + 1, endIndex)
        val aSyncCur = ASyncCurrencyDownload(SqlDbHelper(this))
        aSyncCur.execute(currencyCode)

        val sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val edit = sharedPreferences.edit()
        edit.putString(SHARED_PREF_ACCOUNT_CURRENCY_CODE, currencyCode)
        edit.putString(SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL, countrySymbol)
        edit.apply()

        setResult(Activity.RESULT_OK)
        finish()
    }
}
