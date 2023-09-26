package org.owntracks.android.testutils

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread
import kotlinx.coroutines.DelicateCoroutinesApi
import mqtt.broker.Broker
import mqtt.broker.interfaces.Authentication
import mqtt.broker.interfaces.PacketInterceptor
import mqtt.packets.MQTTPacket
import mqtt.packets.Qos
import mqtt.packets.mqtt.MQTTPublish
import mqtt.packets.mqttv5.MQTT5Properties
import org.eclipse.paho.client.mqttv3.internal.websocket.Base64
import org.owntracks.android.R
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.support.Parser
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.preferences.load.LoadActivity
import timber.log.Timber

@ExperimentalUnsignedTypes
class TestWithAnMQTTBrokerImpl : TestWithAnMQTTBroker {
    private val mqttPort: Int = 18883
    override val mqttUsername = "testUser"
    override val mqttClientId = "testClientId"
    override val deviceId = "aa"
    private val mqttTestPassword = "testPassword"
    override val mqttPacketsReceived: MutableList<MQTTPacket> = mutableListOf()
    override lateinit var broker: Broker
    override val packetReceivedIdlingResource = LatchingIdlingResourceWithData("mqttPacketReceivedIdlingResource")

    override fun MessageBase.sendFromBroker(
        broker: Broker,
        topicName: String,
        retain: Boolean
    ) {
        val actualTopic = topicName + this@sendFromBroker.baseTopicSuffix
        Timber.i("Publishing ${this::class.java.simpleName} message to $actualTopic with retain=$retain")
        this.toJsonBytes(Parser(null))
            .run {
                broker.publish(
                    retain,
                    actualTopic,
                    Qos.AT_LEAST_ONCE,
                    MQTT5Properties(),
                    toUByteArray()
                )
            }
    }

    private lateinit var brokerThread: Thread
    private var shouldBeRunning = false

    @DelicateCoroutinesApi
    override fun startBroker() {
        mqttPacketsReceived.clear()
        Timber.i("Starting MQTT Broker")
        broker = createNewBroker()
        shouldBeRunning = true
        brokerThread = thread {
            while (shouldBeRunning) {
                Timber.i("Calling MQTT Broker listen")
                broker.listen()
                Timber.i("MQTT Broker no longer listening")
            }
            Timber.i("MQTT Broker Thread ending")
        }
        var listening = false
        while (!listening) {
            Socket().use {
                try {
                    it.apply { connect(InetSocketAddress("localhost", mqttPort)) }
                    listening = true
                    Timber.i("Test MQTT Broker listening on port $mqttPort")
                } catch (e: ConnectException) {
                    Timber.i(e, "broker not listening on $mqttPort yet")
                    Thread.sleep(100)
                }
            }
        }
    }

    private fun createNewBroker(): Broker =
        Broker(
            host = "127.0.0.1",
            port = mqttPort,
            authentication = object : Authentication {
                override fun authenticate(
                    clientId: String,
                    username: String?,
                    password: UByteArray?
                ): Boolean {
                    return username == mqttUsername && password.contentEquals(
                        mqttTestPassword.toByteArray()
                            .toUByteArray()
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
                    synchronized(mqttPacketsReceived) {
                        val packetString = String(packet.toByteArray().toByteArray())
                        Timber.v("MQTT Packet received $packet $packetString")
                        mqttPacketsReceived.add(packet)
                        val magic = packetReceivedIdlingResource.data ?: ""
                        if (packet is MQTTPublish && packetString.contains(magic)) {
                            Timber.v("packet contains magic string $magic. Unlatching")
                            packetReceivedIdlingResource.unlatch()
                        }
                    }
                }
            }
        )

    override fun stopBroker() {
        if (::brokerThread.isInitialized) {
            shouldBeRunning = false
            Timber.i("Requesting MQTT Broker stop")
            if (this::broker.isInitialized) {
                broker.stop()
            }
            Timber.i("Waiting to join thread")
            brokerThread.join()
            Timber.i("MQTT Broker stopped")
        }
    }

    override fun configureMQTTConnectionToLocal(password: String) {
        val config = Base64.encode(
            //language=JSON
            """
            {
                "_type": "configuration",
                "clientId": "$mqttClientId",
                "deviceId": "$deviceId",
                "tid": "$deviceId",
                "host": "127.0.0.1",
                "password": "$password",
                "port": $mqttPort,
                "mqttProtocolLevel": 4,
                "username": "$mqttUsername",
                "tls": false,
                "keepalive": 5,
                "connectionTimeoutSeconds": 2
            }
            """.trimIndent()
        )
        InstrumentationRegistry.getInstrumentation().targetContext.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("owntracks:///config?inline=$config")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        waitUntilActivityVisible<LoadActivity>()
        val activity = getCurrentActivity() as LoadActivity
        activity.importStatusIdlingResource.use {
            clickOnAndWait(R.id.save)
        }
    }

    // This will use the right password, so we should test for success
    override fun configureMQTTConnectionToLocalWithGeneratedPassword() {
        configureMQTTConnectionToLocal(mqttTestPassword)
    }
}
