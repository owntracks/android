package org.owntracks.android.ui.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

typealias ClickHasBeenHandled = Boolean

abstract class BaseRecyclerViewAdapterWithClickHandler<T, VH : BaseRecyclerViewHolder<T>>(
    private val clickListener: ClickListener<T>,
    private val viewHolderConstructor: (ViewDataBinding) -> VH,
    @LayoutRes private val viewHolderLayout: Int
) : RecyclerView.Adapter<VH>() {
  private val itemList: MutableList<T> = mutableListOf()

  fun setData(items: Collection<T>) {
    itemList.clear()
    itemList.addAll(items)
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
      viewHolderConstructor(
          DataBindingUtil.inflate(
              LayoutInflater.from(parent.context), viewHolderLayout, parent, false))

  override fun onBindViewHolder(holder: VH, position: Int) {
    holder.bind(itemList[position], clickListener)
  }

  override fun getItemCount(): Int = itemList.size

  interface ClickListener<T> {
    fun onClick(thing: T, view: View, longClick: Boolean): ClickHasBeenHandled
  }
}
