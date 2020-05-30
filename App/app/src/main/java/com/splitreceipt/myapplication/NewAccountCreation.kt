package com.splitreceipt.myapplication

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_BALANCES
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_CATEGORY
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_NAME
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_PARTICIPANTS
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_UNIQUE_ID
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_TABLE_NAME
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.databinding.ActivityNewAccountCreationBinding

class NewAccountCreation : AppCompatActivity() {

    /*

    This activity allows the user to create a new account

     */

    private lateinit var binding: ActivityNewAccountCreationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewAccountCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun saveNewAccount(view: View) {
        // TODO: Save the below results to Firebase and to an SQL db
        // TODO: Redirect the user
        val title: String = binding.accountTitleEditText.text.toString()
        val accountUniqueId = "Pbhbdy46218" // TODO: Create an effective & secure way to create a Unique identifier
        val category = "House" // TODO: Get the toggle buttons value
        val creator = binding.yourNameEditText.text
        val participants = "$creator,Marie" // TODO: Obtain other participant names
        val balances = "0"
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
            startActivity(intent)
        }
    }
}
