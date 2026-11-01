package org.owntracks.android.ui.status.logs

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.owntracks.android.logging.TimberInMemoryLogTree
import org.owntracks.android.preferences.InMemoryPreferencesStore
import org.owntracks.android.preferences.Preferences
import timber.log.Timber

internal class LogViewerViewModelTest {
  private val preferences = Preferences(InMemoryPreferencesStore(), mock())

  @Before
  fun plantTree() {
    Timber.plant(mock<TimberInMemoryLogTree>())
  }

  @After
  fun uprootTree() {
    Timber.uprootAll()
  }

  @Test
  fun `given debug logging is off, when creating the view model, then debug is not a shown level`() {
    preferences.debugLog = false
    assertEquals(
        setOf(LogLevel.ERROR, LogLevel.WARNING, LogLevel.INFO),
        LogViewerViewModel(preferences).filter.levels,
    )
  }

  @Test
  fun `given debug logging is on, when creating the view model, then every level is shown`() {
    preferences.debugLog = true
    assertEquals(LogLevel.entries.toSet(), LogViewerViewModel(preferences).filter.levels)
  }

  @Test
  fun `given the debug level, when changing it, then the debug preference follows`() {
    val viewModel = LogViewerViewModel(preferences)
    viewModel.setLevelEnabled(LogLevel.DEBUG, true)
    assertTrue(preferences.debugLog)
    viewModel.setLevelEnabled(LogLevel.DEBUG, false)
    assertFalse(preferences.debugLog)
  }

  @Test
  fun `given another level, when disabling it, then the debug preference is unchanged`() {
    preferences.debugLog = true
    val viewModel = LogViewerViewModel(preferences)
    viewModel.setLevelEnabled(LogLevel.INFO, false)
    assertTrue(preferences.debugLog)
    assertFalse(LogLevel.INFO in viewModel.filter.levels)
  }

  @Test
  fun `given a filter, when setting the query, then the levels are kept`() {
    val viewModel = LogViewerViewModel(preferences)
    val levels = viewModel.filter.levels
    viewModel.setQuery("mqtt")
    assertEquals(LogFilter(levels, "mqtt"), viewModel.filter)
  }
}
