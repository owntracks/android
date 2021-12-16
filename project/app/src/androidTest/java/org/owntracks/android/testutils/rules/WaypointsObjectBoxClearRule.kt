package org.owntracks.android.testutils.rules

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.owntracks.android.App

class WaypointsObjectBoxClearRule : TestRule {
    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                base!!.evaluate()
                (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as App).resetWaypointsRepo()
            }
        }
    }
}