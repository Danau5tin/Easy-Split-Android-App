package com.splitreceipt.myapplication

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import com.splitreceipt.myapplication.data.SqlDbHelper
import com.splitreceipt.myapplication.databinding.ActivitySettleGroupBinding

class SettleGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettleGroupBinding
    private lateinit var paidBy: String
    private lateinit var paidTo: String
    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettleGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Settle up"
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_back_arrow_white)
        }

        var participantList: ArrayList<String> = ArrayList()
        participantList = SqlDbHelper(this).retrieveParticipants(participantList, ExpenseOverviewActivity.getSqlGroupId!!)
        val spinnerAdapter = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, participantList)
        binding.fromSelectionSpinner.adapter = spinnerAdapter
        binding.toSelectionSpinner.adapter = spinnerAdapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuSave -> {
                val okayToProceed = checkOkayToProceed()
                if (okayToProceed) {
                    val title = binding.settlementTitleText.text.toString()
                    val amount = binding.settleAmountText.text.toString().toFloat()
                    val contribString = "$paidTo,$amount,$paidBy"
                    val firebaseExpenseId = System.currentTimeMillis().toString()
                    val date = NewExpenseCreationActivity.retrieveTodaysDate(this)
                    //TODO: Allow settlements to be made in any currency, defaulting to the base currency.
                    ExpenseOverviewActivity.firebaseDbHelper!!.createUpdateNewExpense(
                        firebaseExpenseId, date, title, amount, paidBy, contribString, false, firebaseExpenseId, ExpenseOverviewActivity.groupBaseCurrency!!,1.0F)
                    SqlDbHelper(this).insertNewExpense(ExpenseOverviewActivity.getSqlGroupId!!,
                        firebaseExpenseId, date, title, amount, paidBy, contribString, false,
                        firebaseExpenseId, ExpenseOverviewActivity.groupBaseCurrency!!, ExpenseOverviewActivity.currencySymbol,1.0F)
                    intent.putExtra(NewExpenseCreationActivity.CONTRIBUTION_INTENT_DATA, contribString)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                    return true
                }
                else {
                    return false
                }
            }
            else -> return false
        }
    }

    private fun checkOkayToProceed(): Boolean {
        paidBy = binding.fromSelectionSpinner.selectedItem.toString()
        paidTo = binding.toSelectionSpinner.selectedItem.toString()
        if (paidBy == paidTo) {
            Toast.makeText(this, "Check recipients", Toast.LENGTH_SHORT).show()
            return false
        } else if (binding.settleAmountText.text.toString().isEmpty()) {
            Toast.makeText(this, "Check amount", Toast.LENGTH_SHORT).show()
            return false
        } else if (binding.settlementTitleText.text.toString().isEmpty()) {
            Toast.makeText(this, "Check title", Toast.LENGTH_LONG).show()
            return false
        }
        else {
            return true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.save_menu, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
