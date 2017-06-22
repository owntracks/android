package org.owntracks.android.ui.welcome;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.databinding.Bindable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;
import org.owntracks.android.ui.map.MapActivity;
import org.owntracks.android.ui.welcome.finish.FinishFragment;
import org.owntracks.android.ui.welcome.finish.FinishFragmentViewModel;

import javax.inject.Inject;

import timber.log.Timber;


@PerActivity
public class WelcomeViewModel extends BaseViewModel<WelcomeMvvm.View> implements WelcomeMvvm.ViewModel<WelcomeMvvm.View> {
    private WelcomeFragmentMvvm.ViewModel fragmentViewModel;


    @Inject
    public WelcomeViewModel() {

    }
    public void attachView(@NonNull WelcomeMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
    }

    @Override
    public boolean isDoneEnabled() {
        if(fragmentViewModel != null) {
            Timber.v("class :%s", fragmentViewModel.getClass());
        }
        return fragmentViewModel != null && fragmentViewModel.getClass() == FinishFragmentViewModel.class;
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

    @SuppressWarnings("unchecked")
    @Override
    public WelcomeFragmentMvvm.ViewModel getFragmentViewModel() {
        return fragmentViewModel;
    }

    @Override
    @Bindable
    public void setFragmentViewModel(WelcomeFragmentMvvm.ViewModel fragmentViewModel) {
        this.fragmentViewModel = fragmentViewModel;
        notifyChange();
        notifyPropertyChanged(BR.vm);
        notifyPropertyChanged(BR.doneEnabled);
    }
}
