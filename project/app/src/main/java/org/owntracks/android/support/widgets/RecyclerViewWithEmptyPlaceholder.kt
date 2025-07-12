package org.owntracks.android.support.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView

// Recycler view that dynamically shows/hides an empty placeholder view
class RecyclerViewWithEmptyPlaceholder : RecyclerView {
  var emptyView: View? = null
  private val emptyObserver: AdapterDataObserver =
      object : AdapterDataObserver() {
        fun checkEmpty() {
          val adapter = adapter
          if (adapter != null && emptyView != null) {
            if (adapter.itemCount == 0) {
              emptyView!!.visibility = VISIBLE
              this@RecyclerViewWithEmptyPlaceholder.visibility = GONE
            } else {
              emptyView!!.visibility = GONE
              this@RecyclerViewWithEmptyPlaceholder.visibility = VISIBLE
            }
          }
        }

        override fun onChanged() {
          checkEmpty()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
          super.onItemRangeInserted(positionStart, itemCount)
          checkEmpty()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
          super.onItemRangeRemoved(positionStart, itemCount)
          checkEmpty()
        }
      }

  override fun setAdapter(adapter: Adapter<*>?) {
    super.setAdapter(adapter)
    adapter?.registerAdapterDataObserver(emptyObserver)
    emptyObserver.onChanged()
  }

  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

  constructor(
      context: Context,
      attrs: AttributeSet,
      defStyle: Int
  ) : super(context, attrs, defStyle)
}
