package org.owntracks.android.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedList
import java.util.Queue

/**
 * Duration options for custom toast display.
 */
enum class ToastDuration(val millis: Long) {
    Short(2000L),
    Long(3500L)
}

/**
 * Data class representing a toast message.
 */
data class ToastData(
    val message: String,
    val duration: ToastDuration = ToastDuration.Short
)

/**
 * State holder for managing toast queue and display.
 * Use [rememberToastState] to create and remember an instance.
 */
@Stable
class ToastState {
    private val queue: Queue<ToastData> = LinkedList()
    private val mutex = Mutex()

    var currentToast by mutableStateOf<ToastData?>(null)
        private set

    var isVisible by mutableStateOf(false)
        private set

    /**
     * Shows a toast message. If a toast is already showing, the new one is queued.
     */
    suspend fun show(message: String, duration: ToastDuration = ToastDuration.Short) {
        show(ToastData(message, duration))
    }

    /**
     * Shows a toast message. If a toast is already showing, the new one is queued.
     */
    suspend fun show(toast: ToastData) {
        mutex.withLock {
            if (currentToast == null) {
                currentToast = toast
                isVisible = true
            } else {
                queue.offer(toast)
            }
        }
    }

    /**
     * Cancels the current toast and shows the next one in the queue if available.
     */
    suspend fun cancelCurrent() {
        mutex.withLock {
            isVisible = false
        }
    }

    /**
     * Cancels all toasts including queued ones.
     */
    suspend fun cancelAll() {
        mutex.withLock {
            queue.clear()
            isVisible = false
        }
    }

    /**
     * Internal function called when toast animation completes hiding.
     */
    internal suspend fun onToastDismissed() {
        mutex.withLock {
            currentToast = null
            val next = queue.poll()
            if (next != null) {
                currentToast = next
                isVisible = true
            }
        }
    }
}

/**
 * Creates and remembers a [ToastState] instance.
 */
@Composable
fun rememberToastState(): ToastState {
    return remember { ToastState() }
}

/**
 * Custom toast host that displays toasts from the given [toastState].
 * Place this at the root of your composable hierarchy, typically inside a Box.
 *
 * @param toastState The state holder managing toast display
 * @param modifier Modifier for the container
 */
@Composable
fun CustomToastHost(
    toastState: ToastState,
    modifier: Modifier = Modifier
) {
    val currentToast = toastState.currentToast
    val isVisible = toastState.isVisible

    // Auto-dismiss timer
    LaunchedEffect(currentToast, isVisible) {
        if (currentToast != null && isVisible) {
            delay(currentToast.duration.millis)
            toastState.cancelCurrent()
        }
    }

    // Handle animation completion
    LaunchedEffect(isVisible) {
        if (!isVisible && currentToast != null) {
            delay(300) // Wait for exit animation
            toastState.onToastDismissed()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = isVisible && currentToast != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            currentToast?.let { toast ->
                ToastContent(
                    message = toast.message,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun ToastContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
