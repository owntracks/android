package org.owntracks.android.ui.base.navigator;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;


public interface Navigator {
    String EXTRA_ARGS = "_args";

    void startActivity(@NonNull Intent intent);
    void startActivity(@NonNull String action);
    void startActivity(@NonNull String action, @NonNull Uri uri);
    void startActivity(@NonNull Class<? extends Activity> activityClass);
    void startActivity(@NonNull Class<? extends Activity> activityClass, Bundle args);
    void startActivity(@NonNull Class<? extends Activity> activityClass, Bundle args, int flags);
    void startActivity(@NonNull Class<? extends Activity> activityClass, Parcelable args);
    void startActivityForResult(Intent intent, int requestCode);
    void startActivityForResult(@NonNull Class<? extends Activity> activityClass, int requestCode, int flags);

    void replaceFragment(@IdRes int containerId, @NonNull android.support.v4.app.Fragment fragment, Bundle args);
    void replaceFragment(@IdRes int containerId, @NonNull android.app.Fragment fragment, Bundle args);

    Bundle getExtrasBundle(Intent intent);

}
