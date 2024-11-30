package org.owntracks.android.testutils

import androidx.test.espresso.IdlingResource
import kotlinx.coroutines.DelicateCoroutinesApi
import mqtt.broker.Broker
import mqtt.packets.MQTTPacket
import org.junit.After
import org.junit.Before
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.testutils.idlingresources.LatchingIdlingResourceWithData

@ExperimentalUnsignedTypes
interface TestWithAnMQTTBroker {
  fun configureMQTTConnectionToLocal(idlingResource: IdlingResource, password: String)

  fun configureMQTTConnectionToLocalWithGeneratedPassword(idlingResource: IdlingResource)

  val mqttPacketsReceived: MutableList<MQTTPacket>
  val broker: Broker
  val mqttUsername: String
  val mqttClientId: String
  val deviceId: String
  val packetReceivedIdlingResource: LatchingIdlingResourceWithData

  fun MessageBase.sendFromBroker(
      broker: Broker,
      topicName: String = "owntracks/someuser/somedevice",
      retain: Boolean = false
  )

  fun <E : MessageBase> Collection<E>.sendFromBroker(
      broker: Broker,
      topicName: String = "owntracks/someuser/somedevice",
      retain: Boolean = false
  ) = forEach { it.sendFromBroker(broker, topicName, retain) }

  @DelicateCoroutinesApi fun startBroker()

  fun stopBroker()

  @After
  fun mqttAfter() {
    stopBroker()
  }

  @DelicateCoroutinesApi
  @Before
  fun mqttBefore() {
    startBroker()
  }
}
