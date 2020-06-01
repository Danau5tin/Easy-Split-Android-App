package com.splitreceipt.myapplication

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.DatabaseManager
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_DATE
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_FK_ACCOUNT_ID
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_PAID_BY
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_TITLE
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_TOTAL
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_UNIQUE_ID
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_TABLE_NAME
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.databinding.ActivityNewReceiptCreationBinding
import java.util.*
import kotlin.collections.ArrayList


class NewReceiptCreationActivity : AppCompatActivity() {

    /*
    Activity gives the user the ability to input a new receipt/ transaction
     */

    private lateinit var binding: ActivityNewReceiptCreationBinding
    private lateinit var sqlId: String

    companion object {
        var sqlAccountId: String? = "-1"
        lateinit var participantList: ArrayList<String>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewReceiptCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sqlAccountId = intent.getStringExtra("sqlID")
        participantList = ArrayList()
        retrieveParticipants()

        val spinAdap = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item,
            resources.getStringArray(R.array.new_receipt_when))
        binding.dateSpinner.adapter = spinAdap

        binding.paidBySpinner.adapter = ArrayAdapter(this,
            R.layout.support_simple_spinner_dropdown_item, participantList)

        val pagerAdapter = ReceiptPagerAdapter(supportFragmentManager)
        binding.receiptViewPager.adapter = pagerAdapter
        binding.receiptTabLayout.setupWithViewPager(binding.receiptViewPager)
    }

    private fun retrieveParticipants() {
        val dbHelper = DbHelper(this)
        sqlAccountId = ReceiptOverviewActivity.getSqlAccountId
        val reader = dbHelper.readableDatabase
        val columns = arrayOf(DatabaseManager.AccountTable.ACCOUNT_COL_PARTICIPANTS)
        val selectClause = "${DatabaseManager.AccountTable.ACCOUNT_COL_ID} = ?"
        val selectArgs = arrayOf(sqlAccountId)
        val cursor: Cursor = reader.query(
            DatabaseManager.AccountTable.ACCOUNT_TABLE_NAME, columns, selectClause, selectArgs,
            null, null, null)
        val particColIndex = cursor.getColumnIndexOrThrow(DatabaseManager.AccountTable.ACCOUNT_COL_PARTICIPANTS)
        while (cursor.moveToNext()){
            val participantsString = cursor.getString(particColIndex)
            val splitParticipants = participantsString.split(",")
            for (participant in splitParticipants) {
                participantList.add(participant)
            }
        }
        cursor.close()
    }

    fun addNewItemizedReceiptButton(view: View) {
        //TODO: Attach this to the relevant next/continue button
        val receiptFirebaseID = "rec00001"
        val date = "31/05/2020"// TODO: Get the correct spinner result
        val title =  binding.receiptTitleEditText.text.toString()
        val total = 1.44F
        val paidBy = "Dan" // TODO: Get the correct spinner result
        // TODO: Take all the itemized results
        val sqlRow = updateSql(receiptFirebaseID, date, title, total, paidBy)
        Toast.makeText(this, sqlRow.toString(), Toast.LENGTH_SHORT).show()
    }

    private fun updateSql(recFirebaseId: String, date: String, title: String, total: Float, paidBy: String) : Int{
        val dbHelper = DbHelper(this)
        val write = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(RECEIPT_COL_UNIQUE_ID, recFirebaseId)
            put(RECEIPT_COL_DATE, date)
            put(RECEIPT_COL_TITLE, title)
            put(RECEIPT_COL_TOTAL, total)
            put(RECEIPT_COL_PAID_BY, paidBy)
            put(RECEIPT_COL_FK_ACCOUNT_ID, sqlAccountId)
        }
        val sqlId = write.insert(RECEIPT_TABLE_NAME, null, values)
        return sqlId.toInt()
    }
}
