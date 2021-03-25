package org.owntracks.android.support

interface RequirementsChecker {
    fun areRequirementsMet(): Boolean
    fun isPermissionCheckPassed():Boolean
    fun isPlayServicesCheckPassed():Boolean
}

