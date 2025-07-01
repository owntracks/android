package org.owntracks.android.ui.preferences.load

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.data.waypoints.InMemoryWaypointsRepo
import org.owntracks.android.model.Parser
import org.owntracks.android.preferences.InMemoryPreferencesStore
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.PreferencesStore
import org.owntracks.android.test.SimpleIdlingResource

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
  fun `Given an invalid URL, when loading it into the LoadViewModel, then the error is correctly set`() =
      runTest {
        val parser = Parser(null)
        val preferences = Preferences(preferencesStore, mockIdlingResource)
        val vm =
            LoadViewModel(
                preferences,
                parser,
                InMemoryWaypointsRepo(this, mockContext, StandardTestDispatcher()),
                UnconfinedTestDispatcher(),
                SimpleIdlingResource("", true))
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
            LoadViewModel(
                preferences,
                parser,
                InMemoryWaypointsRepo(this, mockContext, StandardTestDispatcher()),
                UnconfinedTestDispatcher(),
                SimpleIdlingResource("", true))
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
            LoadViewModel(
                preferences,
                parser,
                InMemoryWaypointsRepo(this, mockContext, StandardTestDispatcher()),
                UnconfinedTestDispatcher(),
                SimpleIdlingResource("", true))
        vm.extractPreferencesFromUri(
            "owntracks:///config?inline=eyJfdHlwZSI6ImNvbmZpZ3VyYXRpb24ifQ==")
        advanceUntilIdle()
        assertEquals(ImportStatus.SUCCESS, vm.configurationImportStatus.value)
        val json = Json.parseToJsonElement(vm.displayedConfiguration.value!!).jsonObject
        assertTrue(json.isNotEmpty())
        assertTrue(json.containsKey("_type"))
        assertEquals("configuration", json["_type"]?.jsonPrimitive?.content)
        assertTrue(json.containsKey("waypoints"))
        assertTrue(json["waypoints"]?.jsonArray?.isEmpty() == true)
      }

  @Test
  fun `Given an inline OwnTracks config URL with a simple MessageWaypoints JSON, when loading it into the LoadViewModel, then the correct waypoints are displayed`() =
      runTest {
        val parser = Parser(null)
        val preferences = Preferences(preferencesStore, mockIdlingResource)
        val vm =
            LoadViewModel(
                preferences,
                parser,
                InMemoryWaypointsRepo(this, mockContext, StandardTestDispatcher()),
                UnconfinedTestDispatcher(),
                SimpleIdlingResource("", true))
        vm.extractPreferencesFromUri(
            "owntracks:///config?inline=eyJfdHlwZSI6IndheXBvaW50cyIsIndheXBvaW50cyI6W3siX3R5cGUiOiJ3YXlwb2ludCIsImRlc2MiOiJUZXN0IFdheXBvaW50IiwibGF0Ijo1MSwibG9uIjowLCJyYWQiOjQ1MCwidHN0IjoxNTk4NDUxMzcyfV19")
        val displayedConfig = vm.displayedConfiguration.value
        val json = Json.parseToJsonElement(displayedConfig!!).jsonObject
        assertTrue(json.isNotEmpty())
        assertTrue(json.containsKey("_type"))
        assertEquals("waypoints", json["_type"]?.jsonPrimitive?.content)
        assertTrue(json.containsKey("_id"))
        assertTrue(json["_id"]?.jsonPrimitive?.isString == true)
        assertTrue(json.containsKey("waypoints"))
        assertTrue(json["waypoints"]?.jsonArray?.isNotEmpty() == true)
        assertEquals(1, json["waypoints"]?.jsonArray?.size)
        assertTrue(json["waypoints"]?.jsonArray?.get(0)?.jsonObject?.isNotEmpty() == true)
        assertTrue(json["waypoints"]?.jsonArray?.get(0)?.jsonObject?.containsKey("_type") == true)
        assertEquals(
            "waypoint",
            json["waypoints"]?.jsonArray?.get(0)?.jsonObject?.get("_type")?.jsonPrimitive?.content)
        assertTrue(json["waypoints"]?.jsonArray?.get(0)?.jsonObject?.containsKey("desc") == true)
        assertEquals(
            "Test Waypoint",
            json["waypoints"]?.jsonArray?.get(0)?.jsonObject?.get("desc")?.jsonPrimitive?.content)
        assertTrue(json["waypoints"]?.jsonArray?.get(0)?.jsonObject?.containsKey("lat") == true)
        assertEquals(
            51.0,
            json["waypoints"]?.jsonArray?.get(0)?.jsonObject?.get("lat")?.jsonPrimitive?.double,
            0.000001)
        assertTrue(json["waypoints"]?.jsonArray?.get(0)?.jsonObject?.containsKey("lon") == true)
        assertEquals(
            0.0,
            json["waypoints"]?.jsonArray?.get(0)?.jsonObject?.get("lon")?.jsonPrimitive?.double,
            0.000001)
        assertTrue(json["waypoints"]?.jsonArray?.get(0)?.jsonObject?.containsKey("rad") == true)
        assertEquals(
            450, json["waypoints"]?.jsonArray?.get(0)?.jsonObject?.get("rad")?.jsonPrimitive?.int)
        assertTrue(json["waypoints"]?.jsonArray?.get(0)?.jsonObject?.containsKey("tst") == true)
        assertEquals(
            1598451372,
            json["waypoints"]?.jsonArray?.get(0)?.jsonObject?.get("tst")?.jsonPrimitive?.long)
        assertEquals(ImportStatus.SUCCESS, vm.configurationImportStatus.value)
      }

  @Test
  fun `Given a configuration with waypoints, when loading and then saving into the LoadViewModel, then the preferences and waypointsrepo are updated`() =
      runTest {
        val parser = Parser(null)
        val preferences = Preferences(preferencesStore, mockIdlingResource)
        val waypointsRepo = InMemoryWaypointsRepo(this, mockContext, StandardTestDispatcher())
        val vm =
            LoadViewModel(
                preferences,
                parser,
                waypointsRepo,
                UnconfinedTestDispatcher(),
                SimpleIdlingResource("", true))
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
        assertEquals(1, waypointsRepo.getAll().size)
        assertEquals("testClientId", preferences.clientId)
      }

  @Test
  fun `Given a configuration with an invalid key, when loading and then saving into the LoadViewModel, then no error is raised`() =
      runTest {
        val parser = Parser(null)
        val preferences = Preferences(preferencesStore, mockIdlingResource)
        val waypointsRepo = InMemoryWaypointsRepo(this, mockContext, StandardTestDispatcher())
        val vm =
            LoadViewModel(
                preferences,
                parser,
                waypointsRepo,
                UnconfinedTestDispatcher(),
                SimpleIdlingResource("", true))
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
        val waypointsRepo = InMemoryWaypointsRepo(this, mockContext, StandardTestDispatcher())
        val vm =
            LoadViewModel(
                preferences,
                parser,
                waypointsRepo,
                UnconfinedTestDispatcher(),
                SimpleIdlingResource("", true))
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
