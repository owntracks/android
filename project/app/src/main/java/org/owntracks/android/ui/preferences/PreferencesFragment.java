package org.owntracks.android.ui.preferences;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.BoolRes;
import androidx.annotation.CallSuper;
import androidx.annotation.IntegerRes;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.owntracks.android.R;
import org.owntracks.android.services.MessageProcessorEndpointHttp;
import org.owntracks.android.services.MessageProcessorEndpointMqtt;
import org.owntracks.android.support.widgets.EditIntegerPreference;
import org.owntracks.android.support.widgets.EditStringPreference;
import org.owntracks.android.support.widgets.ToolbarPreference;
import org.owntracks.android.ui.base.navigator.Navigator;
import org.owntracks.android.ui.preferences.connection.ConnectionActivity;
import org.owntracks.android.ui.preferences.editor.EditorActivity;

import javax.inject.Inject;

import dagger.android.DispatchingAndroidInjector;

import dagger.android.HasAndroidInjector;
import dagger.android.support.AndroidSupportInjection;

import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;

// Class cannot extend BaseFragement. BaseSupportFragment methods are implemented directly.
public class PreferencesFragment extends PreferenceFragmentCompat implements PreferencesFragmentMvvm.View, Preference.OnPreferenceClickListener, HasAndroidInjector {
    private static final String UI_SCREEN_ROOT = "root";
    private static final String UI_PREFERENCE_SCREEN_CONNECTION = "connectionScreen";

    private static final String UI_SCREEN_DOCUMENTATION = "documentation";

    private static final String UI_SCREEN_REPO = "repo";
    private static final String UI_SCREEN_TWITTER = "twitter";
    private static final String UI_SCREEN_CONFIGURATION = "configuration";

    public static final int REQUEST_CODE_CONNECTION = 1310;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1311;
    @Inject
    PreferencesFragmentViewModel viewModel;
    @Inject
    Navigator navigator;

    @Inject
    DispatchingAndroidInjector<Object> fragmentInjector;

    @Override
    public AndroidInjector<Object> androidInjector() {
        return fragmentInjector;
    }

    @Override
    @CallSuper
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (viewModel != null) {
            viewModel.saveInstanceState(outState);
        }
    }

    @Override
    @CallSuper
    public void onDestroyView() {
        super.onDestroyView();
        if (viewModel != null) {
            viewModel.detachView();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        findPreference(UI_PREFERENCE_SCREEN_CONNECTION).setSummary(getConnectionMode());
    }

    private String getConnectionMode() {
        if (viewModel != null) {
            switch (viewModel.getPreferences().getModeId()) {
                case MessageProcessorEndpointHttp.MODE_ID:
                    return getString(R.string.mode_http_private_label);
                case MessageProcessorEndpointMqtt.MODE_ID:
                default:
                    return getString(R.string.mode_mqtt_private_label);
            }
        }
        return "";
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_root, rootKey);
        viewModel.attachView(this, savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(viewModel.getPreferences().getSharedPreferencesName());

        // Have to do these manually here, as there's an android bug that prevents the activity from being found when launched from intent declared on the preferences XML.
        findPreference(UI_SCREEN_CONFIGURATION).setIntent(new Intent(getContext(), EditorActivity.class).addFlags(FLAG_ACTIVITY_NO_ANIMATION));

        //TODO move this to a preferences fragment rather than its own activity.
        findPreference(UI_PREFERENCE_SCREEN_CONNECTION).setIntent(new Intent(getContext(), ConnectionActivity.class).addFlags(FLAG_ACTIVITY_NO_ANIMATION));
    }



    private PreferenceCategory getCategory(@StringRes int titleRes) {
        PreferenceCategory c = new PreferenceCategory(getActivity());
        c.setTitle(titleRes);
        return c;
    }

    private void addToolbar(PreferenceScreen parent) {
        ToolbarPreference t = new ToolbarPreference(getActivity(), parent);
        t.setTitle(parent.getTitle());
        parent.addPreference(t);
    }

    private SwitchPreference addSwitchPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @BoolRes int defaultValueAllModes) {

        SwitchPreference p = new SwitchPreference(getActivity());
        p.setKey(key);
        p.setTitle(titleRes);
        p.setSummary(summaryRes);
        p.setPersistent(false);
        p.setChecked(viewModel.getPreferences().getBoolean(key, defaultValueAllModes));
        p.setPersistent(true);
        parent.addPreference(p);
        return p;
    }

    @SuppressWarnings("UnusedReturnValue")
    private EditStringPreference addEditStringPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @StringRes int defaultValueAllModes) {
        EditStringPreference p = new EditStringPreference(getActivity());
        p.setKey(key);
        p.setTitle(titleRes);
        p.setDialogTitle(titleRes);
        p.setSummary(summaryRes);
        p.setPersistent(false);
        p.setText(getEditStringPreferenceTextValueWithHintSupport(key));
        p.setHint(getString(defaultValueAllModes));
        p.setPersistent(true);
        parent.addPreference(p);
        return p;
    }

    private EditIntegerPreference addEditIntegerPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @IntegerRes int defaultValueAllModes) {
        EditIntegerPreference p = new EditIntegerPreference(getActivity());
        p.setKey(key);
        p.setDialogTitle(titleRes);
        p.setTitle(titleRes);
        p.setPersistent(false);
        p.setText(getEditIntegerPreferenceTextValueWithHintSupport(key));
        p.setHint(Integer.toString(getResources().getInteger((defaultValueAllModes))));
        p.setPersistent(true);
        parent.addPreference(p);
        return p;
    }

    private String getEditStringPreferenceTextValueWithHintSupport(String key) {
        return viewModel.getPreferences().getString(key, R.string.valEmpty);
    }

    // returns an empty string if no key value is found so that a hint can be displayed
    private String getEditIntegerPreferenceTextValueWithHintSupport(String key) {
        int i = viewModel.getPreferences().getInt(key, R.integer.valInvalid);
        if (i == -1) {
            return "";
        } else {
            return Integer.toString(i);
        }
    }


    //// Don't need?

    /* Use this method to inflate the content view for your Fragment. This method also handles
     * creating the binding, setting the view model on the binding and attaching the view. */
    private void setContentView(Bundle savedInstanceState) {
        if (viewModel == null) {
            throw new IllegalStateException("viewModel must not be null and should be injected via fragmentComponent().inject(this)");
        }
        viewModel.attachView(this, savedInstanceState);
    }

    public void loadRoot() {
    }

    @Override
    public void setModeSummary(int modeId) {
    }
}