package org.owntracks.android.support

interface RequirementsChecker {
    fun isLocationPermissionCheckPassed(): Boolean
    fun isLocationServiceEnabled(): Boolean
    fun isPlayServicesCheckPassed(): Boolean
    fun isNotificationsEnabled(): Boolean
}
