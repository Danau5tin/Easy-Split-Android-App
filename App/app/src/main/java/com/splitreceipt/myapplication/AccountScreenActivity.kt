package com.splitreceipt.myapplication

import android.content.Intent
import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.AccountData
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_CATEGORY
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_ID
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_NAME
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_UNIQUE_ID
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_TABLE_NAME
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.databinding.ActivityAccountScreenBinding

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
        readAccounts()

        val adapter = AccountScreenAdapter(accountList)
        binding.accountRecy.layoutManager = LinearLayoutManager(this)
        binding.accountRecy.adapter = adapter
    }

    private fun readAccounts() {
        val dbHelper = DbHelper(this)
        val reader = dbHelper.readableDatabase
        val columns = arrayOf(ACCOUNT_COL_NAME, ACCOUNT_COL_ID)
        val cursor: Cursor =reader.query(ACCOUNT_TABLE_NAME, columns, null, null, null, null, null)
        val accountNameCol = cursor.getColumnIndexOrThrow(ACCOUNT_COL_NAME)
        val accountSqlId = cursor.getColumnIndexOrThrow(ACCOUNT_COL_ID)
        val accountFirebaseId = cursor.getColumnIndexOrThrow(ACCOUNT_COL_UNIQUE_ID)
        while (cursor.moveToNext()) {
            val accountName = cursor.getString(accountNameCol)
            val accountSqlID = cursor.getString(accountSqlId)
            val accountFirebaseID = cursor.getString(accountFirebaseId)
            accountList.add(AccountData(accountName, accountSqlID, accountFirebaseID))
        }
        cursor.close()
    }

    fun addNewAccountButton(view: View) {
        val intent = Intent(this, NewAccountCreation::class.java)
        startActivity(intent)
    }

}