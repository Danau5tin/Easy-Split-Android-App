package com.splitreceipt.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.GroupData
import com.splitreceipt.myapplication.data.SqlDbHelper
import com.splitreceipt.myapplication.databinding.ActivityGroupScreenBinding

class GroupScreenActivity : AppCompatActivity() {
    /*
    Initial activity shown to user which shows all the groups they currently have
     */

    lateinit var binding: ActivityGroupScreenBinding
    lateinit var groupList: ArrayList<GroupData>

    companion object {
        var sqlIntentString: String = "sqlID"
        var firebaseIntentString: String = "fireBaseId"
        var userIntentString: String = "user"
        var groupNameIntentString: String = "groupName"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        groupList = ArrayList()
        groupList = SqlDbHelper(this).readAllGroups()

        val adapter = GroupScreenAdapter(groupList)
        binding.groupRecy.layoutManager = LinearLayoutManager(this)
        binding.groupRecy.adapter = adapter
    }

    fun addNewGroupButton(view: View) {
        val intent = Intent(this, NewGroupCreation::class.java)
        startActivity(intent)
    }
}
