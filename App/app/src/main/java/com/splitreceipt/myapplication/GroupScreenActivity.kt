package com.splitreceipt.myapplication

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.FirebaseDatabase
import com.splitreceipt.myapplication.data.FirebaseDbHelper
import com.splitreceipt.myapplication.data.GroupData
import com.splitreceipt.myapplication.data.SqlDbHelper
import com.splitreceipt.myapplication.databinding.ActivityGroupScreenBinding
import kotlinx.android.synthetic.main.alert_dialog_join_group.view.*

class GroupScreenActivity : AppCompatActivity() {
    /*
    Initial activity shown to user which shows all the groups they currently have
     */

    lateinit var binding: ActivityGroupScreenBinding
    lateinit var groupList: ArrayList<GroupData>

    companion object {
        var firebaseDbHelper: FirebaseDbHelper? = null

        var sqlIntentString: String = "sqlID"
        var firebaseIntentString: String = "fireBaseId"
        var userIntentString: String = "user"
        var groupNameIntentString: String = "groupName"
        var groupBaseCurrencyIntent: String = "groupCurrency"
        var groupBaseCurrencyUiSymbolIntent: String = "groupUiSymbol"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
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

    fun joinNewGroupButton(view: View){
        val diagView = LayoutInflater.from(this).inflate(R.layout.alert_dialog_join_group, null)
        val builder = AlertDialog.Builder(this)
            .setView(diagView).setTitle("Join existing group").show()
        diagView.exitJoinButton.setOnClickListener {
            builder.cancel()
        }
        diagView.joinButton.setOnClickListener {
            Log.i("Join", "clicked")
            val identifier = diagView.groupIdentifierText.text.toString()
            firebaseDbHelper = FirebaseDbHelper(identifier)
            firebaseDbHelper!!.checkJoin(this)
            builder.dismiss()
            }
    }

    fun originalFloatingButton(view: View) {
        binding.addNewGroupFloatBut.visibility = View.VISIBLE
        binding.joinNewGroupFloatBut.visibility = View.VISIBLE
        binding.joinGroupHint.visibility = View.VISIBLE
        binding.newGroupHint.visibility = View.VISIBLE
    }

}
