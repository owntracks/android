package org.owntracks.android.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.owntracks.android.R;
import org.owntracks.android.services.ServiceBroker;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.DrawerFactory;
import org.owntracks.android.support.EditIntegerPreference;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;

import de.greenrobot.event.EventBus;

public class ActivityPreferences extends ActivityBase {
    private static final String TAG = "ActivityPreferences";

    static Preference connectionPreferenceScreen;
    private static final int REQUEST_CODE_CONNECTION = 1310 ;


    // Return from ActivityPreferencesConnection to see if we need to reload the preferences because a mode change occured
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
        Log.v(TAG, "onActivityResult: RequestCode: " + requestCode + " resultCode: " + resultCode);
        switch(requestCode) {
            case (REQUEST_CODE_CONNECTION) : {
                Log.v(TAG, "onActivityResult with REQUEST_CODE_CONNECTION");
                if(resultIntent != null && resultIntent.getBooleanExtra(ActivityPreferencesConnection.KEY_MODE_CHANGED, false)) {
                    Log.v(TAG,"recreating ActivityPreferences due to mode change");
                    this.recreate();
                }

                break;
            }
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = this;

        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = (Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getTitle());
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Drawer.OnDrawerItemClickListener drawerListener = new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                switch (drawerItem.getIdentifier()) {
                    case R.string.idLocations:
                        goToRoot();
                        return true;
                    case R.string.idPager:
                        Intent intent1 = new Intent(context, ActivityMessages.class);
                        startActivity(intent1);
                        return true;
                    case R.string.idWaypoints:
                        Intent intent = new Intent(context, ActivityWaypoints.class);
                        startActivity(intent);
                        return true;
                    case R.string.idSettings:
                        return true;
                    case R.string.idStatistics:
                        Intent intent2 = new Intent(context, ActivityStatistics.class);
                        startActivity(intent2);
                        return true;

                }
                return false;
            }
        };

        DrawerFactory.buildDrawer(this, toolbar, drawerListener, 2);

        getFragmentManager().beginTransaction().replace(R.id.content_frame, new FragmentPreferences(), "preferences").commit();

    }


    private void goToRoot() {
        Intent intent1 = new Intent(this, ActivityMain.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent1);
        finish();
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:     // If the user hits the toolbar back arrow, go back to ActivityMain, no matter where he came from (same as hitting back)
                goToRoot();
                return true;
            case R.id.connect:

                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        ServiceProxy.getServiceBroker().reconnect();
                    }
                };
                new Thread(r).start();
                goToRoot();
                return true;
            case R.id.disconnect:
                Runnable s = new Runnable() {

                    @Override
                    public void run() {
                        ServiceProxy.getServiceBroker().disconnect(true);

                    }
                };
                new Thread(s).start();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    // If the user hits back, go back to ActivityMain, no matter where he came from
    @Override
    public void onBackPressed() {
        goToRoot();
    }


    public static class FragmentPreferences extends PreferenceFragment {
        String[] modesReadable;

        private static org.owntracks.android.support.EditIntegerPreference locatorDisplacement;
        private static org.owntracks.android.support.EditIntegerPreference locatorInterval;

        private static Preference version;
        private static Preference repo;
        private static Preference twitter;
        private static Preference community;

        static String ver;



        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Activity a = getActivity();
            PackageManager pm = a.getPackageManager();
            modesReadable = getResources().getStringArray(R.array.profileIds_readable);

            Log.v(TAG, "Prepping preferences: " + Preferences.getModeId());

            if (Preferences.isModePrivate()) {
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_PRIVATE);
                addPreferencesFromResource(R.xml.preferences_private);
            } else if(Preferences.isModeHosted()) {
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_HOSTED);
                addPreferencesFromResource(R.xml.preferences_hosted);
            } else if(Preferences.isModePublic()){
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_PUBLIC);
                addPreferencesFromResource(R.xml.preferences_public);
            } else {
                throw new RuntimeException("Unknown application mode");
            }

            repo = findPreference("repo");
            twitter = findPreference("twitter");
            community = findPreference("community");
            version = findPreference("versionReadOnly");

            locatorDisplacement = (EditIntegerPreference) findPreference(Preferences.getKey(R.string.keyLocatorDisplacement));
            locatorDisplacement.setHint(Integer.toString(Preferences.getIntResource(R.integer.valLocatorDisplacement)));

            locatorInterval = (EditIntegerPreference) findPreference(Preferences.getKey(R.string.keyLocatorInterval));
            locatorInterval.setHint(Integer.toString(Preferences.getIntResource(R.integer.valLocatorInterval)));


            try { ver = pm.getPackageInfo(a.getPackageName(), 0).versionName; } catch (PackageManager.NameNotFoundException e) { ver = a.getString(R.string.na);}
            version.setSummary(ver);

            repo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(Preferences.getRepoUrl()));
                    a.startActivity(intent);
                    return false;
                }
            });


            twitter.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(Preferences.getTwitterUrl()));
                    a.startActivity(intent);
                    return false;
                }
            });

            community.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(Preferences.getSupportUrl()));
                    a.startActivity(intent);
                    return false;
                }
            });


            setServerPreferenceSummary(this);





            Preference.OnPreferenceClickListener connectionListener = new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.v(TAG, "startActivityForResult ActivityPreferencesConnection");
                    Intent intent = new Intent(getActivity(), ActivityPreferencesConnection.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    getActivity().startActivityForResult(intent, REQUEST_CODE_CONNECTION);
                    return true;
                }
            };

                // Fix toolbars for PreferenceScreens on demand
            Preference.OnPreferenceClickListener genericListener = new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final android.preference.Preference preference) {
                    if(!(preference instanceof PreferenceScreen))
                        return false;

                    final Dialog dialog = ((PreferenceScreen)preference).getDialog();

                    LinearLayout root = (LinearLayout) dialog.findViewById(android.R.id.list).getParent();
                    final Toolbar bar = (Toolbar) LayoutInflater.from(preference.getContext()).inflate(R.layout.toolbar, root, false);
                    root.addView(bar, 0); // insert at top

                    bar.setTitle(preference.getTitle());
                    bar.setNavigationOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    return false;
                }
            };
            findPreference("connectionScreen").setOnPreferenceClickListener(connectionListener);
            findPreference("reportingScreen").setOnPreferenceClickListener(genericListener);
            findPreference("notificationScreen").setOnPreferenceClickListener(genericListener);
            findPreference("advancedScreen").setOnPreferenceClickListener(genericListener);
            findPreference("informationScreen").setOnPreferenceClickListener(genericListener);
        }



        @Override
        public void onStart() {
            super.onStart();
            EventBus.getDefault().registerSticky(this);
        }

        @Override
        public void onStop() {
            EventBus.getDefault().unregister(this);
            super.onStop();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
            if ((e != null) && (e.getExtra() != null) && (e.getExtra() instanceof Exception)) {
                if ((((Exception) e.getExtra()).getCause() != null))
                    setServerPreferenceSummary(this, getResources().getString(R.string.error) + ": " + ((Exception) e.getExtra()).getCause().getLocalizedMessage());
                else
                    setServerPreferenceSummary(this, getResources().getString(R.string.error) + ": " + e.getExtra().toString());

            } else {
                setServerPreferenceSummary(this);
            }
        }

        private  void setServerPreferenceSummary(PreferenceFragment c) {
            setServerPreferenceSummary(c, ServiceBroker.getStateAsString(c.getActivity()));
        }

        private  void setServerPreferenceSummary(PreferenceFragment f, String s) {
            f.findPreference("connectionScreen").setSummary(modesReadable[Preferences.getModeId()] + " - "+ s);


            ((BaseAdapter) ((PreferenceScreen) f.findPreference("root")).getRootAdapter()).notifyDataSetChanged(); //Have to redraw the list to reflect summary change
        }

    }




}




