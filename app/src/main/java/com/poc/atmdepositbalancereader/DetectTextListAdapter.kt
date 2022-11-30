package com.poc.atmdepositbalancereader

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView

class DetectTextListAdapter(val context: Context, val listValue: ArrayList<String>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return DetectedTextViewHolder(
            LayoutInflater.from(context).inflate(R.layout.rc_detected_list, parent,false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val detectedTextViewHolder = holder as DetectedTextViewHolder
        detectedTextViewHolder.atv_DetectedValue?.text = listValue[position]
    }

    override fun getItemCount(): Int {
        return listValue.size
    }

    class DetectedTextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val atv_DetectedValue = view.findViewById<AppCompatTextView>(R.id.atv_DetectedValue)
    }
}