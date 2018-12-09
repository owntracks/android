package org.owntracks.android.ui.base;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.BR;
import org.owntracks.android.ui.base.navigator.Navigator;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;

public abstract class BaseSupportFragment<B extends ViewDataBinding, V extends MvvmViewModel> extends DaggerFragment {

    protected B binding;
    @Inject protected V viewModel;
    @Inject protected Navigator navigator;

    /* Use this method to inflate the content view for your Fragment.  */
    protected final View setAndBindContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @LayoutRes int layoutResId, Bundle savedInstanceState) {
        if(viewModel == null) { throw new IllegalStateException("viewModel must not be null and should be injected"); }
        binding = DataBindingUtil.inflate(inflater, layoutResId, container, false);
        binding.setVariable(BR.vm, viewModel);
        //noinspection unchecked
        viewModel.attachView((MvvmView) this, savedInstanceState);
        return binding.getRoot();
    }

    @Override
    @CallSuper
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(viewModel != null) { viewModel.saveInstanceState(outState); }
    }

    @Override
    @CallSuper
    public void onDestroyView() {
        super.onDestroyView();
        if(viewModel != null) { viewModel.detachView(); }
        binding = null;
        viewModel = null;
    }

    @Override
    @CallSuper
    public void onDestroy() {
        super.onDestroy();
    }

}
