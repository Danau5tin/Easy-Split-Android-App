package com.splitreceipt.myapplication

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
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
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_UNIQUE_ID
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_USER
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_TABLE_NAME
import com.splitreceipt.myapplication.data.ParticipantNewGroupData
import com.splitreceipt.myapplication.databinding.ActivityNewGroupCreationBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewGroupCreationBinding.inflate(layoutInflater)
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
        if (newPart.length >= 1){
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
                // TODO: Save the below results to Firebase and to an SQL db
                val title: String = binding.groupTitleEditText.text.toString()
                val groupUniqueId = "Pbhbdy46218" // TODO: Create an effective & secure way to create a Unique identifier
                val category = "House" // TODO: Get the toggle buttons value

                val sqlUser: String = binding.yourNameEditText.text.toString()
                checkIfUserForgotToAddPartic()
                val participantData: ParticipantNewGroupData = getNewParticipantData(sqlUser)
                val participants: String = participantData.participantString
                val balances: String = participantData.balanceString
                val settlementString = "balanced"

                val dbHelper = DbHelper(this)
                val values: ContentValues = ContentValues().apply {
                    put(GROUP_COL_UNIQUE_ID, groupUniqueId)
                    put(GROUP_COL_NAME, title)
                    put(GROUP_COL_CATEGORY, category)
                    put(GROUP_COL_PARTICIPANTS, participants)
                    put(GROUP_COL_BALANCES, balances)
                    put(GROUP_COL_SETTLEMENTS, settlementString)
                    put(GROUP_COL_USER, sqlUser)
                }
                val write = dbHelper.writableDatabase
                val sqlRes = write.insert(GROUP_TABLE_NAME, null, values)
                if (sqlRes.toInt() == -1) {
                    Toast.makeText(this, "Error #INSQ01. Contact Us", Toast.LENGTH_LONG).show()
                } else {
                    val intent = Intent(this, ReceiptOverviewActivity::class.java)
                    intent.putExtra(GroupScreenActivity.sqlIntentString, sqlRes.toString())
                    intent.putExtra(GroupScreenActivity.userIntentString, sqlUser)
                    intent.putExtra(GroupScreenActivity.groupNameIntentString, title)
                    intent.putExtra(ReceiptOverviewActivity.ImagePathIntent, path)
                    startActivity(intent)
                    finish()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                val uri: Uri? = data!!.data
                val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val newBitmap = rotateBitmap(this, uri, bitmap)

                path = saveToInternalStorage(newBitmap!!) //TODO: Put this in an A-Sync task
                                                        //TODO: Select the file name to be relevant to the group so there are no clashes.
                intent.putExtra(ReceiptOverviewActivity.ImagePathIntent, path)
                binding.newGroupImage.setImageBitmap(newBitmap)
                //TODO: Store the image internally.
                //TODO: Store the image with Firebase and create logic in DB so that all members of the group can download a new image if profile picture is ever c hanged.
            }
        }

    }

    private fun getOrientation(
        context: Context,
        photoUri: Uri
    ): Int {
        val cursor: Cursor? = context.contentResolver.query(
            photoUri,
            arrayOf(MediaStore.Images.ImageColumns.ORIENTATION),
            null,
            null,
            null
        )
        if (cursor?.getCount() != 1) {
            cursor?.close()
            return -1
        }
        cursor.moveToFirst()
        val orientation: Int = cursor.getInt(0)
        cursor.close()
        return orientation
    }

    fun rotateBitmap(
        context: Context?,
        photoUri: Uri?,
        bitmap: Bitmap
    ): Bitmap? {
        var bitmap = bitmap
        val orientation = getOrientation(context!!, photoUri!!)
        if (orientation <= 0) {
            return bitmap
        }
        val matrix = Matrix()
        matrix.postRotate(orientation.toFloat())
        bitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
        return bitmap
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


    private fun saveToInternalStorage(bitmapImage: Bitmap): String {
        val cw = ContextWrapper(applicationContext)
        // path to /data/data/yourapp/app_data/imageDir
        val directory: File = cw.getDir("imageDir", Context.MODE_PRIVATE)
        // Create imageDir
        val myPath = File(directory, "profile.jpg")
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(myPath)
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                fos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return directory.absolutePath
    }

    private fun rotateImage(bitmap: Bitmap?, degrees: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap!!, 0,0,bitmap.width, bitmap.height, matrix, true)
    }

}

