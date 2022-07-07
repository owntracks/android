package org.owntracks.android.testutils

import java.lang.annotation.Inherited

@Retention(AnnotationRetention.RUNTIME)
@Inherited
/**
 * Useful for isolating a subset of the espresso tests to run as part of a PR, rather than the full suite
 */
annotation class SmokeTest
