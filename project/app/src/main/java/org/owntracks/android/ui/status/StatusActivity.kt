package org.owntracks.android.ui.status

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URISyntaxException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.net.ssl.SSLException
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_TIMEOUT
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CONNECTION_LOST
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_FAILED_AUTHENTICATION
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_INVALID_CLIENT_ID
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_NOT_AUTHORIZED
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_SERVER_CONNECT_ERROR
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_SSL_CONFIG_ERROR
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_SUBSCRIBE_FAILED
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_WRITE_TIMEOUT
import org.owntracks.android.R
import org.owntracks.android.data.EndpointState
import org.owntracks.android.databinding.UiStatusBinding
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.net.mqtt.MqttConnectionConfiguration
import org.owntracks.android.support.DrawerProvider
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException
import org.owntracks.android.ui.status.logs.LogViewerActivity

@AndroidEntryPoint
class StatusActivity : AppCompatActivity() {
    @Inject
    lateinit var drawerProvider: DrawerProvider

    @Inject
    lateinit var preferences: Preferences

    val viewModel: StatusViewModel by viewModels()
    private val batteryOptimizationIntents by lazy { BatteryOptimizingIntents(this) }
    private lateinit var binding: UiStatusBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView<UiStatusBinding>(this, R.layout.ui_status)
            .apply {
                vm = viewModel
                lifecycleOwner = this@StatusActivity
                appbar.toolbar.apply {
                    setSupportActionBar(this)
                    drawerProvider.attach(this)
                }
                dozeWhiteListed.setOnClickListener {
                    MaterialAlertDialogBuilder(this@StatusActivity)
                        .setIcon(R.drawable.ic_baseline_battery_charging_full_24)
                        .setTitle(getString(R.string.batteryOptimizationWhitelistDialogTitle))
                        .setMessage(getString(R.string.batteryOptimizationWhitelistDialogMessage))
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.batteryOptimizationWhitelistDialogButtonLabel)) { _, _ ->
                            if (viewModel.dozeWhitelisted.value == true) {
                                startActivity(batteryOptimizationIntents.settingsIntent)
                            } else {
                                startActivity(batteryOptimizationIntents.directPackageIntent)
                            }
                        }
                        .show()
                }
                viewLogsButton.setOnClickListener {
                    startActivity(
                        Intent(
                            this@StatusActivity,
                            LogViewerActivity::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                locationPermissions.setOnClickListener {
                    val showLocationPermissionsStarter = {
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:$packageName")
                            }
                        )
                    }
                    if (viewModel.locationPermissions.value != R.string.statusLocationPermissionsFineBackground) {
                        MaterialAlertDialogBuilder(this@StatusActivity).setTitle(
                            R.string.statusLocationPermissionsPromptTitle
                        )
                            .setMessage(R.string.statusLocationPermissionsPromptText)
                            .setIcon(R.drawable.ic_baseline_my_location_24)
                            .setPositiveButton(
                                R.string.statusLocationPermissionsPromptPositiveButton
                            ) { _, _ -> showLocationPermissionsStarter() }
                            .setNegativeButton(
                                R.string.statusLocationPermissionsPromptNegativeButton
                            ) { dialog, _ -> dialog.cancel() }
                            .show()
                    } else {
                        showLocationPermissionsStarter()
                    }
                }
            }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDozeModeWhitelisted()
        viewModel.refreshLocationPermissions()
    }
}

@BindingAdapter("endpointState")
fun LinearLayout.setVisibility(endpointState: EndpointState) {
    isVisible = !(endpointState.error == null && endpointState.message == null)
}

@BindingAdapter("endpointState")
fun TextView.setText(endpointState: EndpointState) {
    text = if (endpointState.error != null) {
        when (val e = endpointState.error) {
            is ConfigurationIncompleteException -> {
                when (e.cause) {
                    is MqttConnectionConfiguration.MissingHostException -> context.getString(
                        R.string.statusEndpointStateMessageMissingHost
                    )
                    is URISyntaxException -> context.getString(
                        R.string.statusEndpointStateMessageMalformedHostPort
                    )
                    is IllegalArgumentException -> e.cause!!.message
                    else -> e.toString()
                }
            }
            is MqttException -> when (val cause = e.cause) {
                is UnknownHostException -> context.getString(
                    R.string.statusEndpointStateMessageUnknownHost
                )
                is SocketTimeoutException -> context.getString(
                    R.string.statusEndpointStateMessageSocketTimeout
                )
                is SSLException -> {
                    if (cause.message != null && cause.message!!.contains("TLSV1_ALERT_CERTIFICATE_REQUIRED")) {
                        context.getString(
                            R.string.statusEndpointStateMessageTLSError,
                            "TLSV1_ALERT_CERTIFICATE_REQUIRED"
                        )
                    } else {
                        context.getString(
                            R.string.statusEndpointStateMessageTLSError,
                            cause.message
                        )
                    }
                }
                else -> when (e.reasonCode.toShort()) {
                    REASON_CODE_INVALID_PROTOCOL_VERSION -> context.getString(
                        R.string.statusEndpointStateMessageInvalidProtocolVersion
                    )
                    REASON_CODE_INVALID_CLIENT_ID -> context.getString(
                        R.string.statusEndpointStateMessageInvalidClientId
                    )
                    REASON_CODE_FAILED_AUTHENTICATION -> context.getString(
                        R.string.statusEndpointStateMessageAuthenticationFailed
                    )
                    REASON_CODE_NOT_AUTHORIZED -> context.getString(
                        R.string.statusEndpointStateMessageNotAuthorized
                    )
                    REASON_CODE_SUBSCRIBE_FAILED -> context.getString(
                        R.string.statusEndpointStateMessageSubscribeFailed
                    )
                    REASON_CODE_CLIENT_TIMEOUT -> context.getString(
                        R.string.statusEndpointStateMessageClientTimeout
                    )
                    REASON_CODE_WRITE_TIMEOUT -> context.getString(
                        R.string.statusEndpointStateMessageServerTimeout
                    )
                    REASON_CODE_SERVER_CONNECT_ERROR -> context.getString(
                        R.string.statusEndpointStateMessageUnableToConnect
                    )
                    REASON_CODE_SSL_CONFIG_ERROR -> context.getString(
                        R.string.statusEndpointStateMessageTLSConfigError
                    )
                    REASON_CODE_CONNECTION_LOST -> context.getString(
                        R.string.statusEndpointStateMessageConnectionLost,
                        cause.toString()
                    )
                    else -> e.toString()
                }
            }
            is IOException -> {
                if (e.message == "PKCS12 key store mac invalid - wrong password or corrupted file.") {
                    context.getString(R.string.statusEndpointStateMessageTLSBadClientCertPassword)
                } else {
                    e.toString()
                }
            }
            else -> e.toString()
        }
    } else {
        endpointState.message
    }
}
