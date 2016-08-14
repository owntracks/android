package org.owntracks.android.ui.base;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.injection.components.DaggerViewHolderComponent;
import org.owntracks.android.injection.components.ViewHolderComponent;
import org.owntracks.android.injection.modules.ViewHolderModule;
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

/* Base class for ViewHolders when using a view model with data binding.
 * This class provides the binding and the view model to the subclass. The
 * view model is injected and the binding is created when the content view is bound.
 * Each subclass therefore has to call the following code in the constructor:
 *    viewHolderComponent().inject(this);
 *    bindContentView(view);
 *
 * After calling these methods, the binding and the view model is initialized.
 * saveInstanceState() and restoreInstanceState() are not called/used for ViewHolder
 * view models.
 *
 * Your subclass must implement the MvvmView implementation that you use in your
 * view model. */
public abstract class BaseViewHolder<B extends ViewDataBinding, V extends MvvmViewModel> extends RecyclerView.ViewHolder {

    protected B binding;
    @Inject protected V viewModel;

    private ViewHolderComponent viewHolderComponent;

    private View itemView = null;

    public BaseViewHolder(View itemView) {
        super(itemView);
        this.itemView = itemView;
    }

    protected final ViewHolderComponent viewHolderComponent() {
        if(viewHolderComponent == null) {
            viewHolderComponent = DaggerViewHolderComponent.builder()
                    .appComponent(App.getAppComponent())
                    .viewHolderModule(new ViewHolderModule(itemView))
                    .build();

            itemView = null;
        }

        return viewHolderComponent;
    }

    /* Use this method to create the binding for the ViewHolder. This method also handles
     * setting the view model on the binding and attaching the view. */
    protected final void bindContentView(@NonNull View view) {
        if(viewModel == null) { throw new IllegalStateException("viewModel must not be null and should be injected via viewHolderComponent().inject(this)"); }
        binding = DataBindingUtil.bind(view);
        binding.setVariable(BR.vm, viewModel);
        //noinspection unchecked
        viewModel.attachView((MvvmView) this, null);
    }

    public final V viewModel() { return viewModel; }
}
