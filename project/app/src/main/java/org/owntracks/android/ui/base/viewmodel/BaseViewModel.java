package org.owntracks.android.ui.base.viewmodel;

import android.databinding.BaseObservable;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.ui.base.navigator.Navigator;
import org.owntracks.android.ui.base.view.MvvmView;

import javax.inject.Inject;
import javax.inject.Provider;

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
public abstract class BaseViewModel<V extends MvvmView> extends BaseObservable implements MvvmViewModel<V> {

    @Inject protected Provider<Navigator> navigator;

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
