package org.owntracks.android.support;


import android.app.Activity;
import android.support.v7.widget.Toolbar;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;

import org.owntracks.android.R;

public class DrawerFactory {
    public static Drawer.Result buildDrawer(Activity activity, Toolbar toolbar, Drawer.OnDrawerItemClickListener listener, boolean hamburger, int selection) {
        Drawer.Result d = new Drawer()
                .withActivity(activity)
                .withToolbar(toolbar)
                .withHeaderDivider(false)
                .withHeader(R.layout.drawer_header)
                .withActionBarCompatibility(false)
                .withActionBarDrawerToggle(hamburger)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Locations").withIdentifier(R.string.idLocations).withIcon(activity.getResources().getDrawable(R.drawable.ic_locations)),
                        new PrimaryDrawerItem().withName("Waypoints").withIdentifier(R.string.idWaypoints).withIcon(activity.getResources().getDrawable(R.drawable.ic_waypoints)),
                        new PrimaryDrawerItem().withName("Preferences").withIdentifier(R.string.idSettings).withIcon(activity.getResources().getDrawable(R.drawable.ic_settings))
                )
                .withOnDrawerItemClickListener(listener)
                .withSelectedItem(selection)
                .build();
        d.getListView().setVerticalScrollBarEnabled(false);
        return d;
    }
}
