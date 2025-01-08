package org.owntracks.android

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.CustomTestApplication

@Suppress("unused")
class CustomTestRunner : AndroidJUnitRunner() {
  override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
    return super.newApplication(cl, TestApp_Application::class.java.name, context)
  }
}

@CustomTestApplication(BaseApp::class) interface TestApp
