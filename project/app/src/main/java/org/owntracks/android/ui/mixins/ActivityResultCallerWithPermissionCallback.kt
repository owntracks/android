package org.owntracks.android.ui.mixins

import androidx.activity.result.ActivityResultCaller

interface ActivityResultCallerWithPermissionCallback :
    ActivityResultCaller, PermissionResultCallback
