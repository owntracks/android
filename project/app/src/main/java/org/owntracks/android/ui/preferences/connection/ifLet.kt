package org.owntracks.android.ui.preferences.connection

// https://stackoverflow.com/questions/35513636/multiple-variable-let-in-kotlin
inline fun <T : Any> ifLet(vararg elements: T?, closure: (List<T>) -> Unit) {
    if (elements.all { it != null }) {
        closure(elements.filterNotNull())
    }
}