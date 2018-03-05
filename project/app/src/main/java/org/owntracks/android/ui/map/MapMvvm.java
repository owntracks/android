package org.owntracks.android.ui.map;

import android.databinding.Bindable;
import android.support.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.model.LatLng;

import org.owntracks.android.model.FusedContact;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

import java.util.Collection;

public interface MapMvvm {

    interface View extends MvvmView {
        void setBottomSheetExpanded();
        void setBottomSheetCollapsed();
        void setBottomSheetHidden();

        void updateContact(FusedContact contact);
        void removeContact(FusedContact c);

        void clearMarkers();
        void updateCamera(@NonNull LatLng latLng);

    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V>  {
        LatLng getCurrentLocation();


        @Bindable
        FusedContact getActiveContact();
        Collection<FusedContact> getContacts();

        void onBottomSheetLongClick();
        void onBottomSheetClick();
        void onMenuCenterDeviceClicked();
        void onClearContactClicked();

        void restore(String contactId);
        boolean hasLocation();

        LocationSource getMapLocationSource();
        GoogleMap.OnMapClickListener getOnMapClickListener();
        GoogleMap.OnMarkerClickListener getOnMarkerClickListener();
        void onMapReady();
    }
}
