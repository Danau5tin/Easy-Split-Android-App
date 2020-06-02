package com.splitreceipt.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.data.ParticipantData
import com.splitreceipt.myapplication.databinding.FragmentSplitReceiptManuallyBinding

class SplitReceiptManuallyFragment : Fragment(), NewManualReceiptRecyclerAdapter.onRecyRowCheked {

    private lateinit var binding: FragmentSplitReceiptManuallyBinding
    private lateinit var adapter: NewManualReceiptRecyclerAdapter
    private lateinit var dbHelper: DbHelper
    private lateinit var contxt: Context
    private var everybodyEqual: Boolean = true
    private var transactionTotal: String = zeroCurrency
    private lateinit var sharedPreferences: SharedPreferences


    companion object{
        private const val zeroCurrency: String = "0.00"
        private const val SHARED_PREF_NAME = "SharedPref"
        private const val SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL = "currency_symbol"
        private const val SHARED_PREF_ACCOUNT_CURRENCY_CODE = "currency_code"
        private const val CURRENCY_INTENT = 2
        var participantList: ArrayList<ParticipantData> = ArrayList()

        var currencyCode = ""
        var currencySymbol = ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSplitReceiptManuallyBinding.inflate(inflater, container, false)
        sharedPreferences = contxt.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)

        retrieveParticipants()
        updateUICurrency()

        adapter = NewManualReceiptRecyclerAdapter(participantList, this)
        binding.fragManualRecy.layoutManager = LinearLayoutManager(activity)
        binding.fragManualRecy.adapter = adapter

        binding.currencyAmount.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                val correctNumber: CharSequence
                if (!text.isNullOrBlank()){
                    val allText = text.toString()
                    if (allText.contains(".")){
                       /* If there is a decimal place present: find the index and ensure the user
                        cannot type more than 2 decimal places from that point by taking a substring.
                        Reset the cursor to end of text with setSelection.
                        */
                        val dotIndex = allText.indexOf(".")
                        if (start > (dotIndex + 2)){
                        correctNumber = text.subSequence(0, dotIndex + 3)
                            binding.currencyAmount.setText(correctNumber.toString())
                            binding.currencyAmount.text?.length?.let {binding.currencyAmount.setSelection(it)}
                        }}
                    transactionTotal = text.toString()
                    setContributionStatus()
                }
                else{
                    transactionTotal = zeroCurrency
                    setContributionStatus()}}})

        binding.currencyButton.setOnClickListener{v ->
            Toast.makeText(contxt, binding.currencyButton.text.toString(), Toast.LENGTH_SHORT).show()
            val intent = Intent(activity, CurrencySelectorActivity::class.java)
            startActivityForResult(intent, CURRENCY_INTENT)
        }

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.i("TEST", "OnActivityResultFragmentCalled")

        if (requestCode == CURRENCY_INTENT) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i("TEST", "OnActivityResultFragmentCalled")

                val editSharedPref = sharedPreferences.edit()
                editSharedPref.putString(SHARED_PREF_ACCOUNT_CURRENCY_CODE, currencyCode)
                editSharedPref.putString(SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL, currencySymbol)
                editSharedPref.apply()
                updateUICurrency(adapterInitialised = true)
            }
        }
    }

    private fun updateUICurrency(adapterInitialised: Boolean = false) {
        currencySymbol = sharedPreferences.getString(SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL, "$").toString()
        currencyCode = sharedPreferences.getString(SHARED_PREF_ACCOUNT_CURRENCY_CODE, "US").toString()
        binding.currencyButton.text = currencyCode
        if (adapterInitialised) {
            binding.fragManualRecy.post(Runnable { adapter.notifyDataSetChanged() })
        }
    }


    fun setContributionStatus(handlerRequired: Boolean = false){
        val totalOfReceipt = transactionTotal.toFloat()
        if (everybodyEqual) {
            setContributionValues(totalOfReceipt, participantList)
        }
        else {
            val contributingParticipants: ArrayList<ParticipantData> = ArrayList()
            for (participant in participantList){
                if (participant.contributing){
                    contributingParticipants.add(participant)
                }
            }
            setContributionValues(totalOfReceipt, contributingParticipants)
        }
        if (!handlerRequired){
            // handler is required only when user checks or unchecks a participant in the UI.
            adapter.notifyDataSetChanged()}
    }

    private fun setContributionValues(total: Float, activeParticipants: ArrayList<ParticipantData>){
        var contribution: String
        val roundOff = Math.round((total / activeParticipants.size) * 100.0) / 100.0
        contribution = roundOff.toString()
        if (contribution.contains(".")) {
            if (contribution.length - contribution.indexOf(".") == 2) {
                contribution += "0"
            }}
        for (participant in activeParticipants){
            participant.contributionValue = contribution
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        dbHelper = DbHelper(context)
        contxt = context
    }

    private fun retrieveParticipants() {
        for (participant in NewReceiptCreationActivity.participantList) {
            participantList.add(ParticipantData(participant, zeroCurrency, true))
        }
    }

    override fun onRecyUnCheck(pos: Int) {
        everybodyEqual = false
        participantList[pos].contributing = false
        participantList[pos].contributionValue = zeroCurrency
        setContributionStatus(true)
        binding.fragManualRecy.post(Runnable { adapter.notifyDataSetChanged() })
    }

    override fun onRecyChecked(pos: Int) {
        participantList[pos].contributing = true
        setContributionStatus(true)
        binding.fragManualRecy.post(Runnable { adapter.notifyDataSetChanged() })
    }
}
