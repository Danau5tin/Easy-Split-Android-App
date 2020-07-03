package com.splitreceipt.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.getSqlGroupId
import com.splitreceipt.myapplication.data.ParticipantBalanceData
import com.splitreceipt.myapplication.data.SqlDbHelper
import com.splitreceipt.myapplication.databinding.ActivityNewParticipantInviteBinding

class NewParticipantInviteActivity : AppCompatActivity(), NewParticipantRecyAdapter.onPartRowClick{

    private lateinit var binding: ActivityNewParticipantInviteBinding
    private lateinit var adapter: NewParticipantRecyAdapter
    private lateinit var participantList: ArrayList<String>
    private lateinit var shareHelper: ShareGroupHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewParticipantInviteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        shareHelper = ShareGroupHelper(this, ExpenseOverviewActivity.getFirebaseId!!)
        participantList = ArrayList()
        participantList = SqlDbHelper(this).retrieveParticipants(participantList, getSqlGroupId!!)

        adapter = NewParticipantRecyAdapter(participantList, this)
        binding.recyclerViewNewParti.adapter = adapter
        binding.recyclerViewNewParti.layoutManager = LinearLayoutManager(this)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Add participant"
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_back_arrow_white)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.save_menu, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuSave -> {
                saveNewParticipantStrings()
                return true
            }
            else -> return false
        }
    }

    private fun saveNewParticipantStrings() {
        finish()
    }

    override fun onRowclick(position: Int) {
        participantList.removeAt(position)
        adapter.notifyDataSetChanged()
    }

    fun copyLinkButton(view: View) {
        shareHelper.clipboardShareCopy()
    }

    fun shareEmailButton(view: View) {
        shareHelper.shareViaEmail()
    }

    fun whatsappShareButton(view: View) {
        shareHelper.shareViaWhatsapp()
    }

    fun addParticButton(view: View) {
        val newParticipantName: String = binding.addParticActivtext.text.toString()
        if (newParticipantName.isNotEmpty()) {
            participantList.add(newParticipantName)
            adapter.notifyDataSetChanged()
            binding.addParticActivtext.setText("")
            val sqlDbHelper = SqlDbHelper(this)
            val fBaseKey = NewGroupCreation.generateFbaseUserKey(newParticipantName)
            val timestamp = System.currentTimeMillis().toString()
            val newParticipant = ParticipantBalanceData(newParticipantName, fBaseKey = fBaseKey)
            sqlDbHelper.setGroupParticipants(newParticipant, getSqlGroupId!!, timestamp)
            ExpenseOverviewActivity.firebaseDbHelper!!.setGroupParticipants(newParticipant, timestamp)
        } else {
            Toast.makeText(this, "Please type in a name for the participant", Toast.LENGTH_SHORT).show()
        }
    }
}
