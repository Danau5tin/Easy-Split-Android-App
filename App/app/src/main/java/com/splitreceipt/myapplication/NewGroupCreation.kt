package com.splitreceipt.myapplication

import android.app.Activity
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.splitreceipt.myapplication.data.FirebaseDbHelper
import com.splitreceipt.myapplication.data.SqlDbHelper
import com.splitreceipt.myapplication.data.ParticipantNewGroupData
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_ACCOUNT_CURRENCY_CODE
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_NAME
import com.splitreceipt.myapplication.databinding.ActivityNewGroupCreationBinding
import java.util.*
import kotlin.collections.ArrayList


class NewGroupCreation : AppCompatActivity(), NewParticipantRecyAdapter.onPartRowClick {

    /*
    This activity allows the user to create a new group
     */

    private lateinit var binding: ActivityNewGroupCreationBinding
    private lateinit var storageReference: StorageReference
    private lateinit var recyAdapter: NewParticipantRecyAdapter
    private lateinit var participantList: ArrayList<String>
    private var uriString: String? = null
    private val pickImage: Int = 10
    private val requestStorage = 20
    private val baseCurrencySelection = 30
    private var path = ""
    private var newBitmap: Bitmap? = null
    private var currencySymbol: String = ""
    private lateinit var groupFirebaseId: String

    companion object {
        var firebaseDbHelper: FirebaseDbHelper? = null
        var profileImageSavedLocally: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewGroupCreationBinding.inflate(layoutInflater)
        profileImageSavedLocally = false
        setContentView(binding.root)
        participantList = ArrayList()
        storageReference = FirebaseStorage.getInstance().reference
        recyAdapter = NewParticipantRecyAdapter(participantList, this)
        binding.newParticipantRecy.layoutManager = LinearLayoutManager(this)
        binding.newParticipantRecy.adapter = recyAdapter

        groupFirebaseId = createFirebaseGroupId()!!
        firebaseDbHelper = FirebaseDbHelper(groupFirebaseId)

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
        recyAdapter.notifyDataSetChanged()
        binding.newParticipantName.setText("")
    }

    override fun onRowclick(position: Int) {
        participantList.removeAt(position)
        recyAdapter.notifyDataSetChanged()
    }

    override fun onSupportNavigateUp(): Boolean {
        Toast.makeText(this, "Group cancelled", Toast.LENGTH_SHORT).show()
        finish()
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.next_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuNext -> {
                val title: String = binding.groupTitleEditText.text.toString()
                val sqlUser: String = binding.yourNameEditText.text.toString()
                checkIfUserForgotToAddPartic()
                val participantData: ParticipantNewGroupData = getNewParticipantData(sqlUser)
                val participants: String = participantData.participantString
                val balances: String = participantData.balanceString
                val settlementString = "balanced"
                val imageUploadLastEditTime = System.currentTimeMillis().toString()
                val currencyCode = binding.newGroupCurrencyButton.text.toString()
                val baseCurrencyUiSymbol = CurrencyHelper.returnUiSymbol(currencyCode)

                // Save to SQL and upload to firebase
                val sqlRow = SqlDbHelper(this).insertNewGroup(groupFirebaseId, title,
                    participants, balances, settlementString, sqlUser, imageUploadLastEditTime, currencyCode, baseCurrencyUiSymbol)
                firebaseDbHelper!!.createNewGroup(title, balances,
                    settlementString, participants, imageUploadLastEditTime, currencyCode)

                if (sqlRow == -1) {
                    Toast.makeText(this, "Error #INSQ01. Contact Us", Toast.LENGTH_LONG).show()
                }
                else {
                    val async = ASyncSaveImage(true, this, groupFirebaseId)
                    val intent = Intent(this, ExpenseOverviewActivity::class.java)
                    if (newBitmap == null){
                        //User has not uploaded a group profile image. Use default logo.
                        newBitmap = BitmapFactory.decodeResource(resources, R.drawable.easy_split_logo)
                        path = async.execute(newBitmap!!).get()
                    } else {
                        //User has uploaded a group profile image
                        path = async.execute(newBitmap!!).get()
                    }
                    intent.putExtra(ExpenseOverviewActivity.ImagePathIntent, path)
                    intent.putExtra(GroupScreenActivity.sqlIntentString, sqlRow.toString())
                    intent.putExtra(GroupScreenActivity.firebaseIntentString, groupFirebaseId)
                    intent.putExtra(GroupScreenActivity.userIntentString, sqlUser)
                    intent.putExtra(GroupScreenActivity.groupNameIntentString, title)
                    intent.putExtra(GroupScreenActivity.groupBaseCurrencyIntent, currencyCode)
                    intent.putExtra(GroupScreenActivity.groupBaseCurrencyUiSymbolIntent, currencySymbol)
                    intent.putExtra(ExpenseOverviewActivity.ImagePathIntent, path)
                    intent.putExtra(ExpenseOverviewActivity.UriIntent, uriString)
                    startActivity(intent)
                    finish()
                }
            }
            else -> return false
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createFirebaseGroupId(): String? {
        val groupFirebaseId: String?
        return try {
            val titleChar = title[0]
            val persistentChar = "a"
            val timeStamp = System.currentTimeMillis().toString()
            val randomUUID: String = UUID.randomUUID().toString().replace("-", "").substring(0, 5)
            groupFirebaseId = "$titleChar$timeStamp$persistentChar$randomUUID"
            Log.i("Account", groupFirebaseId)
            groupFirebaseId
        } catch (e: Exception){
            Toast.makeText(this, "Please enter valid characters (A-Z)", Toast.LENGTH_LONG).show()
            return null
        }
    }

    fun newGroupImageButton(view: View) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), requestStorage)
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
            requestStorage -> {
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
        startActivityForResult(intent, pickImage)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImage) {
            if (resultCode == Activity.RESULT_OK) {
                val uri: Uri? = data!!.data
                binding.newGroupImage.setImageURI(uri)
                val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val bitmapHelper = BitmapRotationFixHelper()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    newBitmap = bitmapHelper.rotateBitmap(this, uri, bitmap)
                } else {
                    newBitmap = bitmap
                }
                firebaseDbHelper!!.uploadGroupProfileImage(newBitmap)
                uriString = uri.toString()
            }
        }
        else if (requestCode == baseCurrencySelection) {
            if (resultCode == Activity.RESULT_OK) {
                val sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
                val currencyCode = sharedPreferences.getString(SHARED_PREF_ACCOUNT_CURRENCY_CODE, "USD")
                currencySymbol = sharedPreferences.getString(SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL, "$")!!
                binding.newGroupCurrencyButton.text = currencyCode
            }
        }
    }

    fun newGroupCurrencyButton(view: View) {
        val intent = Intent(this, CurrencySelectorActivity::class.java)
        intent.putExtra(CurrencySelectorActivity.isBaseIntent, true)
        startActivityForResult(intent, baseCurrencySelection)
    }

}

