package com.splitreceipt.myapplication

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.ActionBar
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_COL_BALANCES
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_COL_CATEGORY
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_COL_NAME
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_COL_PARTICIPANTS
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_COL_UNIQUE_ID
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_TABLE_NAME
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_COL_SETTLEMENTS
import com.splitreceipt.myapplication.data.DbManager.AccountTable.ACCOUNT_COL_USER
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

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Add account"
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_x_white)
        }

    }

    private fun checkIfUserForgotToAddPartic() {
        //This function will add any name left in the add recipient text box presuming the user wanted to add them.
        val newPart = binding.newParticipantName.text.toString()
        if (newPart.length >= 1){
            participantList.add(newPart)
        }
    }

    private fun getNewParticipantData(creator: String): ParticipantNewAccountData {
        // Simple function is able to construct the strings required for SQL storage of both the participants and the original balances.
        val stringBuilderParticipant = StringBuilder()
        val stringBuilderBalance = StringBuilder()
        val originalBalance = "0.00"

        val sqlUserNameString = "$creator,"
        stringBuilderParticipant.append(sqlUserNameString)
        stringBuilderBalance.append(sqlUserNameString)
        val sqlUserBalanceString = "$originalBalance/"
        stringBuilderBalance.append(sqlUserBalanceString)

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

    override fun onSupportNavigateUp(): Boolean {
        Toast.makeText(this, "Account cancelled", Toast.LENGTH_SHORT).show()
        finish()
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add_account_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.addAccountSave -> {
                // TODO: Save the below results to Firebase and to an SQL db
                val title: String = binding.accountTitleEditText.text.toString()
                val accountUniqueId = "Pbhbdy46218" // TODO: Create an effective & secure way to create a Unique identifier
                val category = "House" // TODO: Get the toggle buttons value

                val sqlUser: String = binding.yourNameEditText.text.toString()
                checkIfUserForgotToAddPartic()
                val participantData: ParticipantNewAccountData = getNewParticipantData(sqlUser)
                val participants: String = participantData.participantString
                val balances: String = participantData.balanceString
                val settlementString = "balanced"

                val dbHelper = DbHelper(this)
                val values: ContentValues = ContentValues().apply {
                    put(ACCOUNT_COL_UNIQUE_ID, accountUniqueId)
                    put(ACCOUNT_COL_NAME, title)
                    put(ACCOUNT_COL_CATEGORY, category)
                    put(ACCOUNT_COL_PARTICIPANTS, participants)
                    put(ACCOUNT_COL_BALANCES, balances)
                    put(ACCOUNT_COL_SETTLEMENTS, settlementString)
                    put(ACCOUNT_COL_USER, sqlUser)
                }
                val write = dbHelper.writableDatabase
                val sqlRes = write.insert(ACCOUNT_TABLE_NAME, null, values)
                if (sqlRes.toInt() == -1) {
                    Toast.makeText(this, "Error #INSQ01. Contact Us", Toast.LENGTH_LONG).show()
                } else {
                    val intent = Intent(this, ReceiptOverviewActivity::class.java)
                    intent.putExtra(AccountScreenActivity.sqlIntentString, sqlRes.toString())
                    intent.putExtra(AccountScreenActivity.userIntentString, sqlUser)
                    intent.putExtra(AccountScreenActivity.accountNameIntentString, title)
                    startActivity(intent)
                    finish()
                }
            }
            else -> return false
        }
        return super.onOptionsItemSelected(item)
    }
}
