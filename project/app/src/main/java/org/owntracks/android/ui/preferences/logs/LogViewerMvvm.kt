package org.owntracks.android.ui.preferences.logs

import org.owntracks.android.ui.base.view.MvvmView
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel

interface LogViewerMvvm {
    interface View : MvvmView {

    }
    interface ViewModel<V : MvvmView?> : MvvmViewModel<V> {

    }
}