package com.splitreceipt.myapplication

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.splitreceipt.myapplication.data.AccountData

class AccountScreenAdapter(val accountNameList: ArrayList<AccountData>) : RecyclerView.Adapter<AccountScreenAdapter.AccountViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.account_screen_recy_row, parent, false)
        return AccountViewHolder(parent.context, view)
    }

    override fun getItemCount(): Int {
        return accountNameList.size
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.titleText.text = accountNameList[position].name
        holder.sqlId = accountNameList[position].sqlId
        holder.firebaseId = accountNameList[position].firebaseId
        holder.sqlUser = accountNameList[position].sqlUser
    }

    class AccountViewHolder(var context: Context, itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val titleText: TextView = itemView.findViewById(R.id.titleText)
        var sqlId: String = "0"
        var firebaseId: String = "0"
        var sqlUser: String = "unknown"

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val intent = Intent(context, ReceiptOverviewActivity::class.java)
            //TODO: Ensure the firebase ID Is relevant and a static variable
            intent.putExtra(AccountScreenActivity.sqlIntentString, sqlId)
            intent.putExtra("FirebaseID", sqlId)
            intent.putExtra(AccountScreenActivity.userIntentString, sqlUser)
            intent.putExtra(AccountScreenActivity.accountNameIntentString, titleText.text.toString())
            context.startActivity(intent)
        }
    }
}