package org.owntracks.android.ui.map;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import org.owntracks.android.R;
import org.owntracks.android.data.model.Contact;
import org.owntracks.android.databinding.ActivityMapBinding;
import org.owntracks.android.databinding.UiActivityMapBinding;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.ui.base.BaseActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import timber.log.Timber;

/* Copyright 2016 Patrick Löwenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
public class MapActivity extends BaseActivity<UiActivityMapBinding, MapMvvm.ViewModel> implements MapMvvm.View, OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private Menu menu = null;
    private GoogleMap mMap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Timber.v("onCreate");
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        Timber.v("setAndBindContentView");
        setAndBindContentView(R.layout.ui_activity_map, savedInstanceState);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(getTitle());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.map.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        Timber.v("onMapReady");
        this.mMap = googleMap;
        this.mMap.setIndoorEnabled(false);
        this.mMap.setLocationSource(mMapLocationSource);
        //this.map.setMyLocationEnabled(true);
        this.mMap.getUiSettings().setMyLocationButtonEnabled(false);
        this.mMap.setOnMapClickListener(this);
        this.mMap.setOnMarkerClickListener(this);
        this.mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });
        viewModel.onMapReady();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        viewModel.onMapClick();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if(marker.getTag() != null)
            viewModel.onMapMarkerClick(Contact.class.cast(marker.getTag()));
        return true;
    }

    LocationSource mMapLocationSource = new LocationSource() {
        LocationSource.OnLocationChangedListener mListener;
        Location mLocation;

        @Override
        public void activate(OnLocationChangedListener onLocationChangedListener) {
            mListener = onLocationChangedListener;
            if(mLocation != null)
                this.mListener.onLocationChanged(mLocation);
            EventBus.getDefault().register(this);
        }

        @Override
        public void deactivate() {
            mListener = null;
            EventBus.getDefault().unregister(this);
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        public void update(@NonNull Location l) {
            this.mLocation = l;
            if(mListener != null)
                this.mListener.onLocationChanged(l);
        }
    };

    @Override
    public void updateMarker(List<FusedContact> contacts) {
        Timber.v("updating list");
        for (FusedContact c : contacts) {
           updateMarker(c);
        }
    }

    @Override
    public void updateMarker(FusedContact contact) {
        Timber.v("updating single id:%s", contact.getId());

    }

    @Override
    public void removeMarker(String key) {
        Timber.v("removing single id:%s", key);

    }
}
