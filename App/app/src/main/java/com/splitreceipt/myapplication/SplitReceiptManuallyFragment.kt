package com.splitreceipt.myapplication

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.data.ParticipantData
import com.splitreceipt.myapplication.databinding.FragmentSplitReceiptManuallyBinding
import java.text.DecimalFormat

class SplitReceiptManuallyFragment : Fragment() {

    private lateinit var binding: FragmentSplitReceiptManuallyBinding
    private lateinit var newParticipantList: ArrayList<ParticipantData>
    private lateinit var adapter: NewManualReceiptRecyclerAdapter
    private lateinit var dbHelper: DbHelper
    private lateinit var contxt: Context
    var sqlId: String? = "-1"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSplitReceiptManuallyBinding.inflate(inflater, container, false)
        newParticipantList = ArrayList()
        retrieveParticipants()

        binding.currencySpinner.adapter = ArrayAdapter(contxt, R.layout.support_simple_spinner_dropdown_item, resources.getStringArray(R.array.new_recipt_currency))

        adapter = NewManualReceiptRecyclerAdapter(newParticipantList)
        binding.fragManualRecy.layoutManager = LinearLayoutManager(activity)
        binding.fragManualRecy.adapter = adapter

        binding.currencyAmount.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                val correctNumber: CharSequence
                Log.i("TEST", start.toString())
                if (!text.isNullOrBlank()){
                    val allText = text.toString()
                    if (allText.contains(".")){
                       /* If there is a decimal place present then find the index and ensure the user
                        cannot type more than 2 decimal places from that point by taking a substring.
                        Reset the cursor to end of text with setSelection
                        */
                        val dotIndex = allText.indexOf(".")
                        if (start > (dotIndex + 2)){
                        correctNumber = text.subSequence(0, dotIndex + 3)
                            binding.currencyAmount.setText(correctNumber.toString())
                            binding.currencyAmount.text?.length?.let {binding.currencyAmount.setSelection(it)}
                        } else correctNumber = text
                        Log.i("TEST", correctNumber.toString())
                }
                setContributions(text.toString()) }
                else{
                    setContributions("0.00")
                } } })

        return binding.root
    }

    fun setContributions(total: String){
        var everybodyEqual = true
        val totalOfReceipt = total.toFloat()
        var contribution: String
        if (everybodyEqual) {
            val roundOff = Math.round((totalOfReceipt / newParticipantList.size) * 100.0) / 100.0
            contribution = roundOff.toString()
            if (contribution.contains(".")) {
                if (contribution.length - contribution.indexOf(".") == 2) {
                    contribution += "0"
                }
            }
            for (participant in newParticipantList){
                participant.contribution = contribution
            }
            adapter.notifyDataSetChanged()
        } else {
            contribution = (totalOfReceipt / newParticipantList.size).toString() // TODO: Edit
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        dbHelper = DbHelper(context)
        contxt = context
    }

    private fun retrieveParticipants() {
        for (participant in NewReceiptCreationActivity.participantList) {
            newParticipantList.add(ParticipantData(participant, "0.00"))
        }
    }


}
