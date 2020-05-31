package com.splitreceipt.myapplication

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_ID
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_COL_PARTICIPANTS
import com.splitreceipt.myapplication.data.DatabaseManager.AccountTable.ACCOUNT_TABLE_NAME
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.databinding.FragmentSplitReceiptManuallyBinding

class SplitReceiptManuallyFragment : Fragment() {

    private lateinit var binding: FragmentSplitReceiptManuallyBinding
    private lateinit var participantList: ArrayList<String>
    private lateinit var dbHelper: DbHelper
    var sqlId: String? = "-1"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSplitReceiptManuallyBinding.inflate(inflater, container, false)
        participantList = ArrayList()
        retrieveParticipants()

        val adapter = NewManualReceiptRecyclerAdapter(participantList)
        binding.fragManualRecy.layoutManager = LinearLayoutManager(activity)
        binding.fragManualRecy.adapter = adapter

        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        dbHelper = DbHelper(context)
    }

    private fun retrieveParticipants() {
        sqlId = ReceiptOverviewActivity.getSqlAccountId
        val reader = dbHelper.readableDatabase
        val columns = arrayOf(ACCOUNT_COL_PARTICIPANTS)
        val selectClause = "$ACCOUNT_COL_ID = ?"
        val selectArgs = arrayOf(sqlId)
        val cursor: Cursor = reader.query(ACCOUNT_TABLE_NAME, columns, selectClause, selectArgs,
                                            null, null, null)
        val particColIndex = cursor.getColumnIndexOrThrow(ACCOUNT_COL_PARTICIPANTS)
        while (cursor.moveToNext()){
            val participantsString = cursor.getString(particColIndex)
            val splitParticipants = participantsString.split(",")
            for (participant in splitParticipants) {
                participantList.add(participant)
            }
        }
        cursor.close()
    }


}
