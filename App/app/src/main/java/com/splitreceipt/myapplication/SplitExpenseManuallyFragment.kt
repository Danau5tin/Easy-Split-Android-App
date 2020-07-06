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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.NewExpenseCreationActivity.Companion.currencyCode
import com.splitreceipt.myapplication.NewExpenseCreationActivity.Companion.currencySymbol
import com.splitreceipt.myapplication.NewExpenseCreationActivity.Companion.zeroCurrency
import com.splitreceipt.myapplication.data.SqlDbHelper
import com.splitreceipt.myapplication.data.ParticipantData
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_ACCOUNT_CURRENCY_CODE
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_NAME
import com.splitreceipt.myapplication.databinding.FragmentSplitReceiptManuallyBinding

class SplitExpenseManuallyFragment : Fragment(), NewManualExpenseRecyclerAdapter.OnRecyRowCheked {

    private lateinit var binding: FragmentSplitReceiptManuallyBinding
    private lateinit var adapter: NewManualExpenseRecyclerAdapter
    private lateinit var sqlDbHelper: SqlDbHelper
    private lateinit var contxt: Context
    private var everybodyEqual: Boolean = true
    private lateinit var sharedPreferences: SharedPreferences

    companion object{

        var transactionTotal: String = zeroCurrency
        private const val currencyIntent = 2
        var fragmentManualParticipantList: ArrayList<ParticipantData> = ArrayList()

        fun addStringZerosForDecimalPlace(value: String): String {
            var fixedValue = ""
            if (value.contains(".")) {
                if (value.length - value.indexOf(".") == 2) {
                    fixedValue = value + "0"
                    return fixedValue }
                else { return value } }
            else { return "$value.00"
            }}
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSplitReceiptManuallyBinding.inflate(inflater, container, false)
        sharedPreferences = contxt.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)

        retrieveParticipants()

        binding.currencyButtonManual.text = currencyCode
        adapter = NewManualExpenseRecyclerAdapter(fragmentManualParticipantList, this)
        binding.fragManualRecy.layoutManager = LinearLayoutManager(activity)
        binding.fragManualRecy.adapter = adapter


        binding.currencyAmountManual.addTextChangedListener(object: TextWatcher {
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
                            binding.currencyAmountManual.setText(correctNumber.toString())
                            binding.currencyAmountManual.text?.length?.let {binding.currencyAmountManual.setSelection(it)}
                        }}
                    transactionTotal = text.toString()
                    setContributionStatus()
                }
                else{
                    transactionTotal = zeroCurrency
                    setContributionStatus()}}})

        if (NewExpenseCreationActivity.isEdit) {
            binding.currencyButtonManual.isEnabled = false

            if (!NewExpenseCreationActivity.isScanned){
                binding.currencyAmountManual.setText(NewExpenseCreationActivity.editTotal)
            }

        } else {
            binding.currencyButtonManual.isEnabled = true
            binding.currencyButtonManual.setOnClickListener{
                val intent = Intent(activity, CurrencySelectorActivity::class.java)
                startActivityForResult(intent, currencyIntent)
            }
        }

        return binding.root
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == currencyIntent) {
            if (resultCode == Activity.RESULT_OK) {
                updateUICurrency(adapterInitialised = true)
            }
        }
    }

    private fun updateUICurrency(adapterInitialised: Boolean = false) {
        currencySymbol = sharedPreferences.getString(SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL, "$").toString()
        currencyCode = sharedPreferences.getString(SHARED_PREF_ACCOUNT_CURRENCY_CODE, "USD").toString()
        binding.currencyButtonManual.text = currencyCode
        if (adapterInitialised) {
            binding.fragManualRecy.post(Runnable { adapter.notifyDataSetChanged() })
        }
    }

    fun setContributionStatus(handlerRequired: Boolean = false){
        val totalOfReceipt = transactionTotal.toFloat()
        if (everybodyEqual) {
            setContributionValues(totalOfReceipt, fragmentManualParticipantList)
        }
        else {
            val contributingParticipants: ArrayList<ParticipantData> = ArrayList()
            for (participant in fragmentManualParticipantList){
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
        val contribution: String
        val fixedContribution: String
        if (activeParticipants.isNotEmpty()){
            val num = total / activeParticipants.size
            contribution = ExpenseOverviewActivity.roundToTwoDecimalPlace(num).toString()
            fixedContribution = addStringZerosForDecimalPlace(contribution)
        } else {
            fixedContribution = "0.00"
        }
        for (participant in activeParticipants){
            participant.contributionValue = fixedContribution
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sqlDbHelper = SqlDbHelper(context)
        contxt = context
    }

    private fun retrieveParticipants() {
        fragmentManualParticipantList.clear()
        if (!NewExpenseCreationActivity.isEdit) {
            for (participant in NewExpenseCreationActivity.participantList) {
                fragmentManualParticipantList.add(ParticipantData(participant, zeroCurrency, true))
            }
        } else {
            binding.currencyAmountManual.setText("") // Resets the amount to nothing so that if the user adds expense after editing the old edit amount will not show.
            for (participant in NewExpenseCreationActivity.participantDataEditList) {
                fragmentManualParticipantList.add(participant)
                if (!participant.contributing) {
                    everybodyEqual = false
                }
            }
        }
    }

    override fun onRecyUnCheck(pos: Int) {
        everybodyEqual = false
        fragmentManualParticipantList[pos].contributing = false
        fragmentManualParticipantList[pos].contributionValue = zeroCurrency
        setContributionStatus(true)
        if (!binding.fragManualRecy.isComputingLayout) {
            binding.fragManualRecy.post(Runnable { adapter.notifyDataSetChanged() })
        }


    }

    override fun onRecyChecked(pos: Int) {
        Log.i("DEBUG", "onRecyCheck pos: $pos")
        fragmentManualParticipantList[pos].contributing = true
        setContributionStatus(true)
        if (!binding.fragManualRecy.isComputingLayout) {
            binding.fragManualRecy.post { adapter.notifyDataSetChanged() }
        }
    }
}
