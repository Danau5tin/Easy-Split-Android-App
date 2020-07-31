package com.splitreceipt.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.FirebaseDatabase
import com.splitreceipt.myapplication.adapters.GroupScreenAdapter
import com.splitreceipt.myapplication.helper_classes.FirebaseDbHelper
import com.splitreceipt.myapplication.data.GroupData
import com.splitreceipt.myapplication.managers.SharedPrefManager
import com.splitreceipt.myapplication.helper_classes.SqlDbHelper
import com.splitreceipt.myapplication.databinding.ActivityGroupScreenBinding
import kotlinx.android.synthetic.main.alert_dialog_join_group.view.*
import kotlinx.android.synthetic.main.alert_dialog_testing.view.*

class GroupScreenActivity : AppCompatActivity() {

    lateinit var binding: ActivityGroupScreenBinding
    lateinit var groupList: ArrayList<GroupData>
    lateinit var sharedPreferences: SharedPreferences
    var firebaseDbHelper: FirebaseDbHelper? = null

    companion object {
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

        sharedPreferences = getSharedPreferences(SharedPrefManager.SHARED_PREF_NAME, Context.MODE_PRIVATE)
        if (!sharedPreferences.getBoolean(SharedPrefManager.SHARED_PREF_TEST_DIALOG, false)) {
            showTestingDialog()
        }

    }

    private fun showTestingDialog() {
        sharedPreferences.edit().putBoolean(SharedPrefManager.SHARED_PREF_TEST_DIALOG, true).apply()
        val diagView = LayoutInflater.from(this).inflate(R.layout.alert_dialog_testing, null)
        val builder = AlertDialog.Builder(this).setView(diagView).show()
        diagView.willDoButton.setOnClickListener {
            builder.dismiss()
        }
    }

    fun addNewGroupButton(view: View) {
        val intent = Intent(this, NewGroupCreationActivity::class.java)
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
            firebaseDbHelper =
                FirebaseDbHelper(
                    identifier
                )
            firebaseDbHelper!!.checkJoinOkayAndStartIntent(this)
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
