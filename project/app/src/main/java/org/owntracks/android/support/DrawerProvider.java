package org.owntracks.android.support;


import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
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
import org.owntracks.android.activities.ActivityPreferences;
import org.owntracks.android.activities.ActivityStatus;
import org.owntracks.android.activities.ActivityRegions;
import org.owntracks.android.ui.contacts.ContactsActivity;
import org.owntracks.android.ui.map.MapActivity;

@Deprecated
public class DrawerProvider {
    private static final String TAG = "DrawerProvider";

    private static final int COLOR_ICON_PRIMARY = R.color.md_light_primary_icon;
    private static final int COLOR_ICON_PRIMARY_ACTIVE = R.color.md_blue_600;
    private static final int COLOR_ICON_SECONDARY = R.color.md_light_secondary;
    private static final int COLOR_ICON_SECONDARY_ACTIVE = COLOR_ICON_PRIMARY_ACTIVE;



    public interface OnDrawerItemClickListener {
        void onItemClick();
    }

    public static Drawer buildDrawer(final ActivityBase activity, Toolbar toolbar) {
        return buildDrawer(activity, toolbar, null);
    }

    private static PrimaryDrawerItem drawerItemForClass(ActivityBase activeActivity, Class<?> targetActivityClass, @StringRes int targetActivityTitleRessource, @DrawableRes int iconResource) {

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
    private static SecondaryDrawerItem secondaryDrawerItemForClass(ActivityBase activeActivity, Class<?> targetActivityClass, @StringRes int targetActivityTitleRessource, @DrawableRes int iconResource) {
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



     public static Drawer buildDrawer(final ActivityBase activity, Toolbar toolbar, final OnDrawerItemClickListener activityClickListener) {
         Log.v(TAG, "buildDrawer for class: " +activity);
         Log.v(TAG, "active class identifier: " + activity.getClass().hashCode());

         return new DrawerBuilder()
                .withActivity(activity)
                .withToolbar(toolbar)
                 .withStickyFooterShadow(false)
                 .withStickyFooterDivider(true)
                .addDrawerItems(
                        drawerItemForClass(activity, MapActivity.class, R.string.title_activity_map, R.drawable.ic_layers_black_24dp),
                        drawerItemForClass(activity, ContactsActivity.class, R.string.title_activity_contacts, R.drawable.ic_supervisor_account_black_24dp),
                      //  TODO: drawerItemForClass(activity, ActivityFeatured.class, R.string.title_activity_featured, R.drawable.ic_info_black_24dp)
                        drawerItemForClass(activity, ActivityRegions.class, R.string.title_activity_regions, R.drawable.ic_adjust_black_24dp)


                ).addStickyDrawerItems(
                        secondaryDrawerItemForClass(activity, ActivityStatus.class, R.string.title_activity_status, R.drawable.ic_info_black_24dp),
                        secondaryDrawerItemForClass(activity, ActivityPreferences.class, R.string.title_activity_preferences, R.drawable.ic_settings_black_36dp)
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