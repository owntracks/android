package org.owntracks.android.support

interface RequirementsChecker {
    fun areRequirementsMet(): Boolean
    fun isLocationPermissionCheckPassed(): Boolean
    fun isLocationServiceEnabled(): Boolean
    fun isPlayServicesCheckPassed(): Boolean
    fun isNotificationsEnabled(): Boolean
}
