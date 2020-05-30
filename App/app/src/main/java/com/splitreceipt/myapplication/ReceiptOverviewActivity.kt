package com.splitreceipt.myapplication

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.splitreceipt.myapplication.databinding.ActivityMainBinding

class ReceiptOverviewActivity : AppCompatActivity() {

    /*

    Activity shows the interior of a user account. Listing all prior transactions and receipts and
    allowing the user to create new receipts.

     */

    lateinit var mainBinding: ActivityMainBinding

    companion object {
        private const val CAMERA_REQ_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        // TODO: Ensure these are static like variables to avoid errors
        val getSqlId = intent.getStringExtra("sqlID")
        val getFirebaseId = intent.getStringExtra("FirebaseID")
        Toast.makeText(this, getSqlId, Toast.LENGTH_SHORT).show()
    }

    fun addNewReceiptButton(view: View) {
        val intent = Intent(this, ItemizedActivity::class.java)
        startActivity(intent)
//        checkPermissions()
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQ_CODE)
        } else {
            getImageFromCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getImageFromCamera() {
        TODO("Not yet implemented")
    }
}
