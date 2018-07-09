package org.owntracks.android.ui.regions;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.db.Dao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;

@PerActivity

public class RegionsViewModel extends BaseViewModel<RegionsMvvm.View> implements RegionsMvvm.ViewModel<RegionsMvvm.View> {
    @Inject
    Dao dao;

    @Inject
    public RegionsViewModel() {

    }

    public void attachView(@NonNull RegionsMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
    }
}
