package org.owntracks.android.ui.preferences.load

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.data.waypoints.InMemoryWaypointsRepo
import org.owntracks.android.preferences.InMemoryPreferencesStore
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.PreferencesStore
import org.owntracks.android.support.Parser
import org.owntracks.android.support.SimpleIdlingResource

@OptIn(ExperimentalCoroutinesApi::class)
class LoadViewModelTest {
  private lateinit var mockContext: Context
  private lateinit var preferencesStore: PreferencesStore
  private val mockIdlingResource = SimpleIdlingResource("mock", true)

  @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

  @Before
  fun createMocks() {
    mockContext = mock { on { packageName } doReturn javaClass.canonicalName }
    preferencesStore = InMemoryPreferencesStore()
  }

  @Test
  fun `Given an invalid RUL, when loading it into the LoadViewModel, then the error is correctly set`() =
      runTest {
        val parser = Parser(null)
        val preferences = Preferences(preferencesStore, mockIdlingResource)
        val vm =
            LoadViewModel(preferences, parser, InMemoryWaypointsRepo(), UnconfinedTestDispatcher())
        vm.extractPreferencesFromUri("owntracks:///config?{}")
        assertEquals(ImportStatus.FAILED, vm.configurationImportStatus.value)
        assertEquals(
            "Illegal character in query at index 20: owntracks:///config?{}", vm.importError.value)
      }

  @Test
  fun `Given an inline OwnTracks config URL with invalid JSON, when loading it into the LoadViewModel, then the error is correctly set`() =
      runTest {
        val parser = Parser(null)
        val preferences = Preferences(preferencesStore, mockIdlingResource)
        val vm =
            LoadViewModel(preferences, parser, InMemoryWaypointsRepo(), UnconfinedTestDispatcher())
        vm.extractPreferencesFromUri("owntracks:///config?inline=e30=")
        assertEquals(ImportStatus.FAILED, vm.configurationImportStatus.value)
        assertEquals("Message is not a valid configuration message", vm.importError.value)
      }

  @Test
  fun `Given an inline OwnTracks config URL with a simple MessageConfiguration JSON, when loading it into the LoadViewModel, then the correct config is displayed`() =
      runTest {
        val parser = Parser(null)
        val preferences = Preferences(preferencesStore, mockIdlingResource)
        val vm =
            LoadViewModel(preferences, parser, InMemoryWaypointsRepo(), UnconfinedTestDispatcher())
        vm.extractPreferencesFromUri(
            "owntracks:///config?inline=eyJfdHlwZSI6ImNvbmZpZ3VyYXRpb24ifQ")
        advanceUntilIdle()
        assertEquals(ImportStatus.SUCCESS, vm.configurationImportStatus.value)
        val json = ObjectMapper().readTree(vm.displayedConfiguration.value)
        assertTrue(json.isObject)
        assertTrue(json.has("_type"))
        assertEquals("configuration", json.get("_type").asText())
        assertTrue(json.has("waypoints"))
        assertTrue(json.get("waypoints").isArray)
        assertTrue(json.get("waypoints").isEmpty)
      }

