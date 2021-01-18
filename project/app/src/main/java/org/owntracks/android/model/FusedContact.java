package org.owntracks.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.maps.model.LatLng;

import org.owntracks.android.BR;
import org.owntracks.android.geocoding.GeocoderProvider;
import org.owntracks.android.model.messages.MessageCard;
import org.owntracks.android.model.messages.MessageLocation;

import timber.log.Timber;

public class FusedContact extends BaseObservable implements Comparable<FusedContact> {
    private final String id;
    private GeocoderProvider geocoderProvider;
    private MutableLiveData<MessageLocation> messageLocation = new MutableLiveData<>();
    private MessageCard messageCard;
    private Integer imageProvider = 0;
    private long tst = 0;

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
        this.geocoderProvider = geocoderProvider;
    }

    public boolean setMessageLocation(MessageLocation messageLocation) {
        if (tst > messageLocation.getTimestamp())
            return false;

        Timber.v("update contact:%s, tst:%s", id, messageLocation.getTimestamp());

        messageLocation.setContact(this); // Allows to update fusedLocation if geocoder of messageLocation changed
        this.messageLocation.postValue(messageLocation);
        this.tst = messageLocation.getTimestamp();
        notifyMessageLocationPropertyChanged();
        return true;
    }

    public void setMessageCard(MessageCard messageCard) {
        this.messageCard = messageCard;
        notifyMessageCardPropertyChanged();
    }

    private void notifyMessageCardPropertyChanged() {
        this.notifyPropertyChanged(BR.fusedName);
        this.notifyPropertyChanged(BR.imageProvider);
        this.notifyPropertyChanged(BR.id);

    }

    public void notifyMessageLocationPropertyChanged() {
        if (this.messageLocation.getValue() != null) {
            Timber.d("Geocode location updated for %s: %s", this.id, this.messageLocation.getValue().getGeocode());
        }
        this.notifyPropertyChanged(BR.fusedName);
        this.notifyPropertyChanged(BR.messageLocation);
        this.notifyPropertyChanged(BR.geocodedLocation);
        this.notifyPropertyChanged(BR.fusedLocationAccuracy);
        this.notifyPropertyChanged(BR.tst);
        this.notifyPropertyChanged(BR.trackerId);
        this.notifyPropertyChanged(BR.id);
    }


    @Bindable
    public MessageCard getMessageCard() {
        return messageCard;
    }

    @Bindable
    public MutableLiveData<MessageLocation> getMessageLocation() {
        return messageLocation;
    }

    @Bindable
    public String getFusedName() {
        if (hasCard() && getMessageCard().hasName())
            return getMessageCard().getName();
        else
            return getTrackerId();
    }

    @Bindable
    public String getFusedLocationAccuracy() {
        return Integer.toString(this.hasLocation() ? messageLocation.getValue().getAccuracy() : 0);
    }

    @Bindable
    public String getGeocodedLocation() {
        return this.messageLocation.getValue().getGeocode();
    }

    public boolean hasLocation() {
        return this.messageLocation.getValue() != null;
    }

    public boolean hasCard() {
        return this.messageCard != null;
    }


    @Bindable
    @NonNull
    public String getTrackerId() {
        if (hasLocation() && getMessageLocation().getValue().hasTrackerId())
            return getMessageLocation().getValue().getTrackerId();
        else {
            String id = getId().replace("/", "");
            if (id.length() > 2) {
                return id.substring(id.length() - 2);
            } else
                return id;
        }
    }


    @Bindable
    public @NonNull
    String getId() {
        return id;
    }

    public LatLng getLatLng() {
        return new LatLng(this.messageLocation.getValue().getLatitude(), this.messageLocation.getValue().getLongitude());
    }

    private boolean deleted;

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted() {
        this.deleted = true;
    }

    @Bindable
    public long getTst() {
        return tst;
    }

    @Override
    public int compareTo(@NonNull FusedContact o) {
        return Long.compare(o.tst, this.tst);
    }
}
