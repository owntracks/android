package org.owntracks.android.model;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.util.Log;
import android.widget.ImageView;


import com.google.android.gms.maps.model.LatLng;


import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.support.ContactImageProvider;


public class FusedContact extends BaseObservable {
    private static final int FACE_HEIGHT_SCALE = (int) convertDpToPixel(48);
    private static final String TAG = "FusedContact";

    private final String id;
    private MessageLocation messageLocation;
    private MessageCard messageCard;

    @Bindable
    private
    Integer imageProviderLevel = IMAGE_PROVIDER_LEVEL_TID;
    private static final Integer IMAGE_PROVIDER_LEVEL_TID =0;

    public Integer getImageProviderLevel() {
        return imageProviderLevel;
    }
    public void setImageProviderLevel(Integer newLevel) {
        imageProviderLevel = newLevel;
    }


    public FusedContact(String id) {
        Log.v("FusedContact", "new contact allocated for id: " + id);
        this.id = id;
    }


    public void setMessageLocation(MessageLocation messageLocation) {
        this.messageLocation = messageLocation;
        this.messageLocation.setContact(this); // Allows to update fusedLocation if geocoder of messageLocation changed
        notifyMessageLocationPropertyChanged();
    }

    public void setMessageCard(MessageCard messageCard) {
        this.messageCard = messageCard;
        ContactImageProvider.invalidateCacheLevelCard(getId());
        notifyMessageCardPropertyChanged();
    }

    public void notifyMessageCardPropertyChanged() {
        this.notifyPropertyChanged(BR.fusedName);
        this.notifyPropertyChanged(BR.imageProviderLevel);
        this.notifyPropertyChanged(BR.id);

    }

    public void notifyMessageLocationPropertyChanged() {
        Log.v(TAG, "notifyMessageLocationPropertyChanged");
        this.notifyPropertyChanged(BR.fusedName);
        this.notifyPropertyChanged(BR.fusedLocation);
        this.notifyPropertyChanged(BR.fusedLocationDate);
        this.notifyPropertyChanged(BR.fusedLocationAccuracy);

        this.notifyPropertyChanged(BR.trackerId);
        this.notifyPropertyChanged(BR.id);

    }


    @Bindable
    public MessageCard getMessageCard() {
        return messageCard;
    }

    public MessageLocation getMessageLocation() {
        return messageLocation;
    }

    @Bindable
    public String getFusedName() {
        if(hasCard() && messageCard.hasName())
            return messageCard.getName();

        if(hasLocation() && messageLocation.getTid() != null)
            return "Device-"+messageLocation.getTid();

        return this.id;
    }

    @BindingAdapter({"imageProviderLevel", "contact"})
    public static void displayFaceInViewAsync(ImageView view, Integer imageProviderLevel, FusedContact c) {
        ContactImageProvider.setImageViewAsync(view, c);

    }

    @Bindable
    public String getFusedLocation() {
        return this.hasLocation() ? this.messageLocation.getGeocoder() : null;
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
    public String getTrackerId() {
        return hasLocation() ? this.messageLocation.getTid() : getId().substring(getId().length()-2).replace("/","");
    }

    @Bindable
    public String getId() {
        return id;
    }

    private static float convertDpToPixel(float dp) {
        return dp * (App.getContext().getResources().getDisplayMetrics().densityDpi / 160f);
    }

    public LatLng getLatLng() {
        return new LatLng(this.messageLocation.getLatitude(), this.messageLocation.getLongitude());
    }



}
