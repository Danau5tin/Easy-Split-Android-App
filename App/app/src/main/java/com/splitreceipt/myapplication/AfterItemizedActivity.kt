package com.splitreceipt.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.splitreceipt.myapplication.databinding.ActivityAfterItemizedBinding

class AfterItemizedActivity : AppCompatActivity() {

    /*

    Activity gives the user the ability to input a new receipt/ transaction

     */

    private lateinit var afterBinding: ActivityAfterItemizedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        afterBinding = ActivityAfterItemizedBinding.inflate(layoutInflater)
        setContentView(afterBinding.root)
    }
}
