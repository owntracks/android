package org.owntracks.android.support;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

// DialogPreference is abstract, so let's implement it to use it. What the f**k Android?
public class ImplementedDialogPreference extends DialogPreference {
	public ImplementedDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
}
