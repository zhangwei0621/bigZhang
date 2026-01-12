package com.study.googletranslatedemo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.study.googletranslatedemo.data.DemoFunc
import com.study.googletranslatedemo.databinding.ItemFunctionBinding

class DemoFuncAdapter(
    private val list: List<DemoFunc>,
    private val onClick: (DemoFunc) -> Unit
) : RecyclerView.Adapter<DemoFuncAdapter.DemoFuncHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DemoFuncHolder {
        return DemoFuncHolder(
            ItemFunctionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: DemoFuncHolder,
        position: Int
    ) {
        holder.bindData(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }


    inner class DemoFuncHolder(
        private val binding: ItemFunctionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(data: DemoFunc) {
            binding.btnFunction.text = data.title
            binding.btnFunction.setOnClickListener { onClick.invoke(data) }
        }
    }
}