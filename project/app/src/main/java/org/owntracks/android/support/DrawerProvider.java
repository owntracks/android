package org.owntracks.android.support;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondarySwitchDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.owntracks.android.R;
import org.owntracks.android.activities.ActivityPreferences;
import org.owntracks.android.activities.ActivityRegions;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.BaseActivity;
import org.owntracks.android.ui.contacts.ContactsActivity;
import org.owntracks.android.ui.map.MapActivity;
import org.owntracks.android.ui.status.StatusActivity;

import timber.log.Timber;

/* Copyright 2016 Patrick Löwenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
public class DrawerProvider implements Preferences.OnPreferenceChangedListener {
    private static final int COLOR_ICON_PRIMARY = R.color.md_light_primary_icon;
    private static final int COLOR_ICON_PRIMARY_ACTIVE = R.color.md_blue_600;
    private static final int COLOR_ICON_SECONDARY = R.color.md_light_secondary;
    private static final int COLOR_ICON_SECONDARY_ACTIVE = COLOR_ICON_PRIMARY_ACTIVE;

    private final AppCompatActivity activity;
    private final Preferences preferences;
    private SecondarySwitchDrawerItem switchDrawerItemPub;

    private SecondarySwitchDrawerItem switchDrawerItemCopy;
    private Drawer drawer;
    private boolean isBuilt;

    public DrawerProvider(AppCompatActivity activity, Preferences preferences) {
        this.preferences = preferences;
        this.activity = activity;

    }

    final AppCompatActivity getActivity() {
        return activity;
    }

    private PrimaryDrawerItem drawerItemForClass(AppCompatActivity activeActivity, Class<?> targetActivityClass, @StringRes int targetActivityTitleRessource, @DrawableRes int iconResource) {

        return new PrimaryDrawerItem()
                .withName(activeActivity.getString(targetActivityTitleRessource))
                .withSelectable(false)
                .withSelectedTextColorRes(COLOR_ICON_PRIMARY_ACTIVE)
                .withIcon(iconResource)
                .withIconColorRes(COLOR_ICON_PRIMARY)
                .withIconTintingEnabled(true)
                .withSelectedIconColorRes(COLOR_ICON_PRIMARY_ACTIVE)
                .withTag(targetActivityClass)
                .withIdentifier(targetActivityClass.hashCode());

    }
    private SecondaryDrawerItem secondaryDrawerItemForClass(AppCompatActivity activeActivity, Class<?> targetActivityClass, @StringRes int targetActivityTitleRessource, @DrawableRes int iconResource) {
        return new SecondaryDrawerItem()
                .withName(activeActivity.getString(targetActivityTitleRessource))
                .withIcon(iconResource  )
                .withIconColorRes(COLOR_ICON_SECONDARY)
                .withSelectedIconColorRes(COLOR_ICON_SECONDARY_ACTIVE)
                .withIconTintingEnabled(true)
                .withTag(targetActivityClass)
                .withSelectable(false)
                .withIdentifier(targetActivityClass.hashCode());

    }

    private SecondarySwitchDrawerItem switchDrawerItemPub() {
        Timber.v("build item, setting checked to: %s", preferences.getPub());

        OnCheckedChangeListener l = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked) {
                if(isBuilt) {
                    Timber.v("setting pub to: %s", isChecked);
                    preferences.setPub(isChecked);
                } else {
                    Timber.v("ignoring onCheckedChanged because drawer is not built yet");
                }
            }
        };
        return new SecondarySwitchDrawerItem()
                .withIdentifier(100001)
                .withName(R.string.drawerSwitchReporting)
                .withSelectable(false)
                .withCheckable(false)
                .withChecked(preferences.getPub())
                .withOnCheckedChangeListener(l)
                .withIcon(R.drawable.ic_report)
                .withIconTintingEnabled(true)
                .withIconColorRes(COLOR_ICON_SECONDARY);
    }

    private SecondarySwitchDrawerItem switchDrawerItemCopy() {
        OnCheckedChangeListener l = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked) {
                preferences.setCp(isChecked);
            }
        };
        return new SecondarySwitchDrawerItem()
                .withIdentifier(100002)
                .withName(R.string.drawerSwitchCopy)
                .withSelectable(false)
                .withCheckable(false)
                .withChecked(preferences.getCp())
                .withOnCheckedChangeListener(l)
                .withIconTintingEnabled(true)
                .withIcon(R.drawable.ic_layers_black_24dp)
                .withIconColorRes(COLOR_ICON_SECONDARY);

    }

    public void attach(@NonNull Toolbar toolbar) {
        switchDrawerItemPub = switchDrawerItemPub();
        switchDrawerItemCopy = switchDrawerItemCopy();


        this.drawer = new DrawerBuilder()
                .withActivity(activity)
                .withToolbar(toolbar)
                .withStickyFooterShadow(false)
                .withStickyFooterDivider(true)
                .addDrawerItems(
                        drawerItemForClass(activity, MapActivity.class, R.string.title_activity_map, R.drawable.ic_layers_black_24dp),
                        drawerItemForClass(activity, ContactsActivity.class, R.string.title_activity_contacts, R.drawable.ic_supervisor_account_black_24dp),
                        drawerItemForClass(activity, ActivityRegions.class, R.string.title_activity_regions, R.drawable.ic_adjust_black_24dp)


                ).addStickyDrawerItems(
                        switchDrawerItemPub,
                        switchDrawerItemCopy,
                        secondaryDrawerItemForClass(activity, StatusActivity.class, R.string.title_activity_status, R.drawable.ic_info_black_24dp),
                        secondaryDrawerItemForClass(activity, ActivityPreferences.class, R.string.title_activity_preferences, R.drawable.ic_settings_black_36dp)
                ).withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {

                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem == null)
                            return false;

                        if(drawerItem instanceof SecondarySwitchDrawerItem)
                            return true;

                        Class<BaseActivity> targetclass = (Class<BaseActivity>) drawerItem.getTag();


                        if (activity.getClass() == targetclass) {
                            return false;
                        }

                        startActivity(targetclass);

                        return false; // return false to enable withCloseOnClick
                    }
                }).withSelectedItem(activity.getClass().hashCode())
                //.withCloseOnClick(true)
               // .withDelayDrawerClickEvent(350)
                //.withDelayOnDrawerClose(0)
                .build();
        isBuilt = true;
    }


    public final void startActivity(@NonNull Class<? extends Activity> activityClass) {
        Activity activity = getActivity();
        Intent intent = new Intent(activity, activityClass);
        activity.startActivity(intent);
    }


    @Override
    public void onAttachAfterModeChanged() {
        switchDrawerItemPub.withSetSelected(preferences.getPub());
        switchDrawerItemPub.withSetSelected(preferences.getCp());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if(Preferences.Keys.PUB.equals(key) && switchDrawerItemPub != null && drawer != null) {
            switchDrawerItemPub.withChecked(preferences.getPub());
            drawer.updateStickyFooterItem(switchDrawerItemPub);
        } else if(Preferences.Keys.CP.equals(key) && switchDrawerItemCopy != null && drawer != null) {
            drawer.updateStickyFooterItem(switchDrawerItemCopy);
            switchDrawerItemCopy.withSetSelected(preferences.getCp());
        }
    }

    public void unregisterPreferencesChangeListener() {
        preferences.unregisterOnPreferenceChangedListener(this);

    }
    public void registerPreferencesChangeListener() {
        preferences.registerOnPreferenceChangedListener(this);
    }
}


