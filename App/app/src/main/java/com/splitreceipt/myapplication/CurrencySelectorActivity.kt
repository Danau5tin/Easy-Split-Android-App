package com.splitreceipt.myapplication

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.a_sync_classes.ASyncCurrencyDownload
import com.splitreceipt.myapplication.adapters.CurrencySelectorAdapter
import com.splitreceipt.myapplication.data.CurrencyUiData
import com.splitreceipt.myapplication.managers.SharedPrefManager.SHARED_PREF_GROUP_CURRENCY_CODE
import com.splitreceipt.myapplication.managers.SharedPrefManager.SHARED_PREF_GROUP_CURRENCY_SYMBOL
import com.splitreceipt.myapplication.managers.SharedPrefManager.SHARED_PREF_NAME
import com.splitreceipt.myapplication.helper_classes.SqlDbHelper
import com.splitreceipt.myapplication.databinding.ActivityCurrencySelectorBinding
import com.splitreceipt.myapplication.helper_classes.CurrencyHelper
import java.util.*

class CurrencySelectorActivity : AppCompatActivity(), CurrencySelectorAdapter.OnCureClick {

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

        fillCompleteCurrencyList()

        isBase = intent.getBooleanExtra(isBaseIntent, false)

        val adapter = CurrencySelectorAdapter(currencyList, this)
        binding.currencyRecy.adapter = adapter
        binding.currencyRecy.layoutManager = LinearLayoutManager(this)

        binding.currencySearch.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int){}
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                if (text.isNullOrBlank()){
                    currencyList.clear()
                    fillCompleteCurrencyList()
                    adapter.updateList(currencyList)
                }
                else {
                    val searchString = text.toString()
                    val currencyTempList : MutableList<String> = mutableListOf()
                    for (currency in currencyList) {
                        if (currency.toLowerCase(Locale.ROOT).contains(searchString.toLowerCase(Locale.ROOT))) {
                            currencyTempList.add(currency)
                        }
                    }
                    adapter.updateList(currencyTempList)
                } } })



        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = ""
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_x_white)
        }
    }

    private fun fillCompleteCurrencyList() {
        for (currency in countryCodeSymbolList) {
            val currencyString = "${currency.countryCode} - ${currency.currencyName} (${currency.currencySelectorSymbol})"
            currencyList.add(currencyString)
        }
    }

    override fun onRowClick(code: String) {
        var selected: CurrencyUiData = countryCodeSymbolList[0]
        Log.i("Currency", "Currency selected by user: $code")
        for (currency in countryCodeSymbolList) {
            if (currency.countryCode == code) {
                selected = currency
                break
            }
        }
        val currencyCode = selected.countryCode
        val countrySymbol = selected.currencyUiSymbol

        if (isBase) {
            Log.i("Currency", "Base currency will be downloaded")
            val aSyncCur =
                ASyncCurrencyDownload(
                    SqlDbHelper(
                        this
                    )
                )
            aSyncCur.execute(currencyCode)
        } else {
            Log.i("Currency", "Base currency will NOT be downloaded")
        }

        val sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val edit = sharedPreferences.edit()
        edit.putString(SHARED_PREF_GROUP_CURRENCY_CODE, currencyCode)
        edit.putString(SHARED_PREF_GROUP_CURRENCY_SYMBOL, countrySymbol)
        edit.apply()

        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}
