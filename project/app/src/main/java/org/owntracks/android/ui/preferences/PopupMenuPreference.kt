package org.owntracks.android.ui.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.owntracks.android.R

class PopupMenuPreference : Preference {

  private lateinit var view: View

  private var textValue: String = ""

  @Suppress("unused")
  constructor(
      context: Context,
      attrs: AttributeSet?,
      defStyleAttr: Int,
      defStyleRes: Int
  ) : super(context, attrs, defStyleAttr, defStyleRes) {
    summaryProvider = SimpleSummaryProvider
  }

  @Suppress("unused")
  constructor(
      context: Context,
      attrs: AttributeSet?,
      defStyleAttr: Int
  ) : this(context, attrs, defStyleAttr, 0)

  @Suppress("unused")
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    summaryProvider = SimpleSummaryProvider
  }

  @Suppress("unused") constructor(context: Context) : this(context, null)

  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    view = holder.itemView
    super.onBindViewHolder(holder)
  }

  override fun onSetInitialValue(defaultValue: Any?) {
    setValue(getPersistedString((defaultValue ?: "") as String))
  }

  fun setValue(value: String) {
    persistString(value)
    textValue = value
    notifyChanged()
  }

  fun showMenu(clearAction: () -> Unit, selectAction: () -> Unit) {
    PopupMenu(context, view)
        .apply {
          menuInflater.inflate(R.menu.picker, menu)
          setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
              R.id.clear -> {
                clearAction()
                setValue("")
              }
              R.id.select -> {
                selectAction()
              }
            }
            true
          }
        }
        .show()
  }

  object SimpleSummaryProvider : SummaryProvider<PopupMenuPreference> {
    override fun provideSummary(preference: PopupMenuPreference): CharSequence =
        preference.textValue.ifBlank { preference.context.getString(R.string.preferencesNotSet) }
  }
}
