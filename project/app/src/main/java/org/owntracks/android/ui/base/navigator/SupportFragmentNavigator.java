package org.owntracks.android.ui.base.navigator;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class SupportFragmentNavigator extends BaseNavigator {

    private final Fragment fragment;

    public SupportFragmentNavigator(Fragment fragment) {
        this.fragment = fragment;
    }

    @Override
    final Activity getActivity() {
        return fragment.getActivity();
    }




}
