package org.owntracks.android.ui.base.viewmodel

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.databinding.BaseObservable
import org.owntracks.android.ui.base.view.MvvmView

abstract class BaseViewModel<V : MvvmView> : BaseObservable(), MvvmViewModel<V> {

    protected var view: V? = null
        private set

    @CallSuper
    override fun attachView(savedInstanceState: Bundle?, view: V) {
        this.view = view
        savedInstanceState?.let { restoreInstanceState(it) }
    }

    @CallSuper
    override fun detachView() {
        view = null
    }

    override fun saveInstanceState(outState: Bundle) {}
    override fun restoreInstanceState(savedInstanceState: Bundle) {}
}