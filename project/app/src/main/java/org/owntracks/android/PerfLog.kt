package org.owntracks.android

import timber.log.Timber
import kotlin.system.measureNanoTime


inline fun perfLog(description: String, block: () -> Unit) {
    if (BuildConfig.DEBUG) {
        val elapsed = measureNanoTime { block() }
        Timber.tag("PERF").d("$description: ${elapsed / 1_000_000}ms")
    } else {
        block()
    }
}

inline fun perfLog(block: () -> Unit) {
    if (BuildConfig.DEBUG) {
        val caller =
                Thread.currentThread().stackTrace[2].let { "${it.className}/ ${it.methodName}" }
        perfLog(caller, block)
    } else {
        block()
    }
}