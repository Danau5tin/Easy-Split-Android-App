package com.splitreceipt.myapplication

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_ACCOUNT_CURRENCY_CODE
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_NAME
import com.splitreceipt.myapplication.data.SqlDbHelper
import com.splitreceipt.myapplication.databinding.ActivityCurrencySelectorBinding

class CurrencySelectorActivity : AppCompatActivity(), CurrencySelectorAdapter.onCureClick {

    private lateinit var binding: ActivityCurrencySelectorBinding
    private var isBase: Boolean = false
    private var countryCodeSymbolList = CurrencyHelper.currencyArray
    private var currencyList: MutableList<String> = mutableListOf()

    companion object {
        const val isBaseIntent = "isBase"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCurrencySelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        for (currency in countryCodeSymbolList) {
            val currencyString = "${currency.countryCode} - ${currency.currencyName} (${currency.currencySelectorSymbol})"
            currencyList.add(currencyString)
        }

        isBase = intent.getBooleanExtra(isBaseIntent, false)

        val adapter = CurrencySelectorAdapter(currencyList, this)
        binding.currencyRecy.adapter = adapter
        binding.currencyRecy.layoutManager = LinearLayoutManager(this)
    }

    override fun onRowClick(pos: Int) {
        val selection = countryCodeSymbolList[pos]
        val currencyCode = selection.countryCode
        val countrySymbol = selection.currencyUiSymbol

        if (isBase) {
            Log.i("Currency", "Base currency will be downloaded")
            val aSyncCur = ASyncCurrencyDownload(SqlDbHelper(this))
            aSyncCur.execute(currencyCode)
        } else {
            Log.i("Currency", "Base currency will NOT be downloaded")
        }

        val sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val edit = sharedPreferences.edit()
        edit.putString(SHARED_PREF_ACCOUNT_CURRENCY_CODE, currencyCode)
        edit.putString(SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL, countrySymbol)
        edit.apply()

        setResult(Activity.RESULT_OK)
        finish()
    }
}
