package org.owntracks.android.ui.base.navigator;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

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
public abstract class BaseNavigator implements Navigator {

    abstract AppCompatActivity getActivity();
    abstract FragmentManager getChildFragmentManager();

    @Override
    public final void startActivity(@NonNull Intent intent) {
        getActivity().startActivity(intent);
    }

    @Override
    public final void startActivity(@NonNull String action) {
        getActivity().startActivity(new Intent(action));
    }

    @Override
    public final void startActivity(@NonNull String action, @NonNull Uri uri) {
        getActivity().startActivity(new Intent(action, uri));
    }

    @Override
    public final void startActivity(@NonNull Class<? extends Activity> activityClass) {
        startActivity(activityClass, null);
    }

    @Override
    public final void startActivity(@NonNull Class<? extends Activity> activityClass, Bundle args) {
        Activity activity = getActivity();
        Intent intent = new Intent(activity, activityClass);
        if(args != null) { intent.putExtra(EXTRA_ARGS, args); }
        activity.startActivity(intent);
    }

    @Override
    public final void startActivity(@NonNull Class<? extends Activity> activityClass, Parcelable args) {
        Activity activity = getActivity();
        Intent intent = new Intent(activity, activityClass);
        if(args != null) { intent.putExtra(EXTRA_ARGS, args); }
        activity.startActivity(intent);
    }

    @Override
    public final void replaceFragment(@IdRes int containerId, @NonNull Fragment fragment, Bundle args) {
        replaceFragmentInternal(getActivity().getSupportFragmentManager(), containerId, fragment, null, args, false, null);
    }

    @Override
    public void replaceFragment(@IdRes int containerId, @NonNull Fragment fragment, @NonNull String fragmentTag, Bundle args) {
        replaceFragmentInternal(getActivity().getSupportFragmentManager(), containerId, fragment, fragmentTag, args, false, null);
    }

    @Override
    public final void replaceFragmentAndAddToBackStack(@IdRes int containerId, @NonNull Fragment fragment, Bundle args, String backstackTag) {
        replaceFragmentInternal(getActivity().getSupportFragmentManager(), containerId, fragment, null, args, true, backstackTag);
    }

    @Override
    public void replaceFragmentAndAddToBackStack(@IdRes int containerId, @NonNull Fragment fragment, @NonNull String fragmentTag, Bundle args, String backstackTag) {
        replaceFragmentInternal(getActivity().getSupportFragmentManager(), containerId, fragment, fragmentTag, args, true, backstackTag);
    }

    @Override
    public final void replaceChildFragment(@IdRes int containerId, @NonNull Fragment fragment, Bundle args) {
        replaceFragmentInternal(getChildFragmentManager(), containerId, fragment, null, args, false, null);
    }

    @Override
    public void replaceChildFragment(@IdRes int containerId, @NonNull Fragment fragment, @NonNull String fragmentTag, Bundle args) {
        replaceFragmentInternal(getChildFragmentManager(), containerId, fragment, fragmentTag, args, false, null);
    }

    @Override
    public final void replaceChildFragmentAndAddToBackStack(@IdRes int containerId, @NonNull Fragment fragment, Bundle args, String backstackTag) {
        replaceFragmentInternal(getChildFragmentManager(), containerId, fragment, null, args, true, backstackTag);
    }

    @Override
    public final void replaceChildFragmentAndAddToBackStack(@IdRes int containerId, @NonNull Fragment fragment, @NonNull String fragmentTag, Bundle args, String backstackTag) {
        replaceFragmentInternal(getChildFragmentManager(), containerId, fragment, fragmentTag, args, true, backstackTag);
    }

    private void replaceFragmentInternal(FragmentManager fm, @IdRes int containerId, Fragment fragment, String fragmentTag, Bundle args, boolean addToBackstack, String backstackTag) {
        if(args != null) { fragment.setArguments(args);}
        FragmentTransaction ft = fm.beginTransaction().replace(containerId, fragment, fragmentTag);
        if(addToBackstack) {
            ft.addToBackStack(backstackTag).commitNow();
        } else {
            ft.commit();
            fm.executePendingTransactions();
        }
    }
}
