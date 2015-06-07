package org.owntracks.android.support;

import android.app.Activity;
import android.content.Context;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.R;
import org.owntracks.android.services.ServiceBroker;

import de.greenrobot.event.EventBus;

// This is a prefrence that fakes a toolbar for a preference screen until the Android Support library supports toolbars in preferences_private screens
public class ConnectionToolbarPreference extends ToolbarPreference {
    MenuItem connect;
    MenuItem disconnect;

    public ConnectionToolbarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View v = super.onCreateView(parent);

        final Activity activity = (Activity) getContext();


        toolbar.inflateMenu(R.menu.preferences_connection);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                goUp();
                return activity.onOptionsItemSelected(menuItem);
            }
        });

        connect = toolbar.getMenu().findItem(R.id.connect);
        disconnect = toolbar.getMenu().findItem(R.id.disconnect);

        conditionallyEnableConnectButton();

        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().registerSticky(this);

        return v;
    }

    @Override
    public void goUp () {
        super.goUp();
        if(EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    protected void onPrepareForRemoval () {
        if(EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        super.onPrepareForRemoval();
    }



    public void conditionallyEnableConnectButton(){
        Log.v(this.toString(), "conditionallyEnableConnectButton");
        if(Preferences.canConnect())
            enableConnectButton();
        else
            disableConnectButton();

    }

    public void enableConnectButton() {
        connect.setEnabled(true);
        connect.getIcon().setAlpha(255);
    }

    public void disableConnectButton() {
        connect.setEnabled(false);
        connect.getIcon().setAlpha(130);
    }


    public void enableDisconnectButton() {
        disconnect.setEnabled(true);
        disconnect.getIcon().setAlpha(255);
    }

    public void disableDisconnectButton() {
        disconnect.setEnabled(false);
        disconnect.getIcon().setAlpha(130);
    }

    public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
        if(e.getState() == ServiceBroker.State.CONNECTED) {
            enableDisconnectButton();
        } else {
            disableDisconnectButton();
        }
    }

}
