package org.owntracks.android.ui.base.viewmodel;

import android.databinding.BaseObservable;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.ui.base.navigator.Navigator;
import org.owntracks.android.ui.base.view.MvvmView;

import javax.inject.Inject;

public abstract class BaseViewModel<V extends MvvmView> extends BaseObservable implements MvvmViewModel<V> {

    @Inject protected Navigator navigator;

    private V mView;


    public V getView() {
        return mView;
    }

    @Override
    @CallSuper
    public void attachView(@NonNull V view, @Nullable Bundle savedInstanceState) {
        mView = view;
        if(savedInstanceState != null) { restoreInstanceState(savedInstanceState); }
    }

    @Override
    @CallSuper
    public void detachView() {
        mView = null;
    }

    @Override
    public void saveInstanceState(@NonNull Bundle outState) { }

    @Override
    public void restoreInstanceState(@NonNull Bundle savedInstanceState) { }
}
