package org.owntracks.android.services

import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

/**
 * [org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence] is pretty crashy, full of race conditions and
 * `ENOENT` sadness. This is an attempt to do a better job.
 */
class NonCrashyMqttFilePersistence(absolutePath: String) : MemoryPersistence() // TODO implement this
