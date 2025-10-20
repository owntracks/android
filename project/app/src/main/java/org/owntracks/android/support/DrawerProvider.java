package org.owntracks.android.support;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondarySwitchDrawerItem;

import org.owntracks.android.R;
import org.owntracks.android.services.BackgroundService;
import org.owntracks.android.services.worker.Scheduler;
import org.owntracks.android.ui.contacts.ContactsActivity;
import org.owntracks.android.ui.map.MapActivity;
import org.owntracks.android.ui.preferences.PreferencesActivity;
import org.owntracks.android.ui.preferences.about.AboutActivity;
import org.owntracks.android.ui.waypoints.WaypointsActivity;
import org.owntracks.android.ui.status.StatusActivity;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ActivityContext;
import dagger.hilt.android.scopes.ActivityScoped;

import static android.os.Process.killProcess;
import static android.os.Process.myPid;

@ActivityScoped
public class DrawerProvider {
    private static final int EXIT_OPERATION_ID = 88296;

    private final AppCompatActivity activity;
    private final Scheduler scheduler;

    @Inject
    public DrawerProvider(@ActivityContext Context activity, Scheduler scheduler) {
        this.activity = (AppCompatActivity) activity;
        this.scheduler = scheduler;
    }

    private Activity getActivity() {
        return activity;
    }

    private PrimaryDrawerItem drawerItemForClass(AppCompatActivity activeActivity, Class<?> targetActivityClass, @StringRes int targetActivityTitleResource, @DrawableRes int iconResource) {

        return new PrimaryDrawerItem()
                .withName(activeActivity.getString(targetActivityTitleResource))
                .withSelectable(false)
                .withIcon(iconResource)
                .withIconTintingEnabled(true)
                .withTag(targetActivityClass)
                .withIdentifier(targetActivityClass.hashCode());
    }

    private SecondaryDrawerItem secondaryDrawerItemForClass(AppCompatActivity activeActivity, Class<?> targetActivityClass, @StringRes int targetActivityTitleResource, @DrawableRes int iconResource) {
        SecondaryDrawerItem sdi = new SecondaryDrawerItem()
                .withName(activeActivity.getString(targetActivityTitleResource))
                .withIcon(iconResource)
                .withIconTintingEnabled(true)
                .withSelectable(false);
        // The exit operation has no target class
        if (targetActivityClass != null) {
            sdi.withTag(targetActivityClass);
            sdi.withIdentifier(targetActivityClass.hashCode());
        } else {
            sdi.withIdentifier(EXIT_OPERATION_ID);
        }

        return sdi;

    }

    public void attach(@NonNull Toolbar toolbar) {

        new DrawerBuilder()
                .withActivity(activity)
                .withToolbar(toolbar)
                .withStickyFooterShadow(false)
                .withStickyFooterDivider(true)
                .withFullscreen(true)
                .addDrawerItems(
                        drawerItemForClass(activity, MapActivity.class, R.string.title_activity_map, R.drawable.ic_baseline_layers_24),
                        drawerItemForClass(activity, ContactsActivity.class, R.string.title_activity_contacts, R.drawable.ic_baseline_supervisor_account_24),
                        drawerItemForClass(activity, WaypointsActivity.class, R.string.title_activity_waypoints, R.drawable.ic_baseline_adjust_24))
                .addStickyDrawerItems(
                        secondaryDrawerItemForClass(activity, StatusActivity.class, R.string.title_activity_status, R.drawable.ic_baseline_beenhere_24),
                        secondaryDrawerItemForClass(activity, PreferencesActivity.class, R.string.title_activity_preferences, R.drawable.ic_baseline_settings_24),
                        secondaryDrawerItemForClass(activity, AboutActivity.class, R.string.title_activity_about, R.drawable.ic_baseline_info_24),
                        secondaryDrawerItemForClass(activity, null, R.string.title_exit, R.drawable.ic_baseline_power_settings_new_24)
                )
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    if (drawerItem == null)
                        return false;

                    if (drawerItem instanceof SecondarySwitchDrawerItem)
                        return true;

                    // Finish when exit app drawer option selected
                    if (drawerItem.getIdentifier() == EXIT_OPERATION_ID) {
                        // Stop the background service
                        activity.stopService((new Intent(activity, BackgroundService.class)));
                        // Finish the activity
                        activity.finishAffinity();
                        // Kill scheduled tasks
                        scheduler.cancelAllTasks();
                        killProcess(myPid());
                        return true;
                    }

                    Class<AppCompatActivity> targetclass = (Class<AppCompatActivity>) drawerItem.getTag();

                    if (activity.getClass() == targetclass) {
                        return false;
                    }

                    startActivity(targetclass);

                    return false; // return false to enable withCloseOnClick
                })
                .build();
    }

    private void startActivity(@NonNull Class<? extends Activity> activityClass) {
        Context activity = getActivity();
        Intent intent = new Intent(activity, activityClass);
        activity.startActivity(intent);
    }
}
