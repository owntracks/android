package org.owntracks.android.services.worker

import androidx.work.ListenableWorker
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

@MapKey
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WorkerKey(val value: KClass<out ListenableWorker>)

@InstallIn(SingletonComponent::class)
@Module
interface WorkerModule {

  @Binds
  @IntoMap
  @WorkerKey(MQTTReconnectWorker::class)
  fun bindMQTTReconnectWorkerFactory(factory: MQTTReconnectWorker.Factory): ChildWorkerFactory

  @Binds
  @IntoMap
  @WorkerKey(SendLocationPingWorker::class)
  fun bindSendLocationPingWorkerFactory(factory: SendLocationPingWorker.Factory): ChildWorkerFactory
}
