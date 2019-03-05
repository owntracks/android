package org.owntracks.android.support;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class TimberLogTree extends Timber.DebugTree {
    @Override
    protected String createStackElementTag(@NonNull StackTraceElement element) {
        return super.createStackElementTag(element) + "/" + element.getMethodName() + "/" + element.getLineNumber();
    }
}
