package org.owntracks.android.ui.mixins

interface PermissionResultCallback {
  fun permissionGranted(permission: String, code: Int)

  fun permissionDenied(permission: String, code: Int)
}
