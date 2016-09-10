package org.owntracks.android.ui.base.navigator;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.owntracks.android.R;
import org.owntracks.android.activities.ActivityPreferences;
import org.owntracks.android.activities.ActivityRegions;
import org.owntracks.android.activities.ActivityStatus;
import org.owntracks.android.ui.base.BaseActivity;
import org.owntracks.android.ui.contacts.ContactsActivity;
import org.owntracks.android.ui.map.MapActivity;

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
public class ActivityNavigator extends BaseNavigator {
    private static final int COLOR_ICON_PRIMARY = R.color.md_light_primary_icon;
    private static final int COLOR_ICON_PRIMARY_ACTIVE = R.color.md_blue_600;
    private static final int COLOR_ICON_SECONDARY = R.color.md_light_secondary;
    private static final int COLOR_ICON_SECONDARY_ACTIVE = COLOR_ICON_PRIMARY_ACTIVE;

    private final AppCompatActivity activity;

    public ActivityNavigator(AppCompatActivity activity) {
        this.activity = activity;
    }

    @Override
    final AppCompatActivity getActivity() {
        return activity;
    }

    @Override
    final FragmentManager getChildFragmentManager() {
        throw new UnsupportedOperationException("Activities do not have a child fragment manager.");
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
                .withTextColorRes(R.color.md_black_1000)
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
                .withTextColorRes(R.color.md_black_1000)
                .withIdentifier(targetActivityClass.hashCode());

    }

    public Drawer attachDrawer(@NonNull Toolbar toolbar) {

        return new DrawerBuilder()
                .withActivity(activity)
                .withToolbar(toolbar)
                .withStickyFooterShadow(false)
                .withStickyFooterDivider(true)
                .addDrawerItems(
                        drawerItemForClass(activity, MapActivity.class, R.string.title_activity_map, R.drawable.ic_layers_black_24dp),
                        drawerItemForClass(activity, ContactsActivity.class, R.string.title_activity_contacts, R.drawable.ic_supervisor_account_black_24dp),
                        drawerItemForClass(activity, ActivityRegions.class, R.string.title_activity_regions, R.drawable.ic_adjust_black_24dp)


                ).addStickyDrawerItems(
                        secondaryDrawerItemForClass(activity, ActivityStatus.class, R.string.title_activity_status, R.drawable.ic_info_black_24dp),
                        secondaryDrawerItemForClass(activity, ActivityPreferences.class, R.string.title_activity_preferences, R.drawable.ic_settings_black_36dp)
                ).withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {

                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem == null)
                            return false;

                        Class<BaseActivity> targetclass = (Class<BaseActivity>) drawerItem.getTag();


                        if (activity.getClass() == targetclass) {
                            return false;
                        }

                        // Intent i = new Intent(activity, targetclass);
                        // i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        // activity.startActivity(i);
                        //activity.overridePendingTransition (R.anim.push_up_in,R.anim.hold);

                        startActivity(targetclass);
                        return false; // return false to enable withCloseOnClick
                    }
                }).withSelectedItem(activity.getClass().hashCode())
                .withCloseOnClick(true)
                .withDelayDrawerClickEvent(350)
                .withDelayOnDrawerClose(0)
                .build();
    }
}


