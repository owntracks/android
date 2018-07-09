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
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondarySwitchDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.owntracks.android.R;
import org.owntracks.android.ui.base.BaseActivity;
import org.owntracks.android.ui.contacts.ContactsActivity;
import org.owntracks.android.ui.map.MapActivity;
import org.owntracks.android.ui.preferences.PreferencesActivity;
import org.owntracks.android.ui.regions.RegionsActivity;
import org.owntracks.android.ui.status.StatusActivity;

import timber.log.Timber;

public class DrawerProvider  {
    private static final int COLOR_ICON_PRIMARY = R.color.md_light_primary_icon;
    private static final int COLOR_ICON_PRIMARY_ACTIVE = R.color.md_blue_600;
    private static final int COLOR_ICON_SECONDARY = R.color.md_light_secondary;
    private static final int COLOR_ICON_SECONDARY_ACTIVE = COLOR_ICON_PRIMARY_ACTIVE;

    private final AppCompatActivity activity;

    private Drawer drawer;
    private boolean isBuilt;

    public DrawerProvider(AppCompatActivity activity) {
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

    public void attach(@NonNull Toolbar toolbar) {

        this.drawer = new DrawerBuilder()
                .withActivity(activity)
                .withToolbar(toolbar)
                .withStickyFooterShadow(false)
                .withStickyFooterDivider(true)
                .addDrawerItems(
                        drawerItemForClass(activity, MapActivity.class, R.string.title_activity_map, R.drawable.ic_layers_black_24dp),
                        drawerItemForClass(activity, ContactsActivity.class, R.string.title_activity_contacts, R.drawable.ic_supervisor_account_black_24dp),
                        drawerItemForClass(activity, RegionsActivity.class, R.string.title_activity_regions, R.drawable.ic_adjust_black_24dp)


                ).addStickyDrawerItems(
                        secondaryDrawerItemForClass(activity, StatusActivity.class, R.string.title_activity_status, R.drawable.ic_info_black_24dp),
                        secondaryDrawerItemForClass(activity, PreferencesActivity.class, R.string.title_activity_preferences, R.drawable.ic_settings_black_36dp)
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

}


