package org.owntracks.android.ui.status

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.net.SocketTimeoutException
import java.net.URISyntaxException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.net.ssl.SSLException
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttException.*
import org.owntracks.android.R
import org.owntracks.android.data.EndpointState
import org.owntracks.android.databinding.UiStatusBinding
import org.owntracks.android.services.MqttConnectionConfiguration
import org.owntracks.android.support.DrawerProvider
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException
import org.owntracks.android.ui.status.logs.LogViewerActivity

@AndroidEntryPoint
class StatusActivity : AppCompatActivity() {
    @Inject
    lateinit var drawerProvider: DrawerProvider
    val viewModel: StatusViewModel by viewModels()
    private val batteryOptimizationIntents by lazy { BatteryOptimizingIntents(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<UiStatusBinding>(this, R.layout.ui_status)
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
            }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDozeModeWhitelisted()
    }

    companion object {
        @JvmStatic
        @BindingAdapter("app:endpointState")
        fun setText(view: TextView, endpointState: EndpointState) {
            view.text = if (endpointState.error != null) {
                when (val e = endpointState.error) {
                    is ConfigurationIncompleteException -> {
                        when (e.cause) {
                            is MqttConnectionConfiguration.MissingHostException -> view.context.getString(
                                R.string.statusEndpointStateMessageMissingHost
                            )
                            is URISyntaxException -> view.context.getString(
                                R.string.statusEndpointStateMessageMalformedHostPort
                            )
                            else -> e.toString()
                        }
                    }
                    is MqttException -> when (val mqttException = e.cause) {
                        is UnknownHostException -> view.context.getString(
                            R.string.statusEndpointStateMessageUnknownHost
                        )
                        is SocketTimeoutException -> view.context.getString(
                            R.string.statusEndpointStateMessageSocketTimeout
                        )
                        is SSLException -> view.context.getString(
                            R.string.statusEndpointStateMessageTLSError,
                            mqttException.message
                        )
                        else -> when (e.reasonCode.toShort()) {
                            REASON_CODE_INVALID_PROTOCOL_VERSION -> view.context.getString(
                                R.string.statusEndpointStateMessageInvalidProtocolVersion
                            )
                            REASON_CODE_INVALID_CLIENT_ID -> view.context.getString(
                                R.string.statusEndpointStateMessageInvalidClientId
                            )
                            REASON_CODE_FAILED_AUTHENTICATION -> view.context.getString(
                                R.string.statusEndpointStateMessageAuthenticationFailed
                            )
                            REASON_CODE_NOT_AUTHORIZED -> view.context.getString(
                                R.string.statusEndpointStateMessageNotAuthorized
                            )
                            REASON_CODE_SUBSCRIBE_FAILED -> view.context.getString(
                                R.string.statusEndpointStateMessageSubscribeFailed
                            )
                            REASON_CODE_CLIENT_TIMEOUT -> view.context.getString(
                                R.string.statusEndpointStateMessageClientTimeout
                            )
                            REASON_CODE_WRITE_TIMEOUT -> view.context.getString(
                                R.string.statusEndpointStateMessageServerTimeout
                            )
                            REASON_CODE_SERVER_CONNECT_ERROR -> view.context.getString(
                                R.string.statusEndpointStateMessageUnableToConnect
                            )
                            REASON_CODE_SSL_CONFIG_ERROR -> view.context.getString(
                                R.string.statusEndpointStateMessageTLSConfigError
                            )
                            else -> e.toString()
                        }
                    }
                    else -> e.toString()
                }
            } else {
                endpointState.message
            }
        }

        @JvmStatic
        @BindingAdapter("app:endpointState")
        fun setVisibility(view: LinearLayout, endpointState: EndpointState) {
            view.isVisible = !(endpointState.error == null && endpointState.message == null)
        }
    }
}
