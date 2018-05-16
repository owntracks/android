package org.owntracks.android.ui.preferences;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.ArrayRes;
import android.support.annotation.BoolRes;
import android.support.annotation.CallSuper;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.injection.components.DaggerFragmentComponent;
import org.owntracks.android.injection.components.FragmentComponent;
import org.owntracks.android.injection.components.SupportFragmentComponent;
import org.owntracks.android.injection.modules.FragmentModule;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.widgets.EditIntegerPreference;
import org.owntracks.android.support.widgets.EditStringPreference;
import org.owntracks.android.support.widgets.ListIntegerPreference;
import org.owntracks.android.support.widgets.ToolbarPreference;
import org.owntracks.android.ui.base.navigator.Navigator;
import org.owntracks.android.ui.preferences.connection.ConnectionActivity;
import org.owntracks.android.ui.preferences.editor.EditorActivity;

import javax.inject.Inject;

import timber.log.Timber;

// Class cannot extend BaseFragement. BaseSupportFragment methods are implemented directly.
public class PreferencesFragment extends PreferenceFragment implements PreferencesFragmentMvvm.View, Preference.OnPreferenceClickListener {
    private static final String UI_SCREEN_ROOT = "root";
    private static final String UI_SCREEN_CONNECTION = "connectionScreen";

    private static final String UI_SCREEN_DOCUMENTATION = "documentation";
    private static final String UI_SCREEN_VERSION = "version";
    private static final String UI_SCREEN_REPO = "repo";
    private static final String UI_SCREEN_TWITTER = "twitter";
    private static final String UI_SCREEN_CONFIGURATION = "configuration";

    public static final int REQUEST_CODE_CONNECTION = 1310 ;

    @Inject protected PreferencesFragmentViewModel viewModel;
    @Inject protected Navigator navigator;

    private FragmentComponent mFragmentComponent;

    protected final FragmentComponent fragmentComponent() {
        if(mFragmentComponent == null) {
            mFragmentComponent = DaggerFragmentComponent.builder()
                    .appComponent(App.getAppComponent())
                    .fragmentModule(new FragmentModule(this))
                    .build();
        }

        return mFragmentComponent;
    }

    /* Use this method to inflate the content view for your Fragment. This method also handles
     * creating the binding, setting the view model on the binding and attaching the view. */
    protected final void setContentView(Bundle savedInstanceState) {
        if(viewModel == null) { throw new IllegalStateException("viewModel must not be null and should be injected via fragmentComponent().inject(this)"); }
        //noinspection unchecked
        viewModel.attachView(this, savedInstanceState);
    }

