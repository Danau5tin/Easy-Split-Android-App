package com.splitreceipt.myapplication

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import com.splitreceipt.myapplication.ExpenseOverviewActivity.Companion.currentGroupBaseCurrency
import com.splitreceipt.myapplication.data.Expense
import com.splitreceipt.myapplication.helper_classes.SqlDbHelper
import com.splitreceipt.myapplication.databinding.ActivitySettleGroupBinding
import com.splitreceipt.myapplication.helper_classes.CurrencyHelper
import com.splitreceipt.myapplication.helper_classes.CurrencyHelper.EXCHANGE_RATE_OF_1
import com.splitreceipt.myapplication.helper_classes.DateSelectionCleaner.retrieveTodaysDate

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
        participantList = SqlDbHelper(this).retrieveGroupParticipants(participantList, ExpenseOverviewActivity.currentSqlGroupId!!)
        val spinnerAdapter = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, participantList)
        binding.fromSelectionSpinner.adapter = spinnerAdapter
        binding.toSelectionSpinner.adapter = spinnerAdapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuSave -> {
                if (okayToProceed()) {
                    val expense = returnBasicExpense()
                    //TODO: Allow settlements to be made in any currency instead of defaulting to the base currency.
                    val currencyDetails = CurrencyHelper.CurrencyDetails(currentGroupBaseCurrency!!, CurrencyHelper.returnUiSymbol(currentGroupBaseCurrency!!), EXCHANGE_RATE_OF_1)
                    expense.setUpNewExpense(binding.settleAmountText, paidTo, currencyDetails)

                    ExpenseOverviewActivity.firebaseDbHelper!!.insertOrUpdateExpense(expense)
                    SqlDbHelper(this).insertNewExpense(expense)
                    intent.putExtra(NewExpenseCreationActivity.CONTRIBUTION_INTENT_DATA, expense.contribs)
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

    private fun returnBasicExpense() : Expense {
        val date = retrieveTodaysDate(this)
        val title = binding.settlementTitleText.text.toString()
        val lastEdit: String = System.currentTimeMillis().toString()
        return Expense(date, title, paidBy, false, lastEdit)
    }

    private fun okayToProceed(): Boolean {
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
