package org.owntracks.android.ui.base

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

abstract class BaseRecyclerViewHolder<T>(
    private val binding: ViewDataBinding,
    private val bindingVariable: Int
) : RecyclerView.ViewHolder(binding.root) {
  fun bind(item: T, clickListenerRecyclerView: ClickListener<T>) {
    binding.setVariable(bindingVariable, item)
    binding.root.setOnClickListener { clickListenerRecyclerView.onClick(item, binding.root, false) }
    binding.root.setOnLongClickListener {
      clickListenerRecyclerView.onClick(item, binding.root, true)
    }
    binding.executePendingBindings()
  }
}
