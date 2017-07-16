package org.owntracks.android.model;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import android.widget.TextView;


import com.google.android.gms.maps.model.LatLng;


import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.BuildConfig;
import org.owntracks.android.R;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageLocation;

import timber.log.Timber;

public class FusedContact extends BaseObservable {
    private final String id;
    private MessageLocation messageLocation;
    private MessageCard messageCard;
    private Integer imageProvider = 0;

    @Bindable
    public Integer getImageProvider() {
        return imageProvider;
    }


    @Bindable
    public void setImageProvider(Integer imageProvider) {
        this.imageProvider = imageProvider;
    }

    public FusedContact(@Nullable String id) {
        this.id = (id != null && !id.isEmpty()) ? id : "NOID";
    }

    public boolean setMessageLocation(MessageLocation messageLocation) {
        if(this.messageLocation != null && this.messageLocation.getTst() == messageLocation.getTst())
            return false;

        if(BuildConfig.DEBUG)
            Timber.v("update contact:%s, tst:%s", id, messageLocation.getTst());

        this.messageLocation = messageLocation;
        this.messageLocation.setContact(this); // Allows to update fusedLocation if geocoder of messageLocation changed
        notifyMessageLocationPropertyChanged();
        return true;
    }

    public void setMessageCard(MessageCard messageCard) {
        this.messageCard = messageCard;

        App.getContactImageProvider().invalidateCacheLevelCard(getId());
        notifyMessageCardPropertyChanged();
    }

    private void notifyMessageCardPropertyChanged() {
        this.notifyPropertyChanged(BR.fusedName);
        this.notifyPropertyChanged(BR.imageProvider);

        this.notifyPropertyChanged(BR.id);

    }

    public void notifyMessageLocationPropertyChanged() {
        this.notifyPropertyChanged(BR.fusedName);
        this.notifyPropertyChanged(BR.messageLocation);
        this.notifyPropertyChanged(BR.fusedLocationDate);
        this.notifyPropertyChanged(BR.fusedLocationAccuracy);

        this.notifyPropertyChanged(BR.trackerId);
        this.notifyPropertyChanged(BR.id);

    }


    @Bindable
    public MessageCard getMessageCard() {
        return messageCard;
    }

    @Bindable
    public MessageLocation getMessageLocation() {
        return messageLocation;
    }

    @Bindable
    public String getFusedName() {
        if(hasCard() && getMessageCard().hasName())
            return getMessageCard().getName();
        else
            return getTrackerId();
    }

    @BindingAdapter({"imageProvider", "contact"})
    public static void displayFaceInViewAsync(ImageView view, Integer imageProvider, FusedContact c) {
        App.getContactImageProvider().setImageViewAsync(view, c);

    }

    @BindingAdapter({"android:text", "messageLocation"})
    public static void displayFusedLocationInViewAsync(TextView view,  FusedContact c, MessageLocation m) {
        if(m != null)
            App.getGeocodingProvider().resolve(m, view);
        else
            view.setText(R.string.na);
    }

    @Bindable
    public long getFusedLocationDate() {
        return this.hasLocation() ? messageLocation.getTst() : 0;
    }

    @Bindable
    public String getFusedLocationAccuracy() {
        return Integer.toString(this.hasLocation() ? messageLocation.getAcc() : 0);
    }

    public boolean hasLocation() {
        return this.messageLocation != null;
    }

    public boolean hasCard() {
        return this.messageCard != null;
    }


    @Bindable
    @NonNull
    public String getTrackerId() {
        if(hasLocation() && getMessageLocation().hasTid())
            return getMessageLocation().getTid();
        else {
            String id = getId().replace("/","");
            if(id.length() > 2) {
                return id.substring(id.length() - 2);
            }
            else
                return id;
        }
    }


    @Bindable
    public @NonNull String getId() {
        return id;
    }

    public LatLng getLatLng() {
        return new LatLng(this.messageLocation.getLatitude(), this.messageLocation.getLongitude());
    }

    private boolean deleted;

    public boolean isDeleted() {
        return deleted;
    }

    public void  setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
