package com.splitreceipt.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class AccountScreenActivity : AppCompatActivity() {

    /*

    Initial activity shown to user which shows all the accounts they currently have

     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_screen)
        // TODO: Fill the recyclerview with the sql database entry for all available accounts
    }

    fun addNewAccountButton(view: View) {
        val intent = Intent(this, NewAccountCreation::class.java)
        startActivity(intent)
    }
}
