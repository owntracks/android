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
    private final String topic;
    private MessageLocation messageLocation;
    private MessageCard messageCard;

    @Bindable
    private
    Integer imageProviderLevel = IMAGE_PROVIDER_LEVEL_TID;
    private static final Integer IMAGE_PROVIDER_LEVEL_TID =0;
    public static final Integer IMAGE_PROVIDER_LEVEL_CARD =1;
    public static final Integer IMAGE_PROVIDER_LEVEL_LINK =2;

    public Integer getImageProviderLevel() {
        return imageProviderLevel;
    }
    public void setImageProviderLevel(Integer newLevel) {
        imageProviderLevel = newLevel;
    }


    public FusedContact(String topic) {
        Log.v("FusedContact", "new contact allocated for topic: " + topic);
        this.topic = topic;

    }


    public void setMessageLocation(MessageLocation messageLocation) {
        this.messageLocation = messageLocation;
        this.messageLocation.setContact(this);
    }

    public void setMessageCard(MessageCard messageCard) {
        this.messageCard = messageCard;
        this.messageCard.setContact(this);
        this.notifyPropertyChanged(BR.fusedName);
        this.notifyPropertyChanged(BR.imageProviderLevel);
        ContactImageProvider.invalidateCacheLevelCard(getTopic());
    }

    public void notifyMessageCardPropertyChanged() {
        notifyPropertyChanged(BR.fusedName);

    }

    public void notifyMessageLocationPropertyChanged() {
        notifyPropertyChanged(BR.fusedName);
        notifyPropertyChanged(BR.fusedLocation);
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
            return messageCard.getName() + (hasLocation() ? (" (" + messageLocation.getTid() +")") : "" );

        if(hasLocation() && messageLocation.getTid() != null)
            return "Device-"+messageLocation.getTid();

        return this.topic;
    }

    @BindingAdapter({"imageProviderLevel", "contact"})
    public static void displayFaceInViewAsync(ImageView view, Integer imageProviderLevel, FusedContact c) {
        ContactImageProvider.setImageViewAsync(view, c);

    }

    @Bindable
    public String getFusedLocation() {
        return this.hasLocation() ? this.messageLocation.getGeocoder() : null;
    }

    public boolean hasLocation() {
        return this.messageLocation != null;
    }

    public boolean hasCard() {
        return this.messageCard != null;
    }


    public String getTrackerId() {
        return hasLocation() ? this.messageLocation.getTid() : getTopic().substring(getTopic().length()-2).replace("/","");
    }

    @Bindable
    public String getTopic() {
        return topic;
    }

    private static float convertDpToPixel(float dp) {
        return dp * (App.getContext().getResources().getDisplayMetrics().densityDpi / 160f);
    }

    public LatLng getLatLng() {
        return new LatLng(this.messageLocation.getLatitude(), this.messageLocation.getLongitude());
    }
}
