package org.owntracks.android.ui.preferences.about;

import androidx.fragment.app.Fragment;

import org.owntracks.android.ui.preferences.PreferencesActivity;

public class AboutActivity extends PreferencesActivity {
    @Override
    protected Fragment getStartFragment() {
        return new AboutFragment();
    }
}
