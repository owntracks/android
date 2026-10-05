package org.owntracks.android.testutils.hilt

import android.app.Application
import android.app.job.JobScheduler
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.CustomTestApplication
import org.owntracks.android.BaseApp

@Suppress("unused")
class CustomTestRunner : AndroidJUnitRunner() {
  override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
    return super.newApplication(cl, TestApp_Application::class.java.name, context)
  }

  /*
  The orchestrator runs each test in its own process, but with clearPackageData off, WorkManager's
  persisted work - MQTT reconnects, the connection watchdog, location pings - carries over from one
  test's process into the next, where it runs as soon as the process starts. That interferes with
  whatever the next test is doing, and in a test without Hilt set up, a worker binding
  BackgroundService crashes it outright. Some of that work is enqueued in reaction to a test's own
  teardown (the test broker going away), so it can't reliably be cancelled from the test itself.

  So start every process without any: WorkManager initialises on demand (its startup initializer is
  removed in the manifest), so nothing has opened its database yet at this point.
   */
  override fun callApplicationOnCreate(app: Application) {
    app.getSystemService(JobScheduler::class.java)?.cancelAll()
    // WorkManager keeps its database in noBackupFilesDir; the databases dir only ever held an older
    // copy it migrates from.
    app.deleteDatabase(WORK_MANAGER_DATABASE_NAME)
    listOf("", "-shm", "-wal", "-journal").forEach {
      app.noBackupFilesDir.resolve(WORK_MANAGER_DATABASE_NAME + it).delete()
    }
    super.callApplicationOnCreate(app)
  }

  companion object {
    private const val WORK_MANAGER_DATABASE_NAME = "androidx.work.workdb"
  }
}

@CustomTestApplication(BaseApp::class) interface TestApp
