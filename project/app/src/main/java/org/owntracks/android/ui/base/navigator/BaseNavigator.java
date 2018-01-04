package org.owntracks.android.ui.base.navigator;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.app.Fragment;
import android.app.FragmentManager;

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

    abstract Activity getActivity();

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
        startActivity(activityClass, args, 0);
    }


    @Override
    public void startActivity(@NonNull Class<? extends Activity> activityClass, Bundle args, int flags) {
        Activity activity = getActivity();
        Intent intent = new Intent(activity, activityClass);
        intent.setFlags(flags);
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
        replaceFragmentInternal(getActivity().getFragmentManager(), containerId, fragment, null, args);
    }

    @Override
    public void replaceFragment(@IdRes int containerId, @NonNull Fragment fragment, @NonNull String fragmentTag, Bundle args) {
        replaceFragmentInternal(getActivity().getFragmentManager(), containerId, fragment, fragmentTag, args);
    }

    private void replaceFragmentInternal(FragmentManager fm, @IdRes int containerId, Fragment fragment, String fragmentTag, Bundle args) {
        if(args != null) { fragment.setArguments(args);}
        FragmentTransaction ft = fm.beginTransaction().replace(containerId, fragment, fragmentTag);
        ft.commit();
        fm.executePendingTransactions();
    }

    public Bundle getExtrasBundle(Intent intent) {
        return intent.hasExtra(Navigator.EXTRA_ARGS) ? intent.getBundleExtra(Navigator.EXTRA_ARGS) : new Bundle();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        getActivity().startActivityForResult(intent, requestCode);
    }

    @Override
    public void startActivityForResult(@NonNull Class<? extends Activity> activityClass, int requestCode, int flags) {
        Intent intent = new Intent(getActivity(), activityClass);
        intent.setFlags(flags);
        startActivityForResult(intent, requestCode);
    }
}
