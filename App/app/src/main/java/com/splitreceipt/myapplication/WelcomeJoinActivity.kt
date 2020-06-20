package com.splitreceipt.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.Toast
import com.splitreceipt.myapplication.data.FirebaseAccountInfoData
import com.splitreceipt.myapplication.databinding.ActivityWelcomeJoinBinding

class WelcomeJoinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeJoinBinding

    companion object {
        var joinFireBaseIdIntent: String = "firebaseID"
        var joinFireBaseParticipants: String = "firebasePartic"
        var joinFireBaseName: String = "firebaseName"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeJoinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val firebaseID = intent.getStringExtra(joinFireBaseIdIntent)
        val fBaseParticipants = intent.getStringExtra(joinFireBaseParticipants)!!
        val fBaseName = intent.getStringExtra(joinFireBaseName)!!

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
            }
        }
    }
}
