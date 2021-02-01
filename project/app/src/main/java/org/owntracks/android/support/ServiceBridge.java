package org.owntracks.android.support;

import javax.inject.Singleton;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import androidx.annotation.NonNull;

import timber.log.Timber;


@Singleton
public class ServiceBridge {
    private WeakReference<ServiceBridgeInterface> serviceWeakReference;

    public interface ServiceBridgeInterface {
        void requestOnDemandLocationUpdate();
    }

    @Inject
    ServiceBridge() {
        this.serviceWeakReference = new WeakReference<>(null);
    }

    public void bind(@NonNull ServiceBridgeInterface service) {
        this.serviceWeakReference = new WeakReference<>(service);
    }

    public void requestOnDemandLocationFix() {
        if(serviceWeakReference == null) {
            Timber.e("missing service reference");
            return;
        }

        ServiceBridgeInterface service = serviceWeakReference.get();
        if(service != null) {
            service.requestOnDemandLocationUpdate();
        }
    }


}

