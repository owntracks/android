package org.owntracks.android.ui.mixins

import androidx.activity.result.ActivityResultCaller

interface ActivityResultCallerWithLocationPermissionCallback :
    ActivityResultCaller,
    LocationPermissionRequester.PermissionResultCallback
