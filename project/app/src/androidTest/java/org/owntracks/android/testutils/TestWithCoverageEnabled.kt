package org.owntracks.android.testutils

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule

/**
 * Coverage data on espresso tests requires that we explicitly grant
 * [Manifest.permission.WRITE_EXTERNAL_STORAGE] so that the coverage data can actually be written.
 */
abstract class TestWithCoverageEnabled {
  @get:Rule
  var mRuntimePermissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)
}