    @Override
    @CallSuper
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(viewModel != null) { viewModel.saveInstanceState(outState); }
    }

    @Override
    @CallSuper
    public void onDestroyView() {
        super.onDestroyView();
        if(viewModel != null) { viewModel.detachView(); }
        //binding = null;
        viewModel = null;
    }

    @Override
    @CallSuper
    public void onDestroy() {
        mFragmentComponent = null;
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fragmentComponent().inject(this);
        Timber.v("trace");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Timber.v("trace");
        if(viewModel == null) { fragmentComponent().inject(this);}
        setContentView(savedInstanceState);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void loadRoot() {
        Timber.v("trace");
        getPreferenceManager().setSharedPreferencesName(viewModel.getPreferences().getSharedPreferencesName());
        addPreferencesFromResource(R.xml.preferences_root);
        populatePreferencesScreen(PreferenceScreen.class.cast(findPreference(UI_SCREEN_ROOT)));
        attachClickListener();
    }

    public void setVersion() {
        String ver;
        try {
            PackageManager pm = viewModel.getContext().getPackageManager();
            ver = pm.getPackageInfo(viewModel.getContext().getPackageName(), 0).versionName + " (" + pm.getPackageInfo(viewModel.getContext().getPackageName(), 0).versionCode+")";
        } catch (PackageManager.NameNotFoundException e) {
            ver = viewModel.getContext().getString(R.string.na);
        }
        findPreference(UI_SCREEN_VERSION).setSummary(ver);
    }

    @Override
    public void setModeSummary(String mode) {
        findPreference(UI_SCREEN_CONNECTION).setSummary(mode);
    }

    public void attachClickListener() {
        findPreference(UI_SCREEN_CONFIGURATION).setOnPreferenceClickListener(this);
        findPreference(UI_SCREEN_REPO).setOnPreferenceClickListener(this);
        findPreference(UI_SCREEN_TWITTER).setOnPreferenceClickListener(this);
        findPreference(UI_SCREEN_DOCUMENTATION).setOnPreferenceClickListener(this);
        findPreference(UI_SCREEN_CONNECTION).setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case UI_SCREEN_CONNECTION:
                navigator.startActivityForResult(ConnectionActivity.class, REQUEST_CODE_CONNECTION, Intent.FLAG_ACTIVITY_NO_ANIMATION);
                return true;
            case UI_SCREEN_CONFIGURATION:
                navigator.startActivity(EditorActivity.class, null, Intent.FLAG_ACTIVITY_NO_ANIMATION);
                return true;
            case UI_SCREEN_REPO:
                Intent i3 = new Intent(Intent.ACTION_VIEW);
                i3.setData(Uri.parse(getString(R.string.valRepoUrl)));
                navigator.startActivity(i3);
                return true;
            case UI_SCREEN_TWITTER:
                Intent i4 = new Intent(Intent.ACTION_VIEW);
                i4.setData(Uri.parse(getString(R.string.valTwitterUrl)));
                navigator.startActivity(i4);
                return true;
            case UI_SCREEN_DOCUMENTATION:
                Intent i5 = new Intent(Intent.ACTION_VIEW);
                i5.setData(Uri.parse(getString(R.string.valDocumentationUrl)));
                navigator.startActivity(i5);
                return true;
        }
        return false;
    }

    private void populatePreferencesScreen(PreferenceScreen root) {
        populateScreenReporting((PreferenceScreen)root.findPreference("reportingScreen"));
        populateScreenNotification((PreferenceScreen)root.findPreference("notificationScreen"));
        populateScreenAdvanced((PreferenceScreen)root.findPreference("advancedScreen"));
        setupDependencies(root);
    }

    private void populateScreenReporting(PreferenceScreen screen) {
        addToolbar(screen);
        addSwitchPreference(screen, Preferences.Keys.PUB, R.string.preferencesBackgroundUpdates, R.string.preferencesBackgroundUpdatesSummary, R.bool.valPub);
        addSwitchPreference(screen, Preferences.Keys.PUB_EXTENDED_DATA, R.string.preferencesPubExtendedData, R.string.preferencesPubExtendedDataSummary, R.bool.valPubExtendedData);
    }

    private void populateScreenAdvanced(PreferenceScreen screen) {
        addToolbar(screen);
        PreferenceCategory services = getCategory(R.string.preferencesCategoryAdvancedServices);
        screen.addPreference(services);
        addSwitchPreference(services, Preferences.Keys.REMOTE_COMMAND, R.string.preferencesRemoteCommand, R.string.preferencesRemoteCommandSummary, R.bool.valRemoteCommand);

        PreferenceCategory locator = getCategory(R.string.preferencesCategoryAdvancedLocator);
        screen.addPreference(locator);
        addListIntegerPreference(locator, Preferences.Keys.LOCATOR_ACCURACY_FOREGROUND,  R.string.preferencesLocatorAccuracyForeground, R.string.preferencesLocatorAccuracyForegroundSummary, R.array.locatorAccuracy_readable, R.array.locatorAccuracy, R.integer.valLocatorAccuracyForeground);
        addListIntegerPreference(locator, Preferences.Keys.LOCATOR_ACCURACY_BACKGROUND, R.string.preferencesLocatorAccuracyBackground, R.string.preferencesLocatorAccuracyBackgroundSummary, R.array.locatorAccuracy_readable, R.array.locatorAccuracy, R.integer.valLocatorAccuracyForeground);
        addEditIntegerPreference(locator, Preferences.Keys.LOCATOR_DISPLACEMENT, R.string.preferencesLocatorDisplacement, R.integer.valLocatorDisplacement).withPreferencesSummary(R.string.preferencesLocatorDisplacementSummary);
        addEditIntegerPreference(locator, Preferences.Keys.LOCATOR_INTERVAL, R.string.preferencesLocatorInterval, R.integer.valLocatorInterval).withPreferencesSummary(R.string.preferencesLocatorIntervalSummary);

        addEditIntegerPreference(locator, Preferences.Keys.IGNORE_INACCURATE_LOCATIONS, R.string.preferencesIgnoreInaccurateLocations, R.integer.valIgnoreInaccurateLocations).withPreferencesSummary(R.string.preferencesIgnoreInaccurateLocationsSummary).withDialogMessage(R.string.preferencesIgnoreInaccurateLocationsDialog);

        PreferenceCategory encryption = getCategory(R.string.preferencesCategoryAdvancedEncryption);
        screen.addPreference(encryption);
        addEditStringPreference(encryption, Preferences.Keys._ENCRYPTION_KEY, R.string.preferencesEncryptionKey, R.string.preferencesEncryptionKeySummary, R.string.valEmpty);

        PreferenceCategory misc = getCategory(R.string.preferencesCategoryAdvancedMisc);
        screen.addPreference(misc);
        addSwitchPreference(misc, Preferences.Keys.AUTOSTART_ON_BOOT, R.string.preferencesAutostart, R.string.preferencesAutostartSummary, R.bool.valAutostartOnBoot);
    }

    private void populateScreenNotification(PreferenceScreen screen) {
        addToolbar(screen);

        PreferenceCategory ongoing = getCategory(R.string.preferencesCategoryNotificationOngoing);
        screen.addPreference(ongoing);
        addSwitchPreference(ongoing, Preferences.Keys.NOTIFICATION_LOCATION, R.string.preferencesNotificationLocation, R.string.preferencesNotificationLocationSummary, R.bool.valNotificationLocation);

        PreferenceCategory background = getCategory(R.string.preferencesCategoryNotificationBackground);
        screen.addPreference(background);
        addSwitchPreference(background, Preferences.Keys.NOTIFICATION_EVENTS, R.string.preferencesNotificationEvents, R.string.preferencesNotificationEventsSummary, R.bool.valNotificationEvents);

    }

    private void setupDependencies(PreferenceScreen root) {
        //setDependency(root, Preferences.Keys.NOTIFICATION_LOCATION, Preferences.Keys.NOTIFICATION);
        //setDependency(root, Preferences.Keys.NOTIFICATION_EVENTS, Preferences.Keys.NOTIFICATION);
    }

    private void setDependency(PreferenceScreen root, String dependingKey, String dependsOnKey) {
        try {
            root.findPreference(dependingKey).setDependency(dependsOnKey);
        } catch (IllegalStateException e) {
            Timber.e("Preference dependency could not be setup from: " + dependingKey + " to " + dependsOnKey);}
    }

    private PreferenceCategory getCategory(@StringRes int titleRes) {
        PreferenceCategory c = new PreferenceCategory(getActivity());
        c.setTitle(titleRes);
        return c;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean addSwitchPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @BoolRes int defaultValueAllModes) {
        return addSwitchPreference(parent, key, titleRes, summaryRes, defaultValueAllModes, defaultValueAllModes);
    }

    private void addToolbar(PreferenceScreen parent) {
        ToolbarPreference t = new ToolbarPreference(getActivity(), parent);
        t.setTitle(parent.getTitle());
        parent.addPreference(t);
    }

    private boolean addSwitchPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @BoolRes int defaultValueResPrivate, @BoolRes int defaultValueResPublic) {
        // Skip if no default value exists for current mode. Can be used to exclude preferences in some modes
        if((viewModel.getPreferences().isModeMqttPrivate() && defaultValueResPrivate == 0) || (viewModel.getPreferences().isModeMqttPublic() && defaultValueResPublic == 0)) {
            return false;
        }

        SwitchPreference p = new SwitchPreference(getActivity());
        p.setKey(key);
        p.setTitle(titleRes);
        p.setSummary(summaryRes);
        p.setPersistent(false);
        p.setChecked(viewModel.getPreferences().getBoolean(key, defaultValueResPrivate, defaultValueResPublic, false));
        p.setPersistent(true);
        parent.addPreference(p);
        return true;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean addEditStringPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @StringRes int defaultValueAllModes) {
        return addEditStringPreference(parent, key, titleRes, summaryRes, defaultValueAllModes, defaultValueAllModes);
    }

    private boolean addEditStringPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @StringRes int defaultValueResPrivate, @StringRes int defaultValueResPublic) {
        // Skip if no default value exists for current mode. Can be used to exclude preferences in some modes
        if((viewModel.getPreferences().isModeMqttPrivate() && defaultValueResPrivate == 0) || (viewModel.getPreferences().isModeMqttPublic() && defaultValueResPublic == 0)) {
            return false;
        }

        EditStringPreference p = new EditStringPreference(getActivity());
        p.setKey(key);
        p.setTitle(titleRes);
        p.setDialogTitle(titleRes);
        p.setSummary(summaryRes);
        p.setPersistent(false);
        p.setText(getEditStringPreferenceTextValueWithHintSupport(key));
        p.setHint(viewModel.getPreferences().getStringDefaultValue(defaultValueResPrivate, defaultValueResPublic));
        p.setPersistent(true);

        parent.addPreference(p);
        return true;
    }

    private EditIntegerPreference addEditIntegerPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @IntegerRes int defaultValueAllModes) {
        return addEditIntegerPreference(parent, key,  titleRes, defaultValueAllModes, defaultValueAllModes);
    }

    private EditIntegerPreference addEditIntegerPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @IntegerRes int defaultValueResPrivate, @IntegerRes int defaultValueResPublic) {
        EditIntegerPreference p = new EditIntegerPreference(getActivity());
        p.setKey(key);
        p.setDialogTitle(titleRes);
        p.setTitle(titleRes);
        p.setPersistent(false);
        p.setText(getEditIntegerPreferenceTextValueWithHintSupport(key));
        p.setHint(Integer.toString(viewModel.getPreferences().getIntegerDefaultValue(defaultValueResPrivate, defaultValueResPublic)));
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

    @SuppressWarnings("UnusedReturnValue")
    private boolean addListIntegerPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @ArrayRes int entriesRes, @ArrayRes int entryValuesRes, @IntegerRes int defaultValueAllModes) {
        return addListIntegerPreference(parent, key, titleRes, summaryRes, entriesRes, entryValuesRes, defaultValueAllModes, defaultValueAllModes);
    }

    private boolean addListIntegerPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @ArrayRes int entriesRes, @ArrayRes int entryValuesRes, @IntegerRes int defaultValueResPrivate, @IntegerRes int defaultValueResPublic) {
        // Skip if no default value exists for current mode. Can be used to exclude preferences in some modes
        if((viewModel.getPreferences().isModeMqttPrivate() && defaultValueResPrivate == 0) || (viewModel.getPreferences().isModeMqttPublic() && defaultValueResPublic == 0)) {
            return false;
        }

        ListIntegerPreference p = new ListIntegerPreference(parent.getContext());
        p.setKey(key);
        p.setTitle(titleRes);
        p.setDialogTitle(titleRes);
        p.setSummary(summaryRes);
        p.setEntries(entriesRes);
        p.setEntryValues(entryValuesRes);

        p.setPersistent(false);
        p.setValueIndex(viewModel.getPreferences().getInt(key, defaultValueResPrivate, defaultValueResPublic, false));
        p.setPersistent(true);

        parent.addPreference(p);
        return true;
    }

}