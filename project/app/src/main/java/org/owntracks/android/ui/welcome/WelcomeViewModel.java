package org.owntracks.android.ui.welcome;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;
import org.owntracks.android.ui.map.MapActivity;

import javax.inject.Inject;


@PerActivity
public class WelcomeViewModel extends BaseViewModel<WelcomeMvvm.View> implements WelcomeMvvm.ViewModel<WelcomeMvvm.View> {

    private boolean doneEnabled;
    private boolean nextEnabled;

    @Inject
    public WelcomeViewModel() {

    }
    public void attachView(@NonNull WelcomeMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
    }

    @Override
    public void onAdapterPageSelected(int position) {
        getView().setPagerIndicator(position);
    }

    @Override
    public void onNextClicked() {
        getView().showNextFragment();
    }

    @Override
    public void onDoneClicked() {
        navigator.get().startActivity(MapActivity.class);
    }

    @Override
    public void setNextEnabled(boolean enabled) {
        this.nextEnabled = enabled;
        notifyChange();
    }

    @Override
    public boolean isNextEnabled() {
        return nextEnabled;
    }

    @Override
    public boolean isDoneEnabled() {
        return doneEnabled;
    }

    @Override
    public void setDoneEnabled(boolean enabled) {
        this.doneEnabled = enabled;
        notifyChange();
    }

}
