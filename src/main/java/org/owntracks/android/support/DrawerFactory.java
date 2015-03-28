package org.owntracks.android.support;


import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.owntracks.android.R;
import org.owntracks.android.activities.ActivityPreferences;

public class DrawerFactory {
    public static Drawer.Result buildDrawer(Activity activity, Toolbar toolbar, final Drawer.OnDrawerItemClickListener listener, int selection) {
        Drawer.Result d = new Drawer()
                .withActivity(activity)
                .withToolbar(toolbar)
                .withActionBarDrawerToggleAnimated(true)
                .withTranslucentStatusBar(true)
                .withDisplayBelowToolbar(true)
                .withDelayOnDrawerClose(-1)
                .withCloseOnClick(true)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Locations").withIdentifier(R.string.idLocations).withTag("loc").withIcon(activity.getResources().getDrawable(R.drawable.ic_locations)),
                        new PrimaryDrawerItem().withName("Waypoints").withIdentifier(R.string.idWaypoints).withTag("way").withIcon(activity.getResources().getDrawable(R.drawable.ic_waypoints)),
                        new PrimaryDrawerItem().withName("Preferences").withIdentifier(R.string.idSettings).withTag("set").withIcon(activity.getResources().getDrawable(R.drawable.ic_settings))
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public void onItemClick(final AdapterView<?> adapterView, final View view, final int i, final long l, final IDrawerItem iDrawerItem) {
                        if (iDrawerItem == null) // we don't need the onClick if the header was clicked
                            return;

                        // Wrap onClick in delayed runnable to give drawer time to close
                        new Handler().postDelayed(new Runnable() { // Give drawer time to close to prevent UI lag
                            @Override
                            public void run() {
                                listener.onItemClick(adapterView, view, i, l, iDrawerItem);
                            }
                        }, 200);
                    }
                })
                .withSelectedItem(selection)
                .build();
        d.getListView().setVerticalScrollBarEnabled(false);
        return d;
    }
}
