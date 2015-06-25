package org.owntracks.android.support;


import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.mikepenz.iconics.typeface.FontAwesome;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.owntracks.android.R;
import org.owntracks.android.activities.ActivityPreferences;

public class DrawerFactory {
    private static final String TAG = "DrawerFactory";

    public static Drawer buildDrawer(Activity activity, Toolbar toolbar, final Drawer.OnDrawerItemClickListener listener, int selection) {
        return buildDrawer(activity, toolbar, listener, null, selection);
    }

    public static Drawer buildDrawer(Activity activity, Toolbar toolbar, final Drawer.OnDrawerItemClickListener itemClickListener, final Drawer.OnDrawerNavigationListener navigationListener, int selection) {

    DrawerBuilder builder = new DrawerBuilder()
            .withActivity(activity)
            .withToolbar(toolbar)
            .withActionBarDrawerToggleAnimated(true)
            .withTranslucentStatusBar(true)
            .withDisplayBelowToolbar(true)
            .withDelayOnDrawerClose(-1)
            .withCloseOnClick(true)
            .withFooterDivider(false)
            .addDrawerItems(
                    //                    new DividerDrawerItem(),
                    new PrimaryDrawerItem().withName("Locations").withIdentifier(R.string.idLocations).withTag("loc").withIcon(FontAwesome.Icon.faw_map_marker),
                    new PrimaryDrawerItem().withName("Messages").withIdentifier(R.string.idPager).withTag("pag").withIcon(FontAwesome.Icon.faw_bell),
                    new PrimaryDrawerItem().withName("Waypoints").withIdentifier(R.string.idWaypoints).withTag("way").withIcon(FontAwesome.Icon.faw_street_view)
            )
            .addStickyDrawerItems(new SecondaryDrawerItem().withName("Statistics").withIdentifier(R.string.idStatistics).withTag("stat").withIcon(FontAwesome.Icon.faw_bar_chart))

            .addStickyDrawerItems(new SecondaryDrawerItem().withName("Preferences").withIdentifier(R.string.idSettings).withTag("set").withIcon(FontAwesome.Icon.faw_cog))
            .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                @Override
                public boolean onItemClick(final AdapterView<?> parent, final View view, final int position, final long id, final IDrawerItem drawerItem) {
                    if (drawerItem == null) // we don't need the onClick if the header was clicked
                        return false;

                    // Wrap onClick in delayed runnable to give drawer time to close
                    new Handler().postDelayed(new Runnable() { // Give drawer time to close to prevent UI lag
                        @Override
                        public void run() {
                            itemClickListener.onItemClick(parent, view, position, id, drawerItem);
                        }
                    }, 200);
                    return true;
                }
            })
            .withSelectedItem(selection);

        if(navigationListener != null)
            builder.withOnDrawerNavigationListener(navigationListener);
        Drawer d = builder.build();
        d.getListView().setVerticalScrollBarEnabled(false);
        return d;
    }
}