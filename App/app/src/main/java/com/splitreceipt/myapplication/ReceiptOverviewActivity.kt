package com.splitreceipt.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_DATE
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_FK_ACCOUNT_ID
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_PAID_BY
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_TITLE
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_COL_TOTAL
import com.splitreceipt.myapplication.data.DatabaseManager.ReceiptTable.RECEIPT_TABLE_NAME
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.data.ReceiptData
import com.splitreceipt.myapplication.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ReceiptOverviewActivity : AppCompatActivity() {
    /*
    Activity shows the interior of a user account. Listing all prior transactions and receipts and
    allowing the user to create new receipts.
     */

    lateinit var binding: ActivityMainBinding
    lateinit var receiptList: ArrayList<ReceiptData>


    companion object {
        private const val CAMERA_REQ_CODE = 1
        var getSqlAccountId: String? = "-1"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        receiptList = ArrayList()

        // TODO: Ensure these are static like variables to avoid errors
        getSqlAccountId = intent.getStringExtra("sqlID")
        val getFirebaseId = intent.getStringExtra("FirebaseID")
        Toast.makeText(this, getSqlAccountId, Toast.LENGTH_SHORT).show()

        loadPreviousReceipts(getSqlAccountId)
        val todaysDate = getTodaysDate()
        val adapter = ReceiptOverViewAdapter(receiptList, todaysDate)
        binding.mainActivityRecycler.layoutManager = LinearLayoutManager(this)
        binding.mainActivityRecycler.adapter = adapter
    }

    @SuppressLint("SimpleDateFormat")
    fun getTodaysDate(): Date? {
        val currentTime = Calendar.getInstance().time
        val date: Date?
        val dateFormat = SimpleDateFormat(getString(R.string.date_format_dd_MM_yyyy))
        val todaysDate = dateFormat.format(currentTime)
        date = dateFormat.parse(todaysDate)
        return date
    }

    private fun loadPreviousReceipts(sqlId: String?) {
        val dbHelper = DbHelper(this)
        val reader = dbHelper.readableDatabase
        val columns = arrayOf(RECEIPT_COL_DATE, RECEIPT_COL_TITLE, RECEIPT_COL_TOTAL, RECEIPT_COL_PAID_BY)
        val selectClause = "$RECEIPT_COL_FK_ACCOUNT_ID = ?"
        val selectArgs = arrayOf("$sqlId")
        val cursor: Cursor = reader.query(RECEIPT_TABLE_NAME, columns, selectClause, selectArgs, null, null, null)
        val dateColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_DATE)
        val titleColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_TITLE)
        val totalColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_TOTAL)
        val paidByColIndex = cursor.getColumnIndexOrThrow(RECEIPT_COL_PAID_BY)
        while (cursor.moveToNext()) {
            val receiptDate = cursor.getString(dateColIndex)
            val receiptTitle = cursor.getString(titleColIndex)
            val receiptTotal = cursor.getFloat(totalColIndex)
            val receiptPaidBy = cursor.getString(paidByColIndex)
            receiptList.add(ReceiptData(receiptDate, receiptTitle, receiptTotal, receiptPaidBy))
        }
        cursor.close()
    }

    fun addNewReceiptButton(view: View) {
        val intent = Intent(this, NewReceiptCreationActivity::class.java)
        intent.putExtra("sqlID", getSqlAccountId)
        startActivity(intent)
//        checkPermissions()
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQ_CODE)
        } else {
            getImageFromCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getImageFromCamera() {
        TODO("Not yet implemented")
    }
}
