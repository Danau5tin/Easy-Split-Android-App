package com.splitreceipt.myapplication

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.splitreceipt.myapplication.helper_classes.SqlDbHelper
import com.splitreceipt.myapplication.databinding.ActivityGroupSettingsBinding
import com.splitreceipt.myapplication.helper_classes.ImageHelper


class GroupSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupSettingsBinding
    private lateinit var sqlRowId: String
    private val requestStorage: Int = 10
    private val pickImage: Int = 20
    private var imageUri: String? = null

    companion object {
        const val groupNameIntent: String = "groupName"
        const val groupSqlIdIntent: String = "groupSql"
        const val groupNameReturnIntent: String = "groupNameReturn"
        const val groupImageChangedUriIntent: String = "groupImageChanged"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val groupName = intent.getStringExtra(groupNameIntent)
        binding.nameEdit.setText(groupName)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Group settings"
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_back_arrow_white)
        }

        sqlRowId = intent.getStringExtra(groupSqlIdIntent)!!
        val bitmap = ImageHelper.loadImageFromStorage(this, ExpenseOverviewActivity.currentGroupFirebaseId!!)
        binding.groupImage.setImageBitmap(bitmap)
    }

    fun deleteGroupButton(view: View) {
        AlertDialog.Builder(this).apply {
            setIcon(R.drawable.vector_warning_yellow)
            setTitle("Are you ABSOLUTELY sure?")
            setMessage("This group will be deleted for ALL users involved, not just yourself.")
            setPositiveButton("Yes delete", object: DialogInterface.OnClickListener{
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    SqlDbHelper(context).deleteGroup(ExpenseOverviewActivity.currentSqlGroupId!!)
                    //TODO: Delete from firebase also.
                    Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                    onBackPressed()
                }
            })
            setNegativeButton("No, cancel", object: DialogInterface.OnClickListener{
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    dialog?.cancel()
                }
            })
        }.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.save_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuSave -> {
                val okayToProceed = checkOkayToProceed()
                if (okayToProceed) {
                    val groupName = binding.nameEdit.text.toString()
                    ExpenseOverviewActivity.firebaseDbHelper!!.updateGroupName(groupName)
                    SqlDbHelper(this).updateGroupName(sqlRowId, groupName)
                    intent.putExtra(groupNameReturnIntent, groupName)
                    intent.putExtra(groupImageChangedUriIntent, imageUri)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                    return true
                }else {
                    return false
                }
            }
            else -> return false
        }
    }

    private fun checkOkayToProceed(): Boolean {
        return if (binding.nameEdit.text!!.isEmpty()) {
            Toast.makeText(this, "Check group name", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    fun addNewParticipantSettingsButton(view: View) {
        val intent = Intent(this, NewParticipantInviteActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun changeGroupImageButton(view: View) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), requestStorage)
        } else {
            openGallery()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImage) {
            if (resultCode == Activity.RESULT_OK) {
                val uri: Uri? = data?.data
                ImageHelper.handleNewImage(this, uri!!, binding.groupImage)
                imageUri = uri.toString()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, pickImage)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            requestStorage -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Permission required to add a photo!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
