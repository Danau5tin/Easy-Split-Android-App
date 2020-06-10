package com.splitreceipt.myapplication

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.splitreceipt.myapplication.data.GroupData

class GroupScreenAdapter(val groupNameList: ArrayList<GroupData>) : RecyclerView.Adapter<GroupScreenAdapter.GroupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.group_screen_recy_row, parent, false)
        return GroupViewHolder(parent.context, view)
    }

    override fun getItemCount(): Int {
        return groupNameList.size
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.titleText.text = groupNameList[position].name
        holder.sqlId = groupNameList[position].sqlId
        holder.firebaseId = groupNameList[position].firebaseId
        holder.sqlUser = groupNameList[position].sqlUser
    }

    class GroupViewHolder(var context: Context, itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

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
            intent.putExtra(GroupScreenActivity.sqlIntentString, sqlId)
            intent.putExtra(GroupScreenActivity.firebaseIntentString, firebaseId)
            intent.putExtra(GroupScreenActivity.userIntentString, sqlUser)
            intent.putExtra(GroupScreenActivity.groupNameIntentString, titleText.text.toString())
            context.startActivity(intent)
        }
    }
}