package org.owntracks.android.e2e

import com.adevinta.android.barista.interaction.BaristaDrawerInteractions
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mqtt.broker.Broker
import mqtt.broker.interfaces.Authentication
import mqtt.broker.interfaces.PacketInterceptor
import mqtt.packets.MQTTPacket
import mqtt.packets.Qos
import mqtt.packets.mqttv5.MQTT5Properties
import org.junit.After
import org.owntracks.android.R
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.support.Parser
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.scrollToText
import org.owntracks.android.ui.writeToEditTextDialog
import timber.log.Timber


@ExperimentalUnsignedTypes
class TestWithMQTTBrokerImpl : TestWithAnMQTTBroker {
    override val mqttUsername = "testUser"
    override val mqttClientId = "testClientId"
    override val deviceId = "aa"
    private val mqttTestPassword = "testPassword"
    override val mqttPacketsReceived: MutableList<MQTTPacket> = mutableListOf()
    override val broker =
        Broker(host = "127.0.0.1",
            port = 18883,
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

    @DelicateCoroutinesApi
    override fun startBroker() {
        mqttPacketsReceived.clear()
        GlobalScope.launch { broker.listen() }
    }

    @After
    override fun stopBroker() {
        broker.stop()
    }

    override fun configureMQTTConnectionToLocal(password: String, timeoutMs: Long) {
        BaristaDrawerInteractions.openDrawer()
        clickOnAndWait(R.string.title_activity_preferences)
        clickOnAndWait(R.string.preferencesServer)
        clickOnAndWait(R.string.mode_heading)
        clickOnAndWait(R.string.mode_mqtt_private_label)
        writeToEditTextDialog(R.string.preferencesHost, "127.0.0.1")
        writeToEditTextDialog(R.string.preferencesPort, "18883")
        writeToEditTextDialog(R.string.preferencesClientId, "testClientId")
        writeToEditTextDialog(R.string.preferencesUserUsername, mqttUsername)
        writeToEditTextDialog(R.string.preferencesBrokerPassword, password)
        scrollToText(R.string.tls)
        clickOnAndWait(R.string.tls)
        BaristaDrawerInteractions.openDrawer()
        clickOnAndWait(R.string.title_activity_status)
        BaristaSleepInteractions.sleep(timeoutMs)
    }

    override fun configureMQTTConnectionToLocal(timeoutMs: Long) {
        configureMQTTConnectionToLocal(mqttTestPassword, timeoutMs)
    }
}