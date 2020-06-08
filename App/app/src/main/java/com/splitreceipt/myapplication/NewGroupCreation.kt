package com.splitreceipt.myapplication

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_BALANCES
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_CATEGORY
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_NAME
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_PARTICIPANTS
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_UNIQUE_ID
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_TABLE_NAME
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_SETTLEMENTS
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_USER
import com.splitreceipt.myapplication.data.ParticipantNewGroupData
import com.splitreceipt.myapplication.databinding.ActivityNewGroupCreationBinding
import java.lang.StringBuilder

class NewGroupCreation : AppCompatActivity(), NewGroupParticipantAdapter.onPartRowClick {

    /*
    This activity allows the user to create a new group
     */

    private lateinit var binding: ActivityNewGroupCreationBinding
    private lateinit var adapter: NewGroupParticipantAdapter
    private lateinit var participantList: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewGroupCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        participantList = ArrayList()
        adapter = NewGroupParticipantAdapter(participantList, this)
        binding.newParticipantRecy.layoutManager = LinearLayoutManager(this)
        binding.newParticipantRecy.adapter = adapter

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Add group"
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

    private fun getNewParticipantData(creator: String): ParticipantNewGroupData {
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
        return ParticipantNewGroupData(stringBuilderParticipant.toString(), stringBuilderBalance.toString())
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
        Toast.makeText(this, "Group cancelled", Toast.LENGTH_SHORT).show()
        finish()
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add_group_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.addGroupSave -> {
                // TODO: Save the below results to Firebase and to an SQL db
                val title: String = binding.groupTitleEditText.text.toString()
                val groupUniqueId = "Pbhbdy46218" // TODO: Create an effective & secure way to create a Unique identifier
                val category = "House" // TODO: Get the toggle buttons value

                val sqlUser: String = binding.yourNameEditText.text.toString()
                checkIfUserForgotToAddPartic()
                val participantData: ParticipantNewGroupData = getNewParticipantData(sqlUser)
                val participants: String = participantData.participantString
                val balances: String = participantData.balanceString
                val settlementString = "balanced"

                val dbHelper = DbHelper(this)
                val values: ContentValues = ContentValues().apply {
                    put(GROUP_COL_UNIQUE_ID, groupUniqueId)
                    put(GROUP_COL_NAME, title)
                    put(GROUP_COL_CATEGORY, category)
                    put(GROUP_COL_PARTICIPANTS, participants)
                    put(GROUP_COL_BALANCES, balances)
                    put(GROUP_COL_SETTLEMENTS, settlementString)
                    put(GROUP_COL_USER, sqlUser)
                }
                val write = dbHelper.writableDatabase
                val sqlRes = write.insert(GROUP_TABLE_NAME, null, values)
                if (sqlRes.toInt() == -1) {
                    Toast.makeText(this, "Error #INSQ01. Contact Us", Toast.LENGTH_LONG).show()
                } else {
                    val intent = Intent(this, ReceiptOverviewActivity::class.java)
                    intent.putExtra(GroupScreenActivity.sqlIntentString, sqlRes.toString())
                    intent.putExtra(GroupScreenActivity.userIntentString, sqlUser)
                    intent.putExtra(GroupScreenActivity.groupNameIntentString, title)
                    startActivity(intent)
                    finish()
                }
            }
            else -> return false
        }
        return super.onOptionsItemSelected(item)
    }
}
