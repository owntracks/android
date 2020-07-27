package org.owntracks.android.ui.preferences.logs

import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import javax.inject.Inject

@PerActivity
public class LogViewerViewModel @Inject constructor() : BaseViewModel<LogViewerMvvm.View>(), LogViewerMvvm.ViewModel<LogViewerMvvm.View> {


}