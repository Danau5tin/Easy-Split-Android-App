package com.splitreceipt.myapplication

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_COL_BALANCES
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_COL_CATEGORY
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_COL_NAME
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_COL_PARTICIPANTS
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_COL_UNIQUE_ID
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_TABLE_NAME
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.data.ParticipantNewAccountData
import com.splitreceipt.myapplication.databinding.ActivityNewAccountCreationBinding
import java.lang.StringBuilder

class NewAccountCreation : AppCompatActivity(), NewAccountParticipantAdapter.onPartRowClick {

    /*
    This activity allows the user to create a new account
     */

    private lateinit var binding: ActivityNewAccountCreationBinding
    private lateinit var adapter: NewAccountParticipantAdapter
    private lateinit var participantList: ArrayList<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewAccountCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        participantList = ArrayList()
        adapter = NewAccountParticipantAdapter(participantList, this)
        binding.newParticipantRecy.layoutManager = LinearLayoutManager(this)
        binding.newParticipantRecy.adapter = adapter
    }

    fun saveNewAccount(view: View) {
        // TODO: Save the below results to Firebase and to an SQL db
        // TODO: Redirect the user
        val title: String = binding.accountTitleEditText.text.toString()
        val accountUniqueId = "Pbhbdy46218" // TODO: Create an effective & secure way to create a Unique identifier
        val category = "House" // TODO: Get the toggle buttons value


        val creator: String = binding.yourNameEditText.text.toString()
        val participantData: ParticipantNewAccountData = getNewParticipantData(creator)
        val participants: String = participantData.participantString
        val balances: String = participantData.balanceString


        Log.i("TEST", "Participants: $participants")
        Log.i("TEST", "Balances: $balances")


        val dbHelper = DbHelper(this)
        val values: ContentValues = ContentValues().apply {
            put(ACCOUNT_COL_UNIQUE_ID, accountUniqueId)
            put(ACCOUNT_COL_NAME, title)
            put(ACCOUNT_COL_CATEGORY, category)
            put(ACCOUNT_COL_PARTICIPANTS, participants)
            put(ACCOUNT_COL_BALANCES, balances)
        }
        val write = dbHelper.writableDatabase
        val sqlRes = write.insert(ACCOUNT_TABLE_NAME, null, values)
        if (sqlRes.toInt() == -1) {
            Toast.makeText(this, "Error #INSQ01. Contact Us", Toast.LENGTH_LONG).show()
        } else {
            val intent = Intent(this, ReceiptOverviewActivity::class.java)
            intent.putExtra(AccountScreenActivity.sqlIntentString, sqlRes.toString())
            startActivity(intent)
            finish()
        }
    }

    private fun getNewParticipantData(creator: String): ParticipantNewAccountData {
        // Simple function is able to construct the strings required for SQL storage of both the participants and the original balances.
        val stringBuilderParticipant = StringBuilder()
        val stringBuilderBalance = StringBuilder()
        val originalBalance = "0.00"

        val creatorNameString = "$creator,"
        stringBuilderParticipant.append(creatorNameString)
        stringBuilderBalance.append(creatorNameString)
        val creatorBalanceString = "$originalBalance/"
        stringBuilderBalance.append(creatorBalanceString)

        for (participant in participantList) {
            val nameString = "$participant,"
            stringBuilderParticipant.append(nameString)

            stringBuilderBalance.append(nameString)
            val balanceString = "$originalBalance/"
            stringBuilderBalance.append(balanceString)
        }
        stringBuilderParticipant.deleteCharAt(stringBuilderParticipant.lastIndex)
        stringBuilderBalance.deleteCharAt(stringBuilderBalance.lastIndex)
        return ParticipantNewAccountData(stringBuilderParticipant.toString(), stringBuilderBalance.toString())
    }


    fun addNewParticipantButton(view: View) {
        val participantName = binding.newParticipantName.text.toString()
        participantList.add(participantName)
        adapter.notifyDataSetChanged()
        binding.newParticipantName.setText("")
    }

    override fun onRowclick(position: Int) {
        participantList.removeAt(position)
        adapter.notifyDataSetChanged()
    }
}
