package org.owntracks.android.support;


import android.app.Activity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.owntracks.android.R;
import org.owntracks.android.activities.ActivityBase;
import org.owntracks.android.activities.ActivityContacts;
import org.owntracks.android.activities.ActivityFeatured;
import org.owntracks.android.activities.ActivityMap;
import org.owntracks.android.activities.ActivityPreferences;
import org.owntracks.android.activities.ActivityStatus;
import org.owntracks.android.activities.ActivityRegions;

public class DrawerProvider {
    private static final String TAG = "DrawerProvider";

    private static final int COLOR_ICON_PRIMARY = R.color.md_light_primary_icon;
    private static final int COLOR_ICON_PRIMARY_ACTIVE = R.color.materialize_primary;
    private static final int COLOR_ICON_SECONDARY = R.color.md_light_secondary;
    private static final int COLOR_ICON_SECONDARY_ACTIVE = COLOR_ICON_PRIMARY_ACTIVE;



    public interface OnDrawerItemClickListener {
        boolean onItemClick();
    }

    public static Drawer buildDrawer(final ActivityBase activity, Toolbar toolbar) {
        return buildDrawer(activity, toolbar, null);
    }

    private static PrimaryDrawerItem drawerItemForClass(ActivityBase activeActivity, Class<?> targetActivityClass, int targetActivityTitleRessource) {

        return new PrimaryDrawerItem()
                .withName(activeActivity.getString(targetActivityTitleRessource))
                .withSelectable(false)
                .withIcon(R.drawable.ic_place_black_24dp)
                .withIconColorRes(COLOR_ICON_PRIMARY)
                .withIconTintingEnabled(true)
                .withSelectedIconColorRes(COLOR_ICON_PRIMARY_ACTIVE)
                .withTag(targetActivityClass)
                .withIdentifier(targetActivityClass.hashCode());

    }
    private static SecondaryDrawerItem secondaryDrawerItemForClass(Activity activeActivity, Class<?> targetActivityClass, int targetActivityTitleRessource) {
        return new SecondaryDrawerItem()
                .withName(activeActivity.getString(targetActivityTitleRessource))
                .withIcon(R.drawable.ic_settings_black_36dp)
                .withIconColorRes(COLOR_ICON_SECONDARY)
                .withSelectedIconColorRes(COLOR_ICON_SECONDARY_ACTIVE)
                .withIconTintingEnabled(true)
                .withTag(targetActivityClass)
                .withSelectable(false)
                .withIdentifier(targetActivityClass.hashCode());

    }



     public static Drawer buildDrawer(final ActivityBase activity, Toolbar toolbar, final OnDrawerItemClickListener activityClickListener) {
         Log.v(TAG, "buildDrawer for class: " +activity);
         Log.v(TAG, "active class identifier: " + activity.getClass().hashCode());

         return new DrawerBuilder()
                .withActivity(activity)
                .withToolbar(toolbar)
                .addDrawerItems(
                        drawerItemForClass(activity, ActivityMap.class, R.string.title_activity_map),
                        drawerItemForClass(activity, ActivityContacts.class, R.string.title_activity_contacts),
                        drawerItemForClass(activity, ActivityFeatured.class, R.string.title_activity_featured),
                        drawerItemForClass(activity, ActivityRegions.class, R.string.title_activity_regions)


                ).addStickyDrawerItems(
                        secondaryDrawerItemForClass(activity, ActivityStatus.class, R.string.title_activity_status),
                        secondaryDrawerItemForClass(activity, ActivityPreferences.class, R.string.title_activity_preferences)
                ).withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {

                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem == null)
                            return false;

                        Class<ActivityBase> targetclass = (Class<ActivityBase>) drawerItem.getTag();

                        if (activityClickListener != null && (activity.getClass() == targetclass)) {
                            activityClickListener.onItemClick();
                            return false;
                        }

                       // Intent i = new Intent(activity, targetclass);
                        // i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        // activity.startActivity(i);
                        //activity.overridePendingTransition (R.anim.push_up_in,R.anim.hold);
                        ActivityBase.launchActivityFromDrawer(activity, targetclass);
                        return false; // return false to enable withCloseOnClick
                    }
                }).withSelectedItem(activity.getClass().hashCode())
                 .withCloseOnClick(true)
                 .withDelayOnDrawerClose(200)
                .build();


    }

}