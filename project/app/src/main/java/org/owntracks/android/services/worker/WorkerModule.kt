package org.owntracks.android.services.worker

import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

@MapKey
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WorkerKey(val value: KClass<out ListenableWorker>)

@Module
interface WorkerModule {
    @Binds
    @IntoMap
    @WorkerKey(MQTTMaybeReconnectAndPingWorker::class)
    fun bindMQTTMaybeReconnectAndPingWorkerFactory(factory: MQTTMaybeReconnectAndPingWorker.Factory): ChildWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(MQTTReconnectWorker::class)
    fun bindMQTTReconnectWorkerFactory(factory: MQTTReconnectWorker.Factory): ChildWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(SendLocationPingWorker::class)
    fun bindSendLocationPingWorkerFactory(factory: SendLocationPingWorker.Factory): ChildWorkerFactory

    @Binds
    fun bindWorkerFactory(factory: BackgroundWorkerFactory): WorkerFactory
}