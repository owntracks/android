package org.owntracks.android.ui.map;

import android.support.annotation.NonNull;

import org.owntracks.android.model.FusedContact;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

import java.util.List;

public interface MapMvvm {

    interface View extends MvvmView {
        void setBottomSheetExpanded();
        void setBottomSheetCollapsed();
        void setBottomSheetHidden();

        void contactUpdate(FusedContact contact);
        void contactUpdateActive();

        void setModeContact(boolean center);
        void setModeDevice();
        void clearMarker();
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        FusedContact getContact();
        List<FusedContact> getContacts();
        long getContactsRevision();

        void onMarkerClick(@NonNull String contactId);
        void onMapClick();
        void onBottomSheetLongClick();
        void onBottomSheetClick();
        void onMenuCenterDeviceClicked();

        void restore(String contactId);
    }
}
