package org.owntracks.android.ui.welcome

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.flow.StateFlow
import org.owntracks.android.R
import org.owntracks.android.ui.welcome.fragments.PlayFragmentViewModel

@Composable
fun PlayServicesPage(
    welcomeViewModel: WelcomeViewModel,
    refreshFlow: StateFlow<Int>,
    onRequestFix: () -> Unit,
    onCheckAvailability: (PlayFragmentViewModel) -> WelcomeViewModel.ProgressState,
) {
  val playViewModel: PlayFragmentViewModel = hiltViewModel()
  val refreshKey by refreshFlow.collectAsState(initial = 0)

  val updateState = {
    val state = onCheckAvailability(playViewModel)
    welcomeViewModel.setWelcomeState(state)
  }

  LaunchedEffect(Unit) { updateState() }
  LaunchedEffect(refreshKey) { updateState() }
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { updateState() }

  val message by playViewModel.message.observeAsState("")
  val fixAvailable by playViewModel.playServicesFixAvailable.observeAsState(false)

  WelcomePageLayout(
      iconRes = R.drawable.ic_baseline_assignment_late_48,
      iconContentDescription = stringResource(id = R.string.icon_description_warning),
      title = stringResource(id = R.string.welcome_play_heading),
      description = stringResource(id = R.string.welcome_play_description)) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
        if (fixAvailable) {
          OutlinedButton(
              modifier = Modifier.testTag(WelcomeTestTags.PLAY_SERVICES_FIX_BUTTON),
              onClick = onRequestFix) {
                Text(text = stringResource(id = R.string.welcomeFixIssue))
              }
        }
      }
}
