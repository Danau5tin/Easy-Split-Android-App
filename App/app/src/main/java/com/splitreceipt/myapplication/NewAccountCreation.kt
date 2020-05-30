package com.splitreceipt.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.splitreceipt.myapplication.databinding.ActivityNewAccountCreationBinding

class NewAccountCreation : AppCompatActivity() {

    /*

    This activity allows the user to create a new account

     */

    private lateinit var binding: ActivityNewAccountCreationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewAccountCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun saveNewAccount(view: View) {
        // TODO: Save the below results to Firebase and to an SQL db
        // TODO: Redirect the user
        // TODO: Get the toggle buttons value
        val title = binding.accountTitleEditText.text
        val creator = binding.yourNameEditText.text
        val otherParticipants = 1 // TODO: Obtain other participant names
    }
}
