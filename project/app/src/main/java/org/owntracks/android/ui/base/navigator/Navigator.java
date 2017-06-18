package org.owntracks.android.ui.base.navigator;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;

import com.mikepenz.materialdrawer.Drawer;

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
public interface Navigator {
    String EXTRA_ARGS = "_args";

    void startActivity(@NonNull Intent intent);
    void startActivity(@NonNull String action);
    void startActivity(@NonNull String action, @NonNull Uri uri);
    void startActivity(@NonNull Class<? extends Activity> activityClass);
    void startActivity(@NonNull Class<? extends Activity> activityClass, Bundle args);
    void startActivity(@NonNull Class<? extends Activity> activityClass, Bundle args, int flags);
    void startActivity(@NonNull Class<? extends Activity> activityClass, Parcelable args);

    void replaceFragment(@IdRes int containerId, @NonNull Fragment fragment, Bundle args);
    void replaceFragment(@IdRes int containerId, @NonNull Fragment fragment, @NonNull String fragmentTag, Bundle args);
    void replaceFragmentAndAddToBackStack(@IdRes int containerId, @NonNull Fragment fragment, Bundle args, String backstackTag);
    void replaceFragmentAndAddToBackStack(@IdRes int containerId, @NonNull Fragment fragment, @NonNull String fragmentTag, Bundle args, String backstackTag);

    void replaceChildFragment(@IdRes int containerId, @NonNull Fragment fragment, Bundle args);
    void replaceChildFragment(@IdRes int containerId, @NonNull Fragment fragment, @NonNull String fragmentTag, Bundle args);
    void replaceChildFragmentAndAddToBackStack(@IdRes int containerId, @NonNull Fragment fragment, Bundle args, String backstackTag);
    void replaceChildFragmentAndAddToBackStack(@IdRes int containerId, @NonNull Fragment fragment, @NonNull String fragmentTag, Bundle args, String backstackTag);

    Bundle getExtrasBundle(Intent intent);
}
