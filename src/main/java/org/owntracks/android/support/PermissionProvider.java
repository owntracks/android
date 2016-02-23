package org.owntracks.android.support;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import org.owntracks.android.App;
import org.owntracks.android.activities.ActivityPermission;
import org.owntracks.android.services.ProxyableService;

import java.util.HashSet;
import java.util.List;

public class PermissionProvider {
    private static HashSet<String> missingPermissions = new HashSet<>();
    private static HashSet<String> requiredPermissions = new HashSet<>();


    public static boolean hasLocationPermission(Context c) {
        return hasPermission(Manifest.permission_group.LOCATION);
    }
    public static boolean hasStoragePermission(Context c) {
        return hasPermission(Manifest.permission_group.STORAGE);
    }

    public static boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(App.getContext(), permission) == PackageManager.PERMISSION_GRANTED;
    }



    public static void registerRequiredServicePermission(String serviceId, List<String> requiredServicePermissions) {
        if(requiredServicePermissions == null)
            return;

        for (String permission : requiredServicePermissions) {
            if(requiredPermissions.add(permission) && !hasPermission(permission)) {
                onRequiredServicePermissionDenied(serviceId, permission);
            }
        }
    }

    private static void onRequiredServicePermissionDenied(String serviceId, String permission) {

    }
}
