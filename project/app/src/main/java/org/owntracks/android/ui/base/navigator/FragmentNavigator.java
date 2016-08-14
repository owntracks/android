package org.owntracks.android.ui.base.navigator;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

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
public class FragmentNavigator extends BaseNavigator {

    private final Fragment fragment;

    public FragmentNavigator(Fragment fragment) {
        this.fragment = fragment;
    }

    @Override
    final FragmentActivity getActivity() {
        return fragment.getActivity();
    }

    @Override
    final FragmentManager getChildFragmentManager() {
        return fragment.getChildFragmentManager();
    }
}
