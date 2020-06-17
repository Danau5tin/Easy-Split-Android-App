package com.splitreceipt.myapplication

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.data.SqlDbHelper
import com.splitreceipt.myapplication.data.ExpenseAdapterData
import com.splitreceipt.myapplication.data.ParticipantBalanceData
import com.splitreceipt.myapplication.data.ScannedItemizedProductData
import com.splitreceipt.myapplication.databinding.ActivityExpenseViewBinding
import java.util.*
import kotlin.collections.ArrayList

class ExpenseViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseViewBinding
    private lateinit var contributionList: ArrayList<ExpenseAdapterData>
    private lateinit var itemizedProductData: ArrayList<ScannedItemizedProductData>
    private var sqlRowId: String = "-1"
    private var getTitleIntent: String = ""
    private var getTotalIntent: String = ""
    private var getPaidByIntent: String = ""
    private var getScannedIntent: Boolean = false
    private lateinit var participantAdapter: ExpenseViewParticipantAdapter
    private lateinit var expenseProdAdapter: ExpenseViewProductAdapter

    companion object {
        lateinit var expenseDate: String
        lateinit var contributionString: String
        const val expenseSqlIntentString: String = "expense_sql_id"
        const val expenseTitleIntentString: String = "expense_title"
        const val expenseTotalIntentString: String = "expense_total"
        const val expensePaidByIntentString: String = "expense_paid_by"
        const val expenseScannedIntentString: String = "expense_scanned"
        const val expenseReturnNewContributions: String = "expense_return_contributions"
        const val expenseReturnNewSettlements: String = "expense_return_settlements"
        const val expenseReturnEditSql: String = "expense_edit_sql"
        const val expenseReturnEditDate: String = "expense_edit_date"
        const val expenseReturnEditTitle: String = "expense_edit_title"
        const val expenseReturnEditTotal: String = "expense_edit_total"
        const val expenseReturnEditPaidBy: String = "expense_edit_paid_by"
        const val expenseReturnEditContributions: String = "expense_edit_contributions"
        const val EDIT_EXPENSE_INTENT_CODE = 30
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        contributionList = ArrayList()

        getTitleIntent = intent.getStringExtra(expenseTitleIntentString)!!
        getTotalIntent = intent.getStringExtra(expenseTotalIntentString)!!
        getPaidByIntent =
            intent.getStringExtra(expensePaidByIntentString)!!.toLowerCase(Locale.ROOT)
        sqlRowId = intent.getStringExtra(expenseSqlIntentString)!!
        getScannedIntent = intent.getBooleanExtra(expenseScannedIntentString, false)
        retrieveAllSqlDetails(getScannedIntent)
        if (getScannedIntent){
            binding.expenseProductRecy.visibility = View.VISIBLE
            binding.expenseProductDetailText.visibility = View.VISIBLE
            expenseProdAdapter = ExpenseViewProductAdapter(itemizedProductData)
            binding.expenseProductRecy.adapter = expenseProdAdapter
            binding.expenseProductRecy.layoutManager = LinearLayoutManager(this)
        } else {
            binding.expenseProductRecy.visibility = View.GONE
            binding.expenseProductDetailText.visibility = View.GONE
        }
        deconstructContributionString()

        val paidByText = "Paid by $getPaidByIntent"
        val valueText = "£$getTotalIntent" //TODO: Ensure the correct currency is being used
        binding.expenseValue.text = valueText
        binding.paidByText.text = paidByText

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getTitleIntent
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_back_arrow_white)
        }

        participantAdapter = ExpenseViewParticipantAdapter(contributionList)
        binding.expenseParticRecy.layoutManager = LinearLayoutManager(this)
        binding.expenseParticRecy.adapter = participantAdapter
    }

    private fun deconstructContributionString() {
        // Turn the solid contributions string into individual items for the recyclerViewAdapter
        contributionList.clear()
        val contributionsSplit = contributionString.split("/")
        for (contribution in contributionsSplit) {
            val individualContrib = contribution.split(",")
            val contributor = ExpenseOverviewActivity.changeNameToYou(individualContrib[0], true)
            val value = individualContrib[1]
            val newString = "$contributor contributed £$value" //TODO: Ensure the correct currency
            contributionList.add(ExpenseAdapterData(newString, value))
        }
    }
    private fun retrieveAllSqlDetails(scanned: Boolean){
        val dbHelper = SqlDbHelper(this)
        dbHelper.getExpenseDetails(sqlRowId)
        if(scanned){
            itemizedProductData = ArrayList()
            itemizedProductData = dbHelper.getReceiptProductDetails(sqlRowId, itemizedProductData)
        }
        dbHelper.close()
    }

    fun editExpense(view: View) {
        val intent = Intent(this, NewExpenseCreationActivity::class.java)
        intent.putExtra(NewExpenseCreationActivity.editIntentTitleString, getTitleIntent)
        intent.putExtra(NewExpenseCreationActivity.editIntentTotalString, getTotalIntent)
        intent.putExtra(NewExpenseCreationActivity.editIntentPaidByString, getPaidByIntent)
        intent.putExtra(NewExpenseCreationActivity.editIntentDateString, expenseDate)
        intent.putExtra(NewExpenseCreationActivity.editIntentContributionsString, contributionString)
        intent.putExtra(NewExpenseCreationActivity.editIntentSqlRowIdString, sqlRowId)
        intent.putExtra(NewExpenseCreationActivity.editIntentScannedBoolean, getScannedIntent)
        startActivityForResult(intent, EDIT_EXPENSE_INTENT_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_EXPENSE_INTENT_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // Below code retrieves the edited intents after they have already been saved.
                Toast.makeText(this, "Expense edited", Toast.LENGTH_SHORT).show()
                supportActionBar?.title = data?.getStringExtra(expenseReturnEditTitle)
                val total = SplitExpenseManuallyFragment.addStringZerosForDecimalPlace(data?.
                                                getStringExtra(expenseReturnEditTotal).toString())
                val totalWithCurrency = "£$total" //TODO: Ensure correct user currency is displayed
                binding.expenseValue.text = totalWithCurrency
                val newPaidBy = data?.getStringExtra(expenseReturnEditPaidBy)
                binding.paidByText.text = newPaidBy
                binding.dateText.text = data?.getStringExtra(expenseReturnEditDate)
                val newContribString = data?.getStringExtra(expenseReturnEditContributions).toString()

                val prevContributionString: String = contributionString // For readability
                val contributionsChanged: Boolean = prevContributionString != newContribString
                var calculatedContributions: String = newContribString
                val balSetHelper = BalanceSettlementHelper(this, ExpenseOverviewActivity.getSqlGroupId.toString())

                if (contributionsChanged) {
                    val paidByUnchanged: Boolean = newPaidBy == getPaidByIntent
                    if (paidByUnchanged) {
                        // Contributions have changed but paidBy is still the same
                        val previous: ArrayList<ParticipantBalanceData> = contribsToParticBalData(prevContributionString)
                        val new: ArrayList<ParticipantBalanceData> = contribsToParticBalData(newContribString)
                        calculatedContributions = generateNewContributions(previous, new, getPaidByIntent)
                    }
                    else{
                        // Contributions have changed. Paid by has changed also.
                        val reverseContribs = reversePriorExpenseAfterDeletion(prevContributionString, edit = true)
                        // Update balances after reversing the previous contributions.
                        balSetHelper.recalculateBalancesAndSettlements(reverseContribs)
                    }

                }
                // Set new contributions
                val settlementString = balSetHelper.recalculateBalancesAndSettlements(calculatedContributions)
                contributionString = calculatedContributions
                intent.putExtra(expenseReturnNewSettlements, settlementString)
                setResult(Activity.RESULT_OK, intent)
                deconstructContributionString()
                participantAdapter.notifyDataSetChanged()
                if (getScannedIntent){
                    expenseProdAdapter.notifyDataSetChanged()
                }

            }
        }
    }

    private fun contribsToParticBalData(prevContributionString: String): ArrayList<ParticipantBalanceData> {
        val participantList: ArrayList<ParticipantBalanceData> = ArrayList()
        val contributions = prevContributionString.split("/")
        for (contrib in contributions) {
            val splitContrib = contrib.split(",")
            val name = splitContrib[0]
            val value = splitContrib[1].toFloat()
            participantList.add(ParticipantBalanceData(name, value))
        }
        return participantList
    }

    fun generateNewContributions(prevContrib: ArrayList<ParticipantBalanceData>,
                                 newContrib: ArrayList<ParticipantBalanceData>, paidBy: String) : String{

        val stringBuilder = StringBuilder()
        for (prevParticipant in prevContrib) {
            for (newParticipant in newContrib) {
                val participantName = prevParticipant.name
                if (participantName == newParticipant.name) {
                    val change = newParticipant.balance - prevParticipant.balance
                    val rounded = ExpenseOverviewActivity.roundToTwoDecimalPlace(change)
                    stringBuilder.append("$participantName,")
                    stringBuilder.append("$rounded,")
                    stringBuilder.append("$paidBy/")
                }
            }
        }
        stringBuilder.deleteCharAt(stringBuilder.lastIndex)
        return stringBuilder.toString()
    }


    fun deleteExpense(view: View) {
        AlertDialog.Builder(this).apply {
            setIcon(R.drawable.vector_warning_yellow)
            setTitle("Are you sure?")
            setMessage("This expense will be deleted forever.")
            setPositiveButton("Yes delete", object: DialogInterface.OnClickListener{
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    val dbHelper = SqlDbHelper(context)
                    val prevContributionString: String = dbHelper.locatePriorContributions(sqlRowId)
                    dbHelper.deleteExpense(sqlRowId)

                    Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                    reversePriorExpenseAfterDeletion(prevContributionString)
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

    private fun reversePriorExpenseAfterDeletion(prevContribString: String, edit: Boolean=false) : String {
        // Reverse the expense contribution and add to the intent ready for
        val stringBuilder = StringBuilder()
        val spliPriorContrib = prevContribString.split("/")
        for (contribution in spliPriorContrib) {
            val individualContrib = contribution.split(",")
            val priorContributor = individualContrib[0]
            val priorContributee = individualContrib[2]
            val contribValue = individualContrib[1]
            stringBuilder.append("$priorContributee,")
            stringBuilder.append("$contribValue,")
            stringBuilder.append("$priorContributor/")
        }
        stringBuilder.deleteCharAt(stringBuilder.lastIndex)
        val newContribs = stringBuilder.toString()
        if (!edit) {
            // User selected delete button. Balances will be calculated back on Receipt Overview so finish this activity.
            intent.putExtra(expenseReturnNewContributions, newContribs)
            setResult(Activity.RESULT_OK, intent)
            this.finish()
            return ""
        } else {
            // User has edited the expense and also changed the person who paid so we must null the initial contributions.
            return newContribs
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }


}
