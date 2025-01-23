package com.study.googletranslatedemo.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.study.googletranslatedemo.databinding.ItemLanguageBinding
import com.study.googletranslatedemo.dialog.LanguageData
import java.util.Locale

/**
 * author ZhangWei
 * date 2025-01-23
 */
class LanguageAdapter(
    private val onClick: (LanguageData) -> Unit = {},
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var dataList: MutableList<LanguageData> = mutableListOf()
    override fun getItemCount() = dataList.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return LanguageHolder(ItemLanguageBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val languageData = dataList[position]
        if (holder is LanguageHolder) {
            holder.binding.ivDownload.visibility =
                if (languageData.isDownload) View.VISIBLE else View.GONE
            holder.binding.tvLan.text = Locale.forLanguageTag(languageData.name).displayName
            holder.binding.root.setOnClickListener { onClick(languageData) }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<LanguageData>) {
        if (newData.isNotEmpty()) {
            dataList.clear()
            dataList.addAll(newData)
            notifyDataSetChanged()
        }
    }

    inner class LanguageHolder(val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root)

}