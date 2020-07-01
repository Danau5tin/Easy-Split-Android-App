package com.splitreceipt.myapplication

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.databinding.ActivityBalanceOverviewBinding

class BalanceOverviewActivity : AppCompatActivity(), BalanceOverviewAdapter.balanceRowClick {

    private lateinit var binding: ActivityBalanceOverviewBinding
    private lateinit var adapter: BalanceOverviewAdapter
    private val settleResult: Int = 10

    companion object {
        const val balanceResult = "balanceResult"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBalanceOverviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.toolbar))
        val actionBar: ActionBar? = supportActionBar

        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_back_arrow_white)
        }

        adapter = BalanceOverviewAdapter(ExpenseOverviewActivity.settlementArray, this)
        binding.balanceRecyler.layoutManager = LinearLayoutManager(this)
        binding.balanceRecyler.adapter = adapter
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onBalRowClick(pos: Int) {
        intent.putExtra(balanceResult, true)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

}
