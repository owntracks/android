package org.owntracks.android.ui.preferences;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.BoolRes;
import androidx.annotation.CallSuper;
import androidx.annotation.IntegerRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.owntracks.android.R;
import org.owntracks.android.services.MessageProcessorEndpointHttp;
import org.owntracks.android.services.MessageProcessorEndpointMqtt;
import org.owntracks.android.support.TimberLogFileTree;
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
import timber.log.Timber;

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


//    @Override
//    public boolean onPreferenceClick(Preference preference) {
//        switch (preference.getKey()) {
//            case UI_SCREEN_CONNECTION:
//                navigator.startActivityForResult(ConnectionActivity.class, REQUEST_CODE_CONNECTION, Intent.FLAG_ACTIVITY_NO_ANIMATION);
//                return true;
//            case UI_SCREEN_CONFIGURATION:
//                navigator.startActivity(EditorActivity.class, null, Intent.FLAG_ACTIVITY_NO_ANIMATION);
//                return true;
//            case UI_SCREEN_REPO:
//                Intent i3 = new Intent(Intent.ACTION_VIEW);
//                i3.setData(Uri.parse(getString(R.string.valRepoUrl)));
//                navigator.startActivity(i3);
//                return true;
//            case UI_SCREEN_TWITTER:
//                Intent i4 = new Intent(Intent.ACTION_VIEW);
//                i4.setData(Uri.parse(getString(R.string.valTwitterUrl)));
//                navigator.startActivity(i4);
//                return true;
//            case UI_SCREEN_DOCUMENTATION:
//                Intent i5 = new Intent(Intent.ACTION_VIEW);
//                i5.setData(Uri.parse(getString(R.string.valDocumentationUrl)));
//                navigator.startActivity(i5);
//                return true;
//        }
//        return false;
//    }
//
//    private void populatePreferencesScreen(PreferenceScreen root) {
//        populateScreenReporting((PreferenceScreen)root.findPreference("reportingScreen"));
//        populateScreenNotification((PreferenceScreen)root.findPreference("notificationScreen"));
//        populateScreenAdvanced((PreferenceScreen)root.findPreference("advancedScreen"));
//    }
//
//    private void populateScreenReporting(PreferenceScreen screen) {
//        addToolbar(screen);
//        addSwitchPreference(screen, Preferences.Keys.PUB_EXTENDED_DATA, R.string.preferencesPubExtendedData, R.string.preferencesPubExtendedDataSummary, R.bool.valPubExtendedData);
//    }
//
//    private void populateScreenAdvanced(PreferenceScreen screen) {
//        addToolbar(screen);
//        PreferenceCategory services = getCategory(R.string.preferencesCategoryAdvancedServices);
//        screen.addPreference(services);
//        addSwitchPreference(services, Preferences.Keys.REMOTE_COMMAND, R.string.preferencesRemoteCommand, R.string.preferencesRemoteCommandSummary, R.bool.valRemoteCommand);
//
//        PreferenceCategory locator = getCategory(R.string.preferencesCategoryAdvancedLocator);
//        screen.addPreference(locator);
//        addEditIntegerPreference(locator, Preferences.Keys.IGNORE_INACCURATE_LOCATIONS, R.string.preferencesIgnoreInaccurateLocations, R.integer.valIgnoreInaccurateLocations).withPreferencesSummary(R.string.preferencesIgnoreInaccurateLocationsSummary).withDialogMessage(R.string.preferencesIgnoreInaccurateLocationsDialog);
//        addEditIntegerPreference(locator, Preferences.Keys.LOCATOR_INTERVAL, R.string.preferencesLocatorInterval, R.integer.valLocatorInterval).withPreferencesSummary(R.string.preferencesLocatorIntervalSummary).withDialogMessage(R.string.preferencesLocatorIntervalDialog);
//        addEditIntegerPreference(locator, Preferences.Keys.LOCATOR_INTERVAL_MOVE_MODE, R.string.preferencesMoveModeLocatorInterval, R.integer.valMoveModeLocatorInterval).withPreferencesSummary(R.string.preferencesMoveModeLocatorIntervalSummary).withDialogMessage(R.string.preferencesMoveModeLocatorIntervalDialog);
//
//        PreferenceCategory encryption = getCategory(R.string.preferencesCategoryAdvancedEncryption);
//        screen.addPreference(encryption);
//        addEditStringPreference(encryption, Preferences.Keys._ENCRYPTION_KEY, R.string.preferencesEncryptionKey, R.string.preferencesEncryptionKeySummary, R.string.valEmpty).withDialogMessage(R.string.preferencesEncryptionKeyDialogMessage);
//
//        PreferenceCategory misc = getCategory(R.string.preferencesCategoryAdvancedMisc);
//        screen.addPreference(misc);
//        SwitchPreference p = addSwitchPreference(misc, Preferences.Keys.DEBUG_LOG, R.string.preferencesDebugLog,  R.string.preferencesDebugLogSummary, R.bool.valFalse);
//        p.setOnPreferenceChangeListener((preference, newValue) -> {
//            handleDebugLogChange((Boolean)newValue);
//            return true;
//        });
//
//
//        addSwitchPreference(misc, Preferences.Keys.AUTOSTART_ON_BOOT, R.string.preferencesAutostart, R.string.preferencesAutostartSummary, R.bool.valAutostartOnBoot);
//        addSwitchPreference(misc, Preferences.Keys.GEOCODE_ENABLED, R.string.preferencesGeocode, R.string.preferencesGeocodeSummary, R.bool.valGeocodeEnabled);
//        addEditStringPreference(misc, Preferences.Keys.OPENCAGE_GEOCODER_API_KEY, R.string.preferencesOpencageGeocoderApiKey, R.string.preferencesOpencageGeocoderApiKeySummary, R.string.valEmpty).withDialogMessage(R.string.preferencesOpencageGeocoderApiKeyDialog);
//    }
//private void populateScreenNotification(PreferenceScreen screen) {
//    addToolbar(screen);
//
//    PreferenceCategory ongoing = getCategory(R.string.preferencesCategoryNotificationOngoing);
//    screen.addPreference(ongoing);
//    addSwitchPreference(ongoing, Preferences.Keys.NOTIFICATION_LOCATION, R.string.preferencesNotificationLocation, R.string.preferencesNotificationLocationSummary, R.bool.valNotificationLocation);
//
//    PreferenceCategory background = getCategory(R.string.preferencesCategoryNotificationBackground);
//    screen.addPreference(background);
//    addSwitchPreference(background, Preferences.Keys.NOTIFICATION_EVENTS, R.string.preferencesNotificationEvents, R.string.preferencesNotificationEventsSummary, R.bool.valNotificationEvents);
//
//}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableDebugLog();
            } else {
                viewModel.getPreferences().setDebugLog(false);
            }
        }
    }

    private void enableDebugLog() {
        Timber.v("planting new log file tree");
        Timber.plant(new TimberLogFileTree(getActivity()));
    }

    private void handleDebugLogChange(Boolean newValue) {
        if (newValue) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Timber.e("permission not granted");
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);


            } else {
                Timber.d("permission granted");
                boolean debugEnabled = false;
                for (Timber.Tree t : Timber.forest()) {
                    Timber.v("Planted trees :%s", t);
                    if (t instanceof TimberLogFileTree) {
                        debugEnabled = true;
                        break;
                    }
                }
                if (!debugEnabled) {
                    enableDebugLog();
                }

            }
        } else {
            for (Timber.Tree t : Timber.forest()) {
                Timber.v("Planted trees :%s", t);
            }

            for (Timber.Tree t : Timber.forest()) {
                if (t instanceof TimberLogFileTree) {
                    Timber.v("Removing tree :%s", t);
                    Timber.uproot(t);
                }
            }
        }
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