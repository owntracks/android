package org.owntracks.android.ui.base;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.injection.components.DaggerFragmentComponent;
import org.owntracks.android.injection.components.FragmentComponent;
import org.owntracks.android.injection.modules.FragmentModule;
import org.owntracks.android.ui.base.navigator.Navigator;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

import javax.inject.Inject;

/* Copyright 2016 Patrick Löwenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

/* Base class for Fragments when using a view model with data binding.
 * This class provides the binding and the view model to the subclass. The
 * view model is injected and the binding is created when the content view is set.
 * Each subclass therefore has to call the following code in onCreateView():
 *    if(viewModel == null) { fragmentComponent().inject(this); }
 *    return setAndBindContentView(inflater, container, R.layout.my_fragment_layout, savedInstanceState);
 *
 * After calling these methods, the binding and the view model is initialized.
 * saveInstanceState() and restoreInstanceState() methods of the view model
 * are automatically called in the appropriate lifecycle events when above calls
 * are made.
 *
 * Your subclass must implement the MvvmView implementation that you use in your
 * view model. */
public abstract class BaseFragment<B extends ViewDataBinding, V extends MvvmViewModel> extends Fragment {

    protected B binding;
    @Inject protected V viewModel;
    @Inject protected Navigator navigator;

    private FragmentComponent mFragmentComponent;

    protected final FragmentComponent fragmentComponent() {
        if(mFragmentComponent == null) {
            mFragmentComponent = DaggerFragmentComponent.builder()
                    .appComponent(App.getAppComponent())
                    .fragmentModule(new FragmentModule(this))
                    .build();
        }

        return mFragmentComponent;
    }

    /* Use this method to inflate the content view for your Fragment. This method also handles
     * creating the binding, setting the view model on the binding and attaching the view. */
    protected final View setAndBindContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @LayoutRes int layoutResId, Bundle savedInstanceState) {
        if(viewModel == null) { throw new IllegalStateException("viewModel must not be null and should be injected via fragmentComponent().inject(this)"); }
        binding = DataBindingUtil.inflate(inflater, layoutResId, container, false);
        binding.setVariable(BR.vm, viewModel);
        //noinspection unchecked
        viewModel.attachView((MvvmView) this, savedInstanceState);
        return binding.getRoot();
    }

    @Override
    @CallSuper
    public void onSaveInstanceState(Bundle outState) {
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
        mFragmentComponent = null;
        super.onDestroy();
    }
}
