package com.splitreceipt.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
}
