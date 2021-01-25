package org.owntracks.android.ui.welcome;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;
import org.owntracks.android.ui.map.MapActivity;

import javax.inject.Inject;

import timber.log.Timber;


@PerActivity
public class WelcomeViewModel extends BaseViewModel<WelcomeMvvm.View> {

    private final Preferences preferences;
    private boolean doneEnabled;
    private boolean nextEnabled;

    @Inject
    public WelcomeViewModel(Preferences preferences) {
        this.preferences = preferences;
    }

    public void attachView(@Nullable Bundle savedInstanceState, @NonNull WelcomeMvvm.View view) {
        super.attachView(savedInstanceState, view);
    }

    public void onNextClicked() {
        Timber.v("onNextClicked next:%s, done:%s", nextEnabled, doneEnabled);
        getView().showNextFragment();

    }

    public void onDoneClicked() {
        Timber.v("onDoneClicked next:%s, done:%s", nextEnabled, doneEnabled);
        preferences.setSetupCompleted();
        navigator.startActivity(MapActivity.class, null, Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public void setNextEnabled(boolean enabled) {
        this.nextEnabled = enabled;
        notifyChange();
    }

    public boolean isNextEnabled() {
        return nextEnabled;
    }

    public boolean isDoneEnabled() {
        return doneEnabled;
    }

    public void setDoneEnabled(boolean enabled) {
        this.doneEnabled = enabled;
        notifyChange();
    }

}
