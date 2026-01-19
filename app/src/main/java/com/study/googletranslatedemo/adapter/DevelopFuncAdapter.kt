package com.study.googletranslatedemo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.study.googletranslatedemo.data.DevelopFunc
import com.study.googletranslatedemo.databinding.ItemFunctionBinding

class DevelopFuncAdapter(
    private val list: List<DevelopFunc>,
    private val onClick: (DevelopFunc) -> Unit
) : RecyclerView.Adapter<DevelopFuncAdapter.DevelopFuncHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DevelopFuncHolder {
        return DevelopFuncHolder(
            ItemFunctionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: DevelopFuncHolder,
        position: Int
    ) {
        holder.bindData(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }


    inner class DevelopFuncHolder(
        private val binding: ItemFunctionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(data: DevelopFunc) {
            binding.btnFunction.text = data.title
            binding.btnFunction.setOnClickListener { onClick.invoke(data) }
        }
    }
}