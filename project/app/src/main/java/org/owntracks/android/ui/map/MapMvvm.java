package org.owntracks.android.ui.map;

import android.arch.lifecycle.LiveData;
import android.databinding.Bindable;

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

        void updateMarker(FusedContact contact);
        void removeMarker(FusedContact c);
        void clearMarkers();
        void enableLocationMenus();
        void updateMonitoringModeMenu();
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
        void onMapReady();

        LocationSource getMapLocationSource();
        GoogleMap.OnMapClickListener getOnMapClickListener();
        GoogleMap.OnMarkerClickListener getOnMarkerClickListener();

        LiveData<FusedContact> getContact();
        LiveData<Boolean> getBottomSheetHidden();
        LiveData<LatLng> getCenter();

        void sendLocation();
    }
}
