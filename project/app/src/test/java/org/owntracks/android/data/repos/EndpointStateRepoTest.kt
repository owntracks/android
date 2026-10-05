package org.owntracks.android.data.repos

import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.owntracks.android.data.EndpointState

@OptIn(ExperimentalCoroutinesApi::class)
class EndpointStateRepoTest {
  @Test
  fun `an error does not stick to later states of the same kind`() = runTest {
    val repo = EndpointStateRepo()

    repo.setState(EndpointState.ERROR.withError(IOException("first")))
    repo.setState(EndpointState.ERROR.withMessage("second"))

    assertNull(repo.endpointState.value.error)
    assertEquals("second", repo.endpointState.value.message)
  }

  @Test
  fun `a message does not stick to a later bare state`() = runTest {
    val repo = EndpointStateRepo()

    repo.setState(EndpointState.IDLE.withMessage("Response 200"))
    repo.setState(EndpointState.CONNECTING)
    repo.setState(EndpointState.IDLE)

    assertNull(repo.endpointState.value.message)
  }

  @Test
  fun `a new error on the same state is emitted`() = runTest {
    val repo = EndpointStateRepo()
    val seen = mutableListOf<Throwable?>()
    val collector =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
          repo.endpointState.collect { seen.add(it.error) }
        }
    val first = IOException("first")
    val second = IOException("second")

    repo.setState(EndpointState.ERROR.withError(first))
    repo.setState(EndpointState.ERROR.withError(second))

    assertEquals(listOf(null, first, second), seen)
    collector.cancel()
  }
}
