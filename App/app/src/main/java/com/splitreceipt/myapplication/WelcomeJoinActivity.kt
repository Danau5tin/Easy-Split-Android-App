package com.splitreceipt.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.Toast
import com.splitreceipt.myapplication.data.FirebaseDbHelper
import com.splitreceipt.myapplication.data.SqlDbHelper
import com.splitreceipt.myapplication.databinding.ActivityWelcomeJoinBinding

class WelcomeJoinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeJoinBinding
    private lateinit var firebaseId: String
    private lateinit var fBaseName: String

    companion object {
        var joinFireBaseParticipants: String = "firebasePartic"
        var joinFireBaseName: String = "firebaseName"
        var joinFireBaseId: String = "firebaseId"
        var sqlRow = "-1"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeJoinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fBaseParticipants = intent.getStringExtra(joinFireBaseParticipants)!!
        fBaseName = intent.getStringExtra(joinFireBaseName)!!
        firebaseId = intent.getStringExtra(joinFireBaseId)!!

        val firebaseDbHelper: FirebaseDbHelper = GroupScreenActivity.firebaseDbHelper!!
        firebaseDbHelper.downloadToSql(this)

        val welcome = "Welcome to: '$fBaseName'"
        binding.joinWelcome.text = welcome
        val participants = fBaseParticipants.split(",")
        for ((count, participant) in participants.withIndex()) {
            val radioButton = RadioButton(this)
            radioButton.text = participant
            radioButton.id = count
            binding.joinRadioGroup.addView(radioButton)
        }

        binding.joinContinueButton.setOnClickListener {
            val checkedRadio = binding.joinRadioGroup.checkedRadioButtonId
            if (checkedRadio == -1){
                Toast.makeText(this, "Select your name", Toast.LENGTH_SHORT).show()
            } else {
                val sqlUser = participants[checkedRadio]
                Log.i("Join", sqlUser)

                val sqlDbHelper = SqlDbHelper(this)
                sqlDbHelper.setSqlUser(sqlUser, sqlRow)

                val intent = Intent(this, ExpenseOverviewActivity::class.java)
                intent.putExtra(GroupScreenActivity.sqlIntentString, sqlRow)
                intent.putExtra(GroupScreenActivity.firebaseIntentString, firebaseId)
                intent.putExtra(GroupScreenActivity.userIntentString, sqlUser)
                intent.putExtra(GroupScreenActivity.groupNameIntentString, fBaseName)
                startActivity(intent)
                finish()
            }
        }
    }
}
