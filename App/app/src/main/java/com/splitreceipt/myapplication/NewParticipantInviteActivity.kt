package com.splitreceipt.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.firebaseDbHelper
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.currentSqlGroupId
import com.splitreceipt.myapplication.adapters.InviteParticipantRecyAdapter
import com.splitreceipt.myapplication.data.ParticipantBalanceData
import com.splitreceipt.myapplication.helper_classes.SqlDbHelper
import com.splitreceipt.myapplication.databinding.ActivityNewParticipantInviteBinding
import com.splitreceipt.myapplication.helper_classes.ShareGroupHelper
import kotlinx.android.synthetic.main.alert_dialog_edit_participant.view.*

class NewParticipantInviteActivity : AppCompatActivity(),  InviteParticipantRecyAdapter.InviteRecyClick{

    private lateinit var binding: ActivityNewParticipantInviteBinding
    private lateinit var adapter: InviteParticipantRecyAdapter
    private lateinit var participantList: ArrayList<ParticipantBalanceData>
    private lateinit var shareHelper: ShareGroupHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewParticipantInviteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        shareHelper =
            ShareGroupHelper(
                this,
                ExpenseOverviewActivity.currentGroupFirebaseId!!
            )
        participantList = ArrayList()
        participantList = SqlDbHelper(this).retrieveGroupParticipants(currentSqlGroupId!!)

        adapter = InviteParticipantRecyAdapter(participantList, this)
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
            adapter.notifyDataSetChanged()
            binding.addParticActivtext.setText("")
            val sqlDbHelper = SqlDbHelper(this)
            val timestamp = System.currentTimeMillis().toString()
            val newParticipant = ParticipantBalanceData(newParticipantName)
            sqlDbHelper.setGroupParticipants(newParticipant, currentSqlGroupId!!, timestamp)
            firebaseDbHelper!!.setGroupParticipants(newParticipant, timestamp)
            participantList.add(newParticipant)
        } else {
            Toast.makeText(this, "Please type in a name for the participant", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInviteRowClick(pos: Int) {
        val clickedParticipant = participantList[pos]
        val diagView = LayoutInflater.from(this).inflate(R.layout.alert_dialog_edit_participant, null)
        val builder = AlertDialog.Builder(this).setTitle("Edit Participant").setView(diagView).show()
        val textString = "Change ${clickedParticipant.userName}'s name to..."
        diagView.newNameTextView.text = textString
        diagView.newNameCancelButton.setOnClickListener {
            builder.cancel()
        }
        diagView.newNameUpdateButton.setOnClickListener {
            val newNameEditText = diagView.newNameEditText
            if (newNameEditText.text.toString().isEmpty()) {
                Toast.makeText(this, "Check new name", Toast.LENGTH_SHORT).show()
            } else {
                val newName = newNameEditText.text.toString()
                val timestamp = System.currentTimeMillis().toString()
                firebaseDbHelper!!.updateParticipantName(clickedParticipant, newName, timestamp)
                SqlDbHelper(this)
                    .updateParticipantsName(clickedParticipant, newName, timestamp, currentSqlGroupId!!)
                clickedParticipant.userName = newName
                adapter.notifyItemChanged(pos)
                builder.dismiss()
            }
        }
    }
}
