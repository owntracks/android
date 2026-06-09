package org.owntracks.android.location.external

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.location.Location
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject
import javax.inject.Singleton
import org.owntracks.android.location.LocationCallback
import org.owntracks.android.location.LocationResult
import timber.log.Timber

/**
 * Application-scoped coordinator for the external USB GNSS feature.
 *
 * - Owns a single [ExternalGnssManager] instance.
 * - Handles USB attach/detach and runtime permission flow.
 * - Receives GNSS fixes from the manager and fans them out as synthetic [Location] updates to every
 *   [LocationCallback] that has been registered via the [ExternalGnssLocationProviderClient]
 *   wrapper, so that OwnTracks publishes them through its standard pipeline.
 * - Reacts to changes in [NtripConfig] persisted in [SharedPreferences].
 */
@Singleton
class ExternalGnssController
@Inject
constructor(@ApplicationContext private val appContext: Context) {

  private val usbManager =
      appContext.getSystemService(Context.USB_SERVICE) as UsbManager

  private val prefs: SharedPreferences =
      appContext.getSharedPreferences(NtripConfig.PREFS_NAME, Context.MODE_PRIVATE)

  private val callbacks = CopyOnWriteArraySet<LocationCallback>()

  private val startupExecutor =
      java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "external-gnss-startup").apply { isDaemon = true }
      }

  @Volatile private var lastLocation: Location? = null

  private val manager: ExternalGnssManager =
      ExternalGnssManager(
          context = appContext,
          onFix = { lat, lon, alt, accM -> dispatchFix(lat, lon, alt, accM) },
          onStatus = { Timber.i("[ExternalGnss] %s", it) },
          onActiveChanged = { Timber.i("[ExternalGnss] active=%s", it) },
      )

  private val permissionReceiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          if (intent.action != ACTION_USB_PERMISSION) return
          val device: UsbDevice? =
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
              } else {
                @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
              }
          val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
          if (granted && device != null) {
            startupExecutor.execute {
              runCatching { startWithDevice(device) }
                  .onFailure { Timber.e(it, "[ExternalGnss] startWithDevice failed") }
            }
          } else {
            Timber.w("USB permission denied for %s", device?.deviceName)
          }
        }
      }

  private val detachReceiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED && manager.isRunning) {
            Timber.i("USB device detached; stopping external GNSS")
            startupExecutor.execute { runCatching { manager.stop() } }
          }
        }
      }

  private val prefsListener =
      SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        startupExecutor.execute {
          runCatching { onConfigChanged() }
              .onFailure { Timber.e(it, "[ExternalGnss] onConfigChanged failed") }
        }
      }

  init {
    registerReceivers()
    prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    runCatching { manager.updateNtripConfig(NtripConfig.load(appContext)) }
        .onFailure { Timber.w(it, "[ExternalGnss] updateNtripConfig at init failed") }
    // Auto-start if the feature is already enabled and a supported device is attached.
    // Run off the injection thread: USB open + UBX writes are blocking I/O and must never
    // run on the thread that Hilt uses to satisfy @Inject; otherwise BackgroundService.onCreate
    // can ANR/crash.
    if (isExternalGnssEnabled) {
      Timber.i("[ExternalGnss] Feature enabled at boot; scheduling auto-start")
      startupExecutor.execute {
        runCatching { tryStartFromAttachedDevices() }
            .onFailure { Timber.e(it, "[ExternalGnss] auto-start failed") }
      }
    } else {
      Timber.d("[ExternalGnss] Feature disabled at boot; not starting")
    }
  }

  private fun registerReceivers() {
    val permissionFilter = IntentFilter(ACTION_USB_PERMISSION)
    val detachFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.registerReceiver(
          appContext, permissionReceiver, permissionFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
      ContextCompat.registerReceiver(
          appContext, detachReceiver, detachFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
    } else {
      appContext.registerReceiver(permissionReceiver, permissionFilter)
      appContext.registerReceiver(detachReceiver, detachFilter)
    }
  }

  // ---------------------------------------------------------------------------
  // Public API called from LocationProviderClient wrapper
  // ---------------------------------------------------------------------------

  fun registerCallback(cb: LocationCallback) {
    callbacks.add(cb)
  }

  fun unregisterCallback(cb: LocationCallback) {
    callbacks.remove(cb)
  }

  fun getLastExternalLocation(): Location? = lastLocation

  val isExternalGnssEnabled: Boolean
    get() = prefs.getBoolean(NtripConfig.KEY_EXTERNAL_ENABLED, false)

  // ---------------------------------------------------------------------------
  // Called from MapActivity when the USB_DEVICE_ATTACHED intent fires
  // ---------------------------------------------------------------------------

  fun handleUsbAttachIntent(intent: Intent?) {
    if (intent == null) return
    val device: UsbDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
          @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    if (device != null) {
      startupExecutor.execute {
        runCatching { requestPermissionAndStart(device) }
            .onFailure { Timber.e(it, "[ExternalGnss] handleUsbAttachIntent failed") }
      }
    }
  }

  /**
   * Called from preferences/UI when the user toggles the feature or taps "connect". Scans attached
   * USB devices and asks permission for the first supported one.
   */
  fun tryStartFromAttachedDevices() {
    if (!isExternalGnssEnabled) {
      startupExecutor.execute { runCatching { manager.stop() } }
      return
    }
    startupExecutor.execute {
      runCatching {
            val candidate =
                usbManager.deviceList.values.firstOrNull {
                  it.vendorId in SUPPORTED_VENDOR_IDS
                } ?: usbManager.deviceList.values.firstOrNull()
            if (candidate != null) {
              requestPermissionAndStart(candidate)
            } else {
              Timber.i("No USB devices attached")
            }
          }
          .onFailure { Timber.e(it, "[ExternalGnss] tryStartFromAttachedDevices failed") }
    }
  }

  fun stop() {
    startupExecutor.execute { runCatching { manager.stop() } }
  }

  // ---------------------------------------------------------------------------
  // Internal
  // ---------------------------------------------------------------------------

  @SuppressLint("InlinedApi")
  private fun requestPermissionAndStart(device: UsbDevice) {
    if (!isExternalGnssEnabled) return
    if (usbManager.hasPermission(device)) {
      startWithDevice(device)
      return
    }
    val flags =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
          PendingIntent.FLAG_UPDATE_CURRENT
        }
    val intent = Intent(ACTION_USB_PERMISSION).setPackage(appContext.packageName)
    val pi = PendingIntent.getBroadcast(appContext, 0, intent, flags)
    usbManager.requestPermission(device, pi)
  }

  private fun startWithDevice(device: UsbDevice) {
    if (!isExternalGnssEnabled) return
    if (manager.isRunning) {
      // Update NTRIP config anyway in case it changed.
      manager.updateNtripConfig(NtripConfig.load(appContext))
      return
    }
    manager.updateNtripConfig(NtripConfig.load(appContext))
    manager.start(device)
  }

  private fun onConfigChanged() {
    val cfg = NtripConfig.load(appContext)
    val enabled = isExternalGnssEnabled
    if (!enabled) {
      manager.stop()
    } else {
      manager.updateNtripConfig(cfg)
      if (!manager.isRunning) {
        tryStartFromAttachedDevices()
      }
    }
  }

  private fun dispatchFix(lat: Double, lon: Double, alt: Double, accM: Double) {
    if (!isExternalGnssEnabled) return
    if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED) {
      // OwnTracks publishes Location objects freely once permissions have been granted by the
      // user; without ACCESS_FINE_LOCATION the standard provider wouldn't be reporting either,
      // so we mirror that behaviour.
      return
    }
    val location =
        Location(PROVIDER_NAME).apply {
          latitude = lat
          longitude = lon
          altitude = alt
          if (accM > 0.0) {
            accuracy = accM.toFloat()
          }
          time = System.currentTimeMillis()
          elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
    lastLocation = location
    val result = LocationResult(location)
    callbacks.forEach { cb ->
      try {
        cb.onLocationResult(result)
      } catch (t: Throwable) {
        Timber.w(t, "Callback %s threw on external fix", cb)
      }
    }
  }

  companion object {
    const val PROVIDER_NAME = "external_gnss"
    private const val ACTION_USB_PERMISSION = "org.owntracks.android.USB_PERMISSION"

    /** Vendor IDs of the USB-serial chips we support. Mirrors usb_device_filter.xml. */
    private val SUPPORTED_VENDOR_IDS =
        setOf(
            0x1546, // u-blox
            0x0403, // FTDI
            0x10C4, // Silicon Labs CP210x
            0x1A86, // QinHeng CH34x
            0x067B, // Prolific PL2303
        )
  }
}
