package org.owntracks.android.ui.base.navigator;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

public class FragmentNavigator extends BaseNavigator {

    private final Fragment fragment;

    public FragmentNavigator(Fragment fragment) {
        this.fragment = fragment;
    }

    @Override
    final Activity getActivity() {
        return fragment.getActivity();
    }



}
