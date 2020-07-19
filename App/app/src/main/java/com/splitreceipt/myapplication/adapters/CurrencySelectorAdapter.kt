package com.splitreceipt.myapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.splitreceipt.myapplication.R

class CurrencySelectorAdapter(var currencyList: MutableList<String>, var onCurClick: OnCureClick) : RecyclerView.Adapter<CurrencySelectorAdapter.CurrencyViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.currency_selector_row, parent, false)
        return CurrencyViewHolder(
            view,
            onCurClick
        )
    }

    override fun getItemCount(): Int {
        return currencyList.size
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        holder.textView.text = currencyList[position]
        holder.code = currencyList[position].substring(0,3)
    }

    fun updateList(newList: MutableList<String>) {
        currencyList = newList
        notifyDataSetChanged()
    }

    class CurrencyViewHolder(itemView: View, var onCurClick: OnCureClick) : RecyclerView.ViewHolder(itemView), View.OnClickListener{
        val textView: TextView = itemView.findViewById(R.id.currencySelectorText)
        var code: String = ""

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            onCurClick.onRowClick(code)
        }

    }

    interface OnCureClick{
        fun onRowClick(code: String)
    }
}