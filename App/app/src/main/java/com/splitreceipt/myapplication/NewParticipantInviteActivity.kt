package com.splitreceipt.myapplication

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.getSqlGroupId
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
        participantList = SqlDbHelper(this).retrieveParticipants(participantList, ExpenseOverviewActivity.getSqlGroupId!!)

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
            val stringBuilder = StringBuilder()
            for (participant in participantList) {
                stringBuilder.append("$participant,")
            }
            stringBuilder.deleteCharAt(stringBuilder.lastIndex)
            val newParticipantString = stringBuilder.toString()
            val sqlDbHelper = SqlDbHelper(this)
            val prevBalanceString = sqlDbHelper.getBalanceString(getSqlGroupId!!)
            val newBalanceString = "$prevBalanceString/$newParticipantName,0.0"
            sqlDbHelper.updateParticipants(newParticipantString, getSqlGroupId!!, newBalanceString)
            ExpenseOverviewActivity.firebaseDbHelper!!.updateParticipants(newParticipantString, newBalanceString)
        } else {
            Toast.makeText(this, "Please type in a name for the participant", Toast.LENGTH_SHORT).show()
        }
    }
}
