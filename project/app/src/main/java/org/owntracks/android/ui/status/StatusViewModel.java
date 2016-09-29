package org.owntracks.android.ui.status;

import android.content.Context;
import android.databinding.ObservableList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;
import org.owntracks.android.ui.contacts.ContactsMvvm;
import org.owntracks.android.ui.map.MapActivity;

import javax.inject.Inject;


@PerActivity
public class StatusViewModel extends BaseViewModel<StatusMvvm.View> implements StatusMvvm.ViewModel<StatusMvvm.View> {

    @Inject
    public StatusViewModel(@AppContext Context context) {

    }
    public void attachView(@NonNull StatusMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
    }

    @Override
    public String getEndpointState() {
        return null;
    }

    @Override
    public int getEndpointQueue() {
        return 0;
    }

    @Override
    public boolean getPermissionLocation() {
        return false;
    }

    @Override
    public long getLocationServiceUpdateDate() {
        return 0;
    }
}
