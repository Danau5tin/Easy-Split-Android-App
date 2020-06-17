package com.splitreceipt.myapplication

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_BALANCES
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_CATEGORY
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_NAME
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_PARTICIPANTS
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_SETTLEMENTS
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_FIREBASE_ID
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_USER
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_TABLE_NAME
import com.splitreceipt.myapplication.data.ParticipantNewGroupData
import com.splitreceipt.myapplication.data.SharedPrefManager
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_NAME
import com.splitreceipt.myapplication.databinding.ActivityNewGroupCreationBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*
import kotlin.collections.ArrayList


class NewGroupCreation : AppCompatActivity(), NewGroupParticipantAdapter.onPartRowClick {

    /*
    This activity allows the user to create a new group
     */

    private lateinit var binding: ActivityNewGroupCreationBinding
    private lateinit var storageReference: StorageReference
    private lateinit var adapter: NewGroupParticipantAdapter
    private lateinit var participantList: ArrayList<String>
    private val PICK_IMAGE: Int = 10
    private val REQUEST_STORAGE = 20
    private var path = ""
    private var newBitmap: Bitmap? = null

    companion object {
        var profileImageSavedLocally: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewGroupCreationBinding.inflate(layoutInflater)
        profileImageSavedLocally = false
        setContentView(binding.root)
        participantList = ArrayList()
        storageReference = FirebaseStorage.getInstance().reference
        adapter = NewGroupParticipantAdapter(participantList, this)
        binding.newParticipantRecy.layoutManager = LinearLayoutManager(this)
        binding.newParticipantRecy.adapter = adapter

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Add group"
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_x_white)
        }

    }

    private fun checkIfUserForgotToAddPartic() {
        //This function will add any name left in the add recipient text box presuming the user wanted to add them.
        val newPart = binding.newParticipantName.text.toString()
        if (newPart.isNotEmpty()){
            participantList.add(newPart)
        }
    }

    private fun getNewParticipantData(creator: String): ParticipantNewGroupData {
        // Simple function is able to construct the strings required for SQL storage of both the participants and the original balances.
        val stringBuilderParticipant = StringBuilder()
        val stringBuilderBalance = StringBuilder()
        val originalBalance = "0.00"

        val sqlUserNameString = "$creator,"
        stringBuilderParticipant.append(sqlUserNameString)
        stringBuilderBalance.append(sqlUserNameString)
        val sqlUserBalanceString = "$originalBalance/"
        stringBuilderBalance.append(sqlUserBalanceString)

        for (participant in participantList) {
            val nameString = "$participant,"
            stringBuilderParticipant.append(nameString)

            stringBuilderBalance.append(nameString)
            val balanceString = "$originalBalance/"
            stringBuilderBalance.append(balanceString)
        }
        stringBuilderParticipant.deleteCharAt(stringBuilderParticipant.lastIndex)
        stringBuilderBalance.deleteCharAt(stringBuilderBalance.lastIndex)
        return ParticipantNewGroupData(stringBuilderParticipant.toString(), stringBuilderBalance.toString())
    }

    fun addNewParticipantButton(view: View) {
        val participantName = binding.newParticipantName.text.toString()
        participantList.add(participantName)
        adapter.notifyDataSetChanged()
        binding.newParticipantName.setText("")
    }

    override fun onRowclick(position: Int) {
        participantList.removeAt(position)
        adapter.notifyDataSetChanged()
    }

    override fun onSupportNavigateUp(): Boolean {
        Toast.makeText(this, "Group cancelled", Toast.LENGTH_SHORT).show()
        finish()
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add_group_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.addGroupSave -> {
                // TODO: Save the below results to Firebase db
                val title: String = binding.groupTitleEditText.text.toString()
                val category = "House" // TODO: Get the toggle buttons value

                val sqlUser: String = binding.yourNameEditText.text.toString()
                checkIfUserForgotToAddPartic()
                val participantData: ParticipantNewGroupData = getNewParticipantData(sqlUser)
                val participants: String = participantData.participantString
                val balances: String = participantData.balanceString
                val settlementString = "balanced"

                var groupFirebaseId: String? = null
                try {
                    val titleChar = title[0]
                    val sqlUserChar = sqlUser[0]
                    val timeStamp = System.currentTimeMillis().toString()
                    val randomUUID: String = UUID.randomUUID().toString().replace("-", "").substring(0, 5)
                    groupFirebaseId = "$titleChar$timeStamp/$sqlUserChar$randomUUID"
                    Log.i("Account", groupFirebaseId)
                } catch (e: Exception){
                    Toast.makeText(this, "Please enter valid characters (A-Z)", Toast.LENGTH_LONG).show()
                }

                if (groupFirebaseId != null) {
                    val sqlRow = DbHelper(this).insertNewGroup(groupFirebaseId, title,
                        category, participants, balances, settlementString, sqlUser)

                    if (sqlRow == -1) {
                        Toast.makeText(this, "Error #INSQ01. Contact Us", Toast.LENGTH_LONG).show()
                    }
                    else {
                        val intent = Intent(this, ReceiptOverviewActivity::class.java)
                        if (newBitmap == null){
                            //User has not uploaded an group profile image
                            //TODO: Set a standard image depending on which category button was pressed
                        } else {
                            //User has uploaded a group profile image
                            val async = ASyncSaveImage(true, this, groupFirebaseId)
                            path = async.execute(newBitmap!!).get()
                            intent.putExtra(ReceiptOverviewActivity.ImagePathIntent, path)
                        }
                        intent.putExtra(GroupScreenActivity.sqlIntentString, sqlRow.toString())
                        intent.putExtra(GroupScreenActivity.firebaseIntentString, groupFirebaseId)
                        intent.putExtra(GroupScreenActivity.userIntentString, sqlUser)
                        intent.putExtra(GroupScreenActivity.groupNameIntentString, title)
                        intent.putExtra(ReceiptOverviewActivity.ImagePathIntent, path)
                        startActivity(intent)
                        finish()
                    }
                }
            }
            else -> return false
        }
        return super.onOptionsItemSelected(item)
    }

    fun newGroupImageButton(view: View) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_STORAGE)
        } else {
            openGallery()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Permission required to use images!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                val uri: Uri? = data!!.data
                binding.newGroupImage.setImageURI(uri)
                val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val bitmapHelper = BitmapRotationFixHelper()
                newBitmap = bitmapHelper.rotateBitmap(this, uri, bitmap)
                intent.putExtra(ReceiptOverviewActivity.UriIntent, uri.toString())
                //TODO: Store the image with Firebase and create logic in DB so that all members of the group can download a new image if profile picture is ever changed.
            }
        }
    }


    fun uploadImage(imageRef: Uri?) {
        val storageReference = FirebaseStorage.getInstance().reference
        val userStorageRef = storageReference.child("userID")
        val uploadTask = userStorageRef.putFile(imageRef!!)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            val downloadUrl =
                taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
        }
        uploadTask.addOnProgressListener { taskSnapshot ->
            val b = taskSnapshot.bytesTransferred
            Log.i("Firebase", "$b")
        }
    }

}

