package com.splitreceipt.myapplication

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.splitreceipt.myapplication.data.FirebaseDbHelper
import com.splitreceipt.myapplication.data.ParticipantBalanceData
import com.splitreceipt.myapplication.data.SqlDbHelper
import com.splitreceipt.myapplication.databinding.ActivityWelcomeJoinBinding

class WelcomeJoinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeJoinBinding
    private lateinit var firebaseId: String
    private lateinit var fBaseParticipantLastEdit: String
    private lateinit var fBaseName: String
    private lateinit var fBaseCurrencyCode: String
    private lateinit var fBaseCurrencySymbol: String
    private var radioRequired: Boolean = true

    companion object {
        var joinFireBaseParticipants: String = "firebasePartic"
        var joinFireBaseName: String = "firebaseName"
        var joinFireBaseId: String = "firebaseId"
        var joinBaseCurrency: String = "baseCurrency"
        var sqlRow = "-1"

        fun populateRadioButtons (context: Context, participants: ArrayList<String>, radioGroup: RadioGroup) {
            for ((count, participant) in participants.withIndex()) {
                val radioButton = RadioButton(context)
                radioButton.text = participant
                radioButton.id = count
                radioGroup.addView(radioButton)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeJoinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fBaseParticipantLastEdit = intent.getStringExtra(joinFireBaseParticipants)!!
        fBaseName = intent.getStringExtra(joinFireBaseName)!!
        firebaseId = intent.getStringExtra(joinFireBaseId)!!
        fBaseCurrencyCode = intent.getStringExtra(joinBaseCurrency)!!
        fBaseCurrencySymbol = CurrencyHelper.returnUiSymbol(fBaseCurrencyCode)

        val participants: ArrayList<String> = ArrayList()
        val firebaseDbHelper: FirebaseDbHelper = GroupScreenActivity.firebaseDbHelper!!
        firebaseDbHelper.downloadToSql(this, participants, binding.joinRadioGroup)
        firebaseDbHelper.downloadGroupProfileImage(this, binding.circleImageViewWelcome)

        val welcome = "Welcome to: '$fBaseName'"
        binding.joinWelcome.text = welcome

        binding.joinContinueButton.setOnClickListener {
            if (okayToProceed()) {
                val sqlUser: String
                if (radioRequired){
                    val checkedRadio = binding.joinRadioGroup.checkedRadioButtonId
                    sqlUser = participants[checkedRadio]
                } else {
                    sqlUser = binding.joinNameText.text.toString()
                    addParticipant(sqlUser)
                }

                Log.i("Join", sqlUser)

                val sqlDbHelper = SqlDbHelper(this)
                sqlDbHelper.setSqlUser(sqlUser, sqlRow)

                val intent = Intent(this, ExpenseOverviewActivity::class.java)
                intent.putExtra(GroupScreenActivity.sqlIntentString, sqlRow)
                intent.putExtra(GroupScreenActivity.firebaseIntentString, firebaseId)
                intent.putExtra(GroupScreenActivity.userIntentString, sqlUser)
                intent.putExtra(GroupScreenActivity.groupNameIntentString, fBaseName)
                intent.putExtra(GroupScreenActivity.groupBaseCurrencyIntent, fBaseCurrencyCode)
                intent.putExtra(
                    GroupScreenActivity.groupBaseCurrencyUiSymbolIntent,
                    fBaseCurrencySymbol
                )
                startActivity(intent)
                finish()
            }

        }
    }

    private fun addParticipant(sqlUser: String) {
        val sqlDbHelper = SqlDbHelper(this)
        val fBaseKey = NewGroupCreation.generateFbaseUserKey(sqlUser)
        val timestamp = System.currentTimeMillis().toString()
        val newParticipant = ParticipantBalanceData(sqlUser, fBaseKey=fBaseKey)
        sqlDbHelper.setGroupParticipants(newParticipant, sqlRow, timestamp)
        GroupScreenActivity.firebaseDbHelper!!.setGroupParticipants(newParticipant, timestamp)
    }

    private fun okayToProceed() : Boolean {
        if (radioRequired) {
            val checkedRadio = binding.joinRadioGroup.checkedRadioButtonId
            if (checkedRadio == -1) {
                Toast.makeText(this, "Select your name", Toast.LENGTH_SHORT).show()
                return false
            }
            return true
        } else {
            if (binding.joinNameText.text.isNullOrBlank()) {
                Toast.makeText(this, "Enter your name", Toast.LENGTH_SHORT).show()
                return false
            }
            return true
        }
    }

        fun nameNotHereButton(view: View) {
            if (radioRequired) {
                radioRequired = false
                binding.joinRadioGroup.visibility = View.GONE
                binding.joinNameLayout.visibility = View.VISIBLE
                binding.nameNotHereBut.text = "Back"
            } else {
                radioRequired = true
                binding.joinRadioGroup.visibility = View.VISIBLE
                binding.joinNameLayout.visibility = View.GONE
                binding.nameNotHereBut.text = "Name not here?"
            }


        }
    }



