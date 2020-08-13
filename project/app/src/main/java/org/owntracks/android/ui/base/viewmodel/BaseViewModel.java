package org.owntracks.android.ui.base.viewmodel;

import androidx.databinding.BaseObservable;
import android.os.Bundle;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.owntracks.android.ui.base.navigator.Navigator;
import org.owntracks.android.ui.base.view.MvvmView;

import javax.inject.Inject;

public abstract class BaseViewModel<V extends MvvmView> extends BaseObservable implements MvvmViewModel<V> {

    @Inject protected Navigator navigator;

    private V mView;


    protected V getView() {
        return mView;
    }

    @Override
    @CallSuper
    public void attachView(@Nullable Bundle savedInstanceState, @NonNull V view) {
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
