package org.owntracks.android.support;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondarySwitchDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.owntracks.android.BuildConfig;
import org.owntracks.android.R;
import org.owntracks.android.activities.ActivityContacts;
import org.owntracks.android.activities.ActivityMain;
import org.owntracks.android.activities.ActivityMessages;
import org.owntracks.android.activities.ActivityPreferences;
import org.owntracks.android.activities.ActivityStatistics;
import org.owntracks.android.activities.ActivityWaypoints;

public class DrawerFactory {
    private static final String TAG = "DrawerFactory";
    public static final int IDENTIFIER_LOCATIONS = 1;
    public static final int IDENTIFIER_WAYPOINTS = 2;
    public static final int IDENTIFIER_MESSAGES = 3;
    public static final int IDENTIFIER_SETTINGS = 4;
    public static final int IDENTIFIER_STATISTICS = 5;

    private static final int COLOR_ICON_PRIMARY = R.color.md_light_primary_icon;
    private static final int COLOR_ICON_PRIMARY_ACTIVE = R.color.materialize_primary;
    private static final int COLOR_ICON_SECONDARY = R.color.md_light_secondary;
    private static final int COLOR_ICON_SECONDARY_ACTIVE = COLOR_ICON_PRIMARY_ACTIVE;

    public interface OnDrawerItemClickListener {
        boolean onItemClick();
    }
    public static Drawer buildDrawerV2(final Activity activity,Toolbar toolbar) {
        return buildDrawerV2(activity, toolbar, null);
    }

     public static Drawer buildDrawerV2(final Activity activity,Toolbar toolbar, final OnDrawerItemClickListener activityClickListener) {
        Drawer drawer = new DrawerBuilder()
                .withActivity(activity)
                .withToolbar(toolbar)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Locations").withIdentifier(IDENTIFIER_LOCATIONS)
                                .withIcon(R.drawable.ic_place_black_24dp)
                                .withIconColorRes(COLOR_ICON_PRIMARY)
                                .withSelectedIconColorRes(COLOR_ICON_PRIMARY_ACTIVE)
                                .withIconTintingEnabled(true),
                        new PrimaryDrawerItem().withName("Messages").withIdentifier(IDENTIFIER_MESSAGES)
                                .withIcon(R.drawable.ic_chat_bubble_outline_black_24dp)
                                .withIconColorRes(COLOR_ICON_PRIMARY)
                                .withSelectedIconColorRes(COLOR_ICON_PRIMARY_ACTIVE)
                                .withIconTintingEnabled(true),
                        new PrimaryDrawerItem().withName("Waypoints").withIdentifier(IDENTIFIER_WAYPOINTS)
                                .withIcon(R.drawable.ic_adjust_black_24dp)
                                .withIconColorRes(COLOR_ICON_PRIMARY)
                                .withSelectedIconColorRes(COLOR_ICON_PRIMARY_ACTIVE)
                                .withIconTintingEnabled(true)

                ).addStickyDrawerItems(
                        new SecondaryDrawerItem().withName("Preferences").withIdentifier(IDENTIFIER_SETTINGS)
                                .withIcon(R.drawable.ic_settings_black_36dp)
                                .withIconColorRes(COLOR_ICON_SECONDARY)
                                .withSelectedIconColorRes(COLOR_ICON_SECONDARY_ACTIVE)
                                .withIconTintingEnabled(true)

                        //new SecondarySwitchDrawerItem().withName("Reporting"),
                        //new SecondarySwitchDrawerItem().withName("Ranging")


                ).withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {

                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem == null)
                            return false;

                        Class targetClass;
                        switch (drawerItem.getIdentifier()) {
                            case IDENTIFIER_LOCATIONS:
                                targetClass = ActivityMain.class;
                                break;
                            case IDENTIFIER_MESSAGES:
                                targetClass = ActivityMessages.class;
                                break;
                            case IDENTIFIER_WAYPOINTS:
                                targetClass = ActivityWaypoints.class;
                                break;
                            case IDENTIFIER_SETTINGS:
                                targetClass = ActivityPreferences.class;
                                break;
                            case IDENTIFIER_STATISTICS:
                                targetClass = ActivityStatistics.class;
                                break;
                            default:
                                return false;
                        }

                        if (activityClickListener != null && (activity.getClass() == targetClass)) {
                            return activityClickListener.onItemClick();
                        }

                        Intent i = new Intent(activity, targetClass);
                        activity.startActivity(i);
                        return true;
                    }
                })
                //.withAccountHeader(new AccountHeaderBuilder()
                //                .withActivity(activity)
                //                .withHeaderBackground(R.drawable.drawer4)
                //                .build()
                .build();
        return drawer;

    }


    public static Drawer buildDrawer(Activity activity, Toolbar toolbar, final Drawer.OnDrawerItemClickListener listener, int selection) {
        return buildDrawer(activity, toolbar, listener, null, selection);
    }

    public static Drawer buildDrawer(Activity activity, Toolbar toolbar, final Drawer.OnDrawerItemClickListener itemClickListener, final Drawer.OnDrawerNavigationListener navigationListener, int selection) {
        return null;

    }
}