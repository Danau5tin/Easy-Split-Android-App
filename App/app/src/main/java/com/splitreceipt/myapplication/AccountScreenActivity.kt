package com.splitreceipt.myapplication

import android.content.Intent
import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.AccountData
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_CATEGORY
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_ID
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_NAME
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_UNIQUE_ID
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_TABLE_NAME
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.databinding.ActivityAccountScreenBinding
import java.lang.IllegalArgumentException

class AccountScreenActivity : AppCompatActivity() {
    /*
    Initial activity shown to user which shows all the accounts they currently have
     */

    lateinit var binding: ActivityAccountScreenBinding
    lateinit var accountList: ArrayList<AccountData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        accountList = ArrayList()
        val accountsAlready = readAccounts()
        if (!accountsAlready) {
            Toast.makeText(this, "No accounts", Toast.LENGTH_SHORT).show()
        }

        val adapter = AccountScreenAdapter(accountList)
        binding.accountRecy.layoutManager = LinearLayoutManager(this)
        binding.accountRecy.adapter = adapter
    }

    private fun readAccounts() : Boolean{
        var accountsFound = false
        val dbHelper = DbHelper(this)
        val reader = dbHelper.readableDatabase
        val columns = arrayOf(ACCOUNT_COL_ID, ACCOUNT_COL_NAME, ACCOUNT_COL_UNIQUE_ID)
        val cursor: Cursor = reader.query(ACCOUNT_TABLE_NAME, columns, null, null, null, null, null)
        val accountNameCol = cursor.getColumnIndexOrThrow(ACCOUNT_COL_NAME)
        val accountSqlId = cursor.getColumnIndexOrThrow(ACCOUNT_COL_ID)
        val accountFirebaseId = cursor.getColumnIndexOrThrow(ACCOUNT_COL_UNIQUE_ID)
        while (cursor.moveToNext()) {
            accountsFound = true
            val accountName = cursor.getString(accountNameCol)
            val accountSqlID = cursor.getString(accountSqlId)
            val accountFirebaseID = cursor.getString(accountFirebaseId)
            accountList.add(AccountData(accountName, accountSqlID, accountFirebaseID))
        }
        cursor.close()
        if (accountsFound) {return true} else {return false}
    }


    fun addNewAccountButton(view: View) {
        val intent = Intent(this, NewAccountCreation::class.java)
        startActivity(intent)
    }

}
