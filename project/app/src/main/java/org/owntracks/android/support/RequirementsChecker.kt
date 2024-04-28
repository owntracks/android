package org.owntracks.android.support

interface RequirementsChecker {
  fun hasLocationPermissions(): Boolean

  fun isLocationServiceEnabled(): Boolean

  fun isPlayServicesCheckPassed(): Boolean

  fun hasNotificationPermissions(): Boolean
}
