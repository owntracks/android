/*
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Original taken from takisoft/preferencex-android at https://github.com/takisoft/preferencex-android

This file modified from
https://github.com/takisoft/preferencex-android/blob/205ec8110a503f9af50a25ba7c67df05adfe02bb/preferencex/src/main/java/com/takisoft/preferencex/PreferenceFragmentCompatMasterSwitch.java
*/
package org.owntracks.android.ui.preferences

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.SwitchCompat
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceScreen
import com.takisoft.preferencex.PreferenceFragmentCompat
import org.owntracks.android.R

abstract class PreferenceFragmentCompatMasterSwitch : PreferenceFragmentCompat() {
    private val masterSwitch: MasterSwitch = createMasterSwitch()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        if (view is ViewGroup) {
            masterSwitch.onCreateView(inflater, view)
            masterSwitch.refreshMasterSwitch()
        } else {
            throw IllegalArgumentException("The root element must be an instance of ViewGroup")
        }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        masterSwitch.onDestroyView()
    }

    override fun setPreferenceScreen(preferenceScreen: PreferenceScreen) {
        super.setPreferenceScreen(preferenceScreen)
        masterSwitch.refreshMasterSwitch()
    }

    private fun createMasterSwitch(): MasterSwitch {
        return MasterSwitch()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        masterSwitch.updateViews()
    }

    open inner class MasterSwitch {
        private val attributes =
                intArrayOf(R.attr.pref_masterSwitchBackgroundOn, R.attr.pref_masterSwitchBackgroundOff)
        private var masterView: View? = null
        private var masterTitle: TextView? = null
        private var switchCompat: SwitchCompat? = null
        var preferenceDataStore: PreferenceDataStore? = null
        private var isCheckedSet = false
        private var isChecked = false
        /**
         * Returns the callback to be invoked when this preference is changed by the user (but before
         * the internal state has been updated).
         *
         * @return The callback to be invoked
         */
        /**
         * Sets the callback to be invoked when this preference is changed by the user (but before
         * the internal state has been updated).
         *
         * @param onMasterSwitchChangeListener The callback to be invoked
         */
        var onPreferenceChangeListener: OnMasterSwitchChangeListener? = null

        internal fun onCreateView(inflater: LayoutInflater, group: ViewGroup) {
            if (group.findViewById<View?>(com.takisoft.preferencex.R.id.pref_master_switch_view) != null) {
                return
            }
            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(R.attr.pref_masterSwitchStyle, typedValue, true)
            val ctx = ContextThemeWrapper(
                    requireContext(),
                    if (typedValue.resourceId != 0) typedValue.resourceId else R.style.PreferenceMasterSwitch
            )
            //ctx.getTheme().applyStyle(typedValue.resourceId != 0 ? typedValue.resourceId : R.style.PreferenceMasterSwitch, true);
            val inf = inflater.cloneInContext(ctx)
            masterView = inf.inflate(R.layout.preference_list_master_switch, group, false)
            masterTitle = masterView!!.findViewById(R.id.title)
            switchCompat = masterView!!.findViewById(R.id.switchWidget)
            setMasterBackground(ctx)
            masterView!!.setOnClickListener { v -> performClick(v) }
            group.addView(
                    masterView,
                    0,
                    ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            )
        }

        internal fun onDestroyView() {
            masterView = null
            masterTitle = null
            switchCompat = null
            isCheckedSet = false
        }

        private fun setMasterBackground(ctx: Context) {
            val a = ctx.obtainStyledAttributes(attributes)
            val colorOn = a.getColor(a.getIndex(0), 0)
            val colorOff = a.getColor(a.getIndex(1), 0)
            a.recycle()
            masterView?.apply {
                background = StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_selected), ColorDrawable(colorOn))
                    addState(intArrayOf(), ColorDrawable(colorOff))
                }
            }
        }

        internal fun refreshMasterSwitch() {
            val preferenceScreen = preferenceScreen ?: return
            val checked = getPersistedBoolean(false)
            if (checked != isChecked || !isCheckedSet) {
                isCheckedSet = true
                isChecked = checked
                masterTitle?.apply {
                    text = preferenceScreen.title
                }
//                getPreferenceScreen().notifyDependencyChange(false)
            }
            updateViews()
        }

        internal fun updateViews() {
            masterView?.also { view ->
                view.findViewById<ImageView>(com.takisoft.preferencex.R.id.icon)?.run { setImageDrawable(icon) }

                switchCompat?.also { switch ->
                    view.isSelected = isChecked
                    switch.isChecked = isChecked
                }
                preferenceScreen?.also { screen ->
                    view.findViewById<View>(androidx.preference.R.id.icon_frame).visibility =
                            if (screen.isIconSpaceReserved) View.VISIBLE else View.GONE
                }
            }
            masterTitle?.apply {
                text =
                        if (isChecked) getString(R.string.preferenceOn) else getString(R.string.preferenceOff)
                isSingleLine = isSingleLineTitle
            }
        }

        private fun notifyChanged() {
            updateViews()

            // TODO notify internal listener
        }

        private fun performClick(v: View) {
            performClick()
            updateViews()
        }

        private fun performClick() {
            onClick()

            /*if (mOnClickListener != null && mOnClickListener.onPreferenceClick(this)) {
                return;
            }*/
        }

        protected fun onClick() {
            val newValue = !isChecked
            if (callChangeListener(newValue)) {
                setChecked(newValue)
            }
        }

        /**
         * Sets the checked state and saves it.
         *
         * @param checked The checked state
         */
        private fun setChecked(checked: Boolean) {
            val changed = isChecked != checked
            if (changed) {
                isChecked = checked
                persistBoolean(checked)
                notifyChanged()
            }
        }

        /**
         * Attempts to persist a [Boolean] if this preference is persistent.
         *
         *
         * The returned value doesn't reflect whether the given value was persisted, since we may not
         * necessarily commit if there will be a batch commit later.
         *
         * @param value The value to persist
         * @return `true` if the preference is persistent, `false` otherwise
         * @see .getPersistedBoolean
         */
        private fun persistBoolean(value: Boolean): Boolean {
            if (!shouldPersist()) {
                return false
            }
            if (value == getPersistedBoolean(!value)) {
                return true
            }
            val dataStore = preferenceDataStore
            if (dataStore != null) {
                dataStore.putBoolean(key, value)
            } else {
                val editor = preferenceManager.sharedPreferences.edit()
                editor.putBoolean(key, value)
                editor.apply()
            }
            return true
        }

        /**
         * Attempts to get a persisted [Boolean] if this preference is persistent.
         *
         * @param defaultReturnValue The default value to return if either this preference is not
         * persistent or this preference is not in the SharedPreferences.
         * @return The value from the storage or the default return value
         * @see .persistBoolean
         */
        private fun getPersistedBoolean(defaultReturnValue: Boolean): Boolean {
            if (!shouldPersist()) {
                return defaultReturnValue
            }
            val dataStore = preferenceDataStore
            return dataStore?.getBoolean(key, defaultReturnValue)
                    ?: preferenceManager.sharedPreferences.getBoolean(key, defaultReturnValue)
        }

        private fun shouldPersist(): Boolean {
            return preferenceManager != null && preferenceScreen.isPersistent && preferenceScreen.hasKey()
        }

        private val key: String
            get() = preferenceScreen.key

        /**
         * Call this method after the user changes the preference, but before the internal state is
         * set. This allows the client to ignore the user value.
         *
         * @param newValue The new value of this preference
         * @return `true` if the user value should be set as the preference value (and persisted)
         */
        private fun callChangeListener(newValue: Boolean): Boolean {
            return onPreferenceChangeListener == null || onPreferenceChangeListener!!.onMasterSwitchChange(
                    newValue
            )
        }

        val title: CharSequence?
            get() = if (preferenceScreen != null) preferenceScreen.title else null
        val icon: Drawable?
            get() = if (preferenceScreen != null) preferenceScreen.icon else null
        private val isSingleLineTitle: Boolean
            get() = preferenceScreen != null && preferenceScreen.isSingleLineTitle
    }

    /**
     * Interface definition for a callback to be invoked when the value of this
     * [Preference] has been changed by the user and is about to be set and/or persisted.
     * This gives the client a chance to prevent setting and/or persisting the value.
     */
    interface OnMasterSwitchChangeListener {
        /**
         * Called when a preference has been changed by the user. This is called before the state
         * of the preference is about to be updated and before the state is persisted.
         *
         * @param newValue The new value of the preference
         * @return `true` to update the state of the preference with the new value
         */
        fun onMasterSwitchChange(newValue: Boolean): Boolean
    }
}