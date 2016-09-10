package org.owntracks.android.model;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;


import com.google.android.gms.maps.model.LatLng;


import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.R;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.GeocodingProvider;


public class FusedContact extends BaseObservable {
    private static final int FACE_HEIGHT_SCALE = (int) convertDpToPixel(48);
    private static final String TAG = "FusedContact";

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

    public FusedContact(@NonNull String id) {
        Log.v(TAG, "new contact allocated for id: " + id);
        this.id = id;
    }

    public boolean setMessageLocation(MessageLocation messageLocation) {
        if(this.messageLocation != null && this.messageLocation.getTst() == messageLocation.getTst())
            return false;



        this.messageLocation = messageLocation;
        this.messageLocation.setContact(this); // Allows to update fusedLocation if geocoder of messageLocation changed
        notifyMessageLocationPropertyChanged();
        return true;
    }

    public void setMessageCard(MessageCard messageCard) {
        this.messageCard = messageCard;

        ContactImageProvider.invalidateCacheLevelCard(getId());
        notifyMessageCardPropertyChanged();
    }

    public void notifyMessageCardPropertyChanged() {
        this.notifyPropertyChanged(BR.fusedName);
        this.notifyPropertyChanged(BR.imageProvider);

        this.notifyPropertyChanged(BR.id);

    }

    public void notifyMessageLocationPropertyChanged() {
        this.notifyPropertyChanged(BR.fusedName);
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
        if(hasCard() && messageCard.hasName())
            return messageCard.getName();

        if(hasLocation() && messageLocation.getTid() != null)
            return "Device-"+messageLocation.getTid();

        return this.id;
    }

    @BindingAdapter({"imageProvider", "contact"})
    public static void displayFaceInViewAsync(ImageView view, Integer imageProvider, FusedContact c) {
        ContactImageProvider.setImageViewAsync(view, c);

    }

    @BindingAdapter({"android:text", "messageLocation"})
    public static void displayFusedLocationInViewAsync(TextView view,  FusedContact c, MessageLocation m) {
        if(m != null)
            GeocodingProvider.resolve(m, view);
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
    public String getTrackerId() {
        return hasLocation() ? this.messageLocation.getTid() : getId().substring(getId().length()-2).replace("/","");
    }

    @Bindable
    public @NonNull String getId() {
        return id;
    }

    private static float convertDpToPixel(float dp) {
        return dp * (App.getContext().getResources().getDisplayMetrics().densityDpi / 160f);
    }

    public LatLng getLatLng() {
        return new LatLng(this.messageLocation.getLatitude(), this.messageLocation.getLongitude());
    }



}
