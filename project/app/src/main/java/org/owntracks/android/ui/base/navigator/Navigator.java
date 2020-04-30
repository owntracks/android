package org.owntracks.android.ui.base.navigator;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


public class Navigator {
    private static final String EXTRA_ARGS = "_args";

    private final Activity activity;

    public Navigator(AppCompatActivity activity) {
        this.activity = activity;
    }

    private Activity getActivity() {
        return activity;
    }

    public final void startActivity(@NonNull Intent intent) {
        getActivity().startActivity(intent);
    }

    public final void startActivity(@NonNull String action) {
        getActivity().startActivity(new Intent(action));
    }

    public final void startActivity(@NonNull String action, @NonNull Uri uri) {
        getActivity().startActivity(new Intent(action, uri));
    }

    public final void startActivity(@NonNull Class<? extends Activity> activityClass) {
        startActivity(activityClass, null);
    }

    public final void startActivity(@NonNull Class<? extends Activity> activityClass, Bundle args) {
        startActivity(activityClass, args, 0);
    }

    public void startActivity(@NonNull Class<? extends Activity> activityClass, Bundle args, int flags) {
        Activity activity = getActivity();
        Intent intent = new Intent(activity, activityClass);
        intent.setFlags(flags);
        if(args != null) { intent.putExtra(EXTRA_ARGS, args); }
        activity.startActivity(intent);

    }

    public final void startActivity(@NonNull Class<? extends Activity> activityClass, Parcelable args) {
        Activity activity = getActivity();
        Intent intent = new Intent(activity, activityClass);
        if(args != null) { intent.putExtra(EXTRA_ARGS, args); }
        activity.startActivity(intent);
    }

    public Bundle getExtrasBundle(Intent intent) {
        return intent.hasExtra(EXTRA_ARGS) ? intent.getBundleExtra(EXTRA_ARGS) : new Bundle();
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        getActivity().startActivityForResult(intent, requestCode);
    }

    public void startActivityForResult(@NonNull Class<? extends Activity> activityClass, int requestCode, int flags) {
        Intent intent = new Intent(getActivity(), activityClass);
        intent.setFlags(flags);
        startActivityForResult(intent, requestCode);
    }
    public void replaceFragment(@IdRes int containerId, @NonNull Fragment fragment, Bundle args, AppCompatActivity activity) {
        if(args != null) { fragment.setArguments(args);}
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction().replace(containerId, fragment, null);
        fragmentTransaction.commit();
        fragmentManager.executePendingTransactions();
    }
}
