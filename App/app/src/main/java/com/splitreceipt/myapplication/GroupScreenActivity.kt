package com.splitreceipt.myapplication

import android.content.Intent
import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.GroupData
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_ID
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_NAME
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_UNIQUE_ID
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_TABLE_NAME
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_USER
import com.splitreceipt.myapplication.databinding.ActivityGroupScreenBinding

class GroupScreenActivity : AppCompatActivity() {
    /*
    Initial activity shown to user which shows all the groups they currently have
     */

    lateinit var binding: ActivityGroupScreenBinding
    lateinit var groupList: ArrayList<GroupData>

    companion object {
        var sqlIntentString: String = "sqlID"
        var userIntentString: String = "user"
        var groupNameIntentString: String = "groupName"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        groupList = ArrayList()
        val groupsAlready = readGroups()
        if (!groupsAlready) {
            Toast.makeText(this, "No Groups", Toast.LENGTH_SHORT).show()
        }

        val adapter = GroupScreenAdapter(groupList)
        binding.groupRecy.layoutManager = LinearLayoutManager(this)
        binding.groupRecy.adapter = adapter
    }

    private fun readGroups() : Boolean{
        var groupsFound = false
        val dbHelper = DbHelper(this)
        val reader = dbHelper.readableDatabase
        val columns = arrayOf(GROUP_COL_ID, GROUP_COL_NAME, GROUP_COL_UNIQUE_ID, GROUP_COL_USER)
        val cursor: Cursor = reader.query(GROUP_TABLE_NAME, columns, null, null, null, null, null)
        val groupNameColIndex = cursor.getColumnIndexOrThrow(GROUP_COL_NAME)
        val groupSqlIdIndex = cursor.getColumnIndexOrThrow(GROUP_COL_ID)
        val groupFirebaseIdIndex = cursor.getColumnIndexOrThrow(GROUP_COL_UNIQUE_ID)
        val groupSqlUserIndex = cursor.getColumnIndexOrThrow(GROUP_COL_USER)
        while (cursor.moveToNext()) {
            groupsFound = true
            val groupName = cursor.getString(groupNameColIndex)
            val groupSqlID = cursor.getString(groupSqlIdIndex)
            val groupFirebaseID = cursor.getString(groupFirebaseIdIndex)
            val groupSqlUser = cursor.getString(groupSqlUserIndex)
            groupList.add(GroupData(groupName, groupSqlID, groupFirebaseID, groupSqlUser))
        }
        cursor.close()
        if (groupsFound) {return true} else {return false}
    }


    fun addNewGroupButton(view: View) {
        val intent = Intent(this, NewGroupCreation::class.java)
        startActivity(intent)
    }


}
