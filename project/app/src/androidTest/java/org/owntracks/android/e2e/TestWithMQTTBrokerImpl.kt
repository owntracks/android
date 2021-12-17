package org.owntracks.android.e2e

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import kotlinx.coroutines.DelicateCoroutinesApi
import mqtt.broker.Broker
import mqtt.broker.interfaces.Authentication
import mqtt.broker.interfaces.PacketInterceptor
import mqtt.packets.MQTTPacket
import mqtt.packets.Qos
import mqtt.packets.mqttv5.MQTT5Properties
import org.eclipse.paho.client.mqttv3.internal.websocket.Base64
import org.owntracks.android.R
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.support.Parser
import org.owntracks.android.ui.clickOnAndWait
import timber.log.Timber
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread


@ExperimentalUnsignedTypes
class TestWithMQTTBrokerImpl : TestWithAnMQTTBroker {
    private val mqttPort: Int = 18883
    override val mqttUsername = "testUser"
    override val mqttClientId = "testClientId"
    override val deviceId = "aa"
    private val mqttTestPassword = "testPassword"
    override val mqttPacketsReceived: MutableList<MQTTPacket> = mutableListOf()
    override val broker =
            Broker(host = "127.0.0.1",
                    port = mqttPort,
                    authentication = object : Authentication {
                        override fun authenticate(
                                clientId: String,
                                username: String?,
                                password: UByteArray?
                        ): Boolean {
                            return username == mqttUsername && password.contentEquals(
                                    mqttTestPassword.toByteArray().toUByteArray()
                            )
                        }
                    },
                    packetInterceptor = object : PacketInterceptor {
                        override fun packetReceived(
                                clientId: String,
                                username: String?,
                                password: UByteArray?,
                                packet: MQTTPacket
                        ) {
                            Timber.d("MQTT Packet received $packet")
                            mqttPacketsReceived.add(packet)
                        }
                    })

    override fun <E : MessageBase> Collection<E>.sendFromBroker(broker: Broker) {
        map(Parser(null)::toJsonBytes)
                .forEach {
                    broker.publish(
                            false,
                            "owntracks/someuser/somedevice",
                            Qos.AT_LEAST_ONCE,
                            MQTT5Properties(),
                            it.toUByteArray()
                    )
                }
    }

    private lateinit var brokerThread: Thread

    @DelicateCoroutinesApi
    override fun startBroker() {
        mqttPacketsReceived.clear()
        brokerThread = thread {
            broker.listen()
        }
        var listening = false
        while (!listening) {
            try {
                val socket = Socket().apply { connect(InetSocketAddress("localhost", mqttPort)) }
                listening = true
                socket.close()
            } catch (e: ConnectException) {
                Timber.i("$broker not listening yet")
                sleep(100)
            }
        }
        Timber.i("Test MQTT Broker listening $broker")
    }

    override fun stopBroker() {
        Timber.i("Test MQTT Broker stopping")
        broker.stop()
        Timber.i("Test MQTT Broker thread joining")
        brokerThread.join()
        Timber.i("Test MQTT Broker stopped")
    }

    override fun configureMQTTConnectionToLocal(password: String) {
        val config = Base64.encode("""
            {
                "_type": "configuration",
                "clientId": "$mqttClientId",
                "deviceId": "$deviceId",
                "host": "127.0.0.1",
                "password": "$password",
                "port": $mqttPort,
                "username": "$mqttUsername",
                "tls": false,
                "mqttConnectionTimeout": 1
            }
        """.trimIndent())
        InstrumentationRegistry.getInstrumentation().targetContext.startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("owntracks:///config?inline=$config")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        sleep(500)
        clickOnAndWait(R.id.save)
        BaristaDrawerInteractions.openDrawer()
        clickOnAndWait(R.string.title_activity_status)
    }

    // This will use the right password, so we should test for success
    override fun configureMQTTConnectionToLocal() {
        configureMQTTConnectionToLocal(mqttTestPassword)
        sleep(2000)
        assertContains(R.id.connectedStatus, R.string.CONNECTED)
    }
}