  @Test
  fun `Given an inline OwnTracks config URL with a simple MessageWaypoints JSON, when loading it into the LoadViewModel, then the correct waypoints are displayed`() =
      runTest {
        val parser = Parser(null)
        val preferences = Preferences(preferencesStore, mockIdlingResource)
        val vm =
            LoadViewModel(preferences, parser, InMemoryWaypointsRepo(), UnconfinedTestDispatcher())
        vm.extractPreferencesFromUri(
            "owntracks:///config?inline=eyJfdHlwZSI6IndheXBvaW50cyIsIndheXBvaW50cyI6W3siX3R5cGUiOiJ3YXlwb2ludCIsImRlc2MiOiJUZXN0IFdheXBvaW50IiwibGF0Ijo1MSwibG9uIjowLCJyYWQiOjQ1MCwidHN0IjoxNTk4NDUxMzcyfV19")
        val displayedConfig = vm.displayedConfiguration.value
        val json = ObjectMapper().readTree(displayedConfig)
        assertTrue(json.isObject)
        assertTrue(json.has("_type"))
        assertEquals("waypoints", json.get("_type").asText())
        assertTrue(json.has("_id"))
        assertTrue(json.get("_id").isTextual)
        assertTrue(json.has("waypoints"))
        assertTrue(json.get("waypoints").isArray)
        assertEquals(1, json.get("waypoints").size())
        assertTrue(json.get("waypoints").get(0).isObject)
        assertTrue(json.get("waypoints").get(0).has("_type"))
        assertEquals("waypoint", json.get("waypoints").get(0).get("_type").asText())
        assertTrue(json.get("waypoints").get(0).has("desc"))
        assertEquals("Test Waypoint", json.get("waypoints").get(0).get("desc").asText())
        assertTrue(json.get("waypoints").get(0).has("lat"))
        assertEquals(51.0, json.get("waypoints").get(0).get("lat").asDouble(), 0.000001)
        assertTrue(json.get("waypoints").get(0).has("lon"))
        assertEquals(0.0, json.get("waypoints").get(0).get("lon").asDouble(), 0.000001)
        assertTrue(json.get("waypoints").get(0).has("rad"))
        assertEquals(450, json.get("waypoints").get(0).get("rad").asInt())
        assertTrue(json.get("waypoints").get(0).has("tst"))
        assertEquals(1598451372, json.get("waypoints").get(0).get("tst").asLong())
        assertEquals(ImportStatus.SUCCESS, vm.configurationImportStatus.value)
      }

  @Test
  fun `Given a configuration with waypoints, when loading and then saving into the LoadViewModel, then the preferences and waypointsrepo are updated`() =
      runTest {
        val parser = Parser(null)
        val preferences = Preferences(preferencesStore, mockIdlingResource)
        val waypointsRepo = InMemoryWaypointsRepo()
        val vm = LoadViewModel(preferences, parser, waypointsRepo, UnconfinedTestDispatcher())
        val config =
            """
            {
              "_type":"configuration",
              "waypoints":[
                {
                  "_type":"waypoint",
                  "desc":"Test Waypoint",
                  "lat":51.0,
                  "lon":0.0,
                  "rad":450,
                  "tst":1598451372
                }
              ],
              "clientId": "testClientId"
            }
            """
                .trimIndent()
        vm.extractPreferences(config.toByteArray())
        vm.saveConfiguration()
        advanceUntilIdle()
        assertEquals(ImportStatus.SAVED, vm.configurationImportStatus.value)
        assertEquals(1, waypointsRepo.all.size)
        assertEquals("testClientId", preferences.clientId)
      }

  @Test
  fun `Given a configuration with an invalid key, when loading and then saving into the LoadViewModel, then no error is raised`() =
      runTest {
        val parser = Parser(null)
        val preferences = Preferences(preferencesStore, mockIdlingResource)
        val waypointsRepo = InMemoryWaypointsRepo()
        val vm = LoadViewModel(preferences, parser, waypointsRepo, UnconfinedTestDispatcher())
        val config =
            """
            {
              "_type":"configuration",
              "mode": "http"
            }
            """
                .trimIndent()
        vm.extractPreferences(config.toByteArray())
        vm.saveConfiguration()
        advanceUntilIdle()
        assertEquals(ImportStatus.SAVED, vm.configurationImportStatus.value)
      }

  @Test
  fun `Given a configuration with a tid parameter set, when loading and then saving into the LoadViewModel, then the preferences tid `() =
      runTest {
        val parser = Parser(null)
        val preferences = Preferences(preferencesStore, mockIdlingResource)
        val waypointsRepo = InMemoryWaypointsRepo()
        val vm = LoadViewModel(preferences, parser, waypointsRepo, UnconfinedTestDispatcher())
        val config =
            """
            {
              "_type":"configuration",
              "waypoints":[ ],
              "clientId": "testClientId",
              "tid": "testTid"
            }
            """
                .trimIndent()
        vm.extractPreferences(config.toByteArray())
        vm.saveConfiguration()
        advanceUntilIdle()
        assertEquals(ImportStatus.SAVED, vm.configurationImportStatus.value)
        assertEquals("te", preferences.tid.toString())
      }
}
