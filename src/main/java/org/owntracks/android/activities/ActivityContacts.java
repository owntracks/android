package org.owntracks.android.activities;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.databinding.tool.Binding;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Explode;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;


import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.BR;

import org.owntracks.android.adapter.ContactsAdapter;
import org.owntracks.android.databinding.ActivityContactsBinding;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.services.ServiceLocator;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.DrawerFactory;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.RecyclerViewAdapter;
import org.owntracks.android.support.Toasts;

import de.greenrobot.event.EventBus;
import me.tatarka.bindingcollectionadapter.BindingRecyclerViewAdapter;
import me.tatarka.bindingcollectionadapter.ItemViewArg;
import me.tatarka.bindingcollectionadapter.factories.BindingRecyclerViewAdapterFactory;

public class ActivityContacts extends ActivityBase implements RecyclerViewAdapter.ClickHandler, RecyclerViewAdapter.LongClickHandler, BindingRecyclerViewAdapterFactory {
    private static final String TAG = "ActivityContacts";
    private static final int PERMISSION_REQUEST_SETUP_FOREGROUND_LOCATION_REQUEST = 1;
    private static final int PERMISSION_REQUEST_REPORT_LOCATION = 2;
    private static final int PERMISSION_REQUEST_SHOW_CURRENT_LOCATION = 3;
    private Toolbar toolbar;
    private Drawer drawer;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        ActivityContactsBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_contacts);
        binding.setVariable(BR.adapterFactory,this );
        binding.setViewModel(App.getContactsViewModel());

        // ((org.owntracks.android.support.RecyclerViewAdapter)binding.rvContacts.getAdapter()).setClickHandler(this);
       // ((org.owntracks.android.support.RecyclerViewAdapter)binding.rvContacts.getAdapter()).setLongClickHandler(this);


        // ActivityContactsBinding b = DataBindingUtil.setContentView(this, R.layout.activity_contacts);
       // b.setFusedContacts(App.getFusedContactsViewModel());
        //b.rvContacts.setLayoutManager(new LinearLayoutManager(this));

        toolbar =(Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getTitle());
        drawer = DrawerFactory.buildDrawerV2(this, toolbar, new DrawerFactory.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick() {
                drawer.closeDrawer();
                return false;
            }
        });


        //drawer = DrawerFactory.buildDrawer(this, toolbar, null, null, 0);
        //rvContacts = (RecyclerView) findViewById(R.id.rvContacts);

        //adapter = new ContactsAdapter(App.getFusedContacts());
        //b.rvContacts.setAdapter(adapter);


        //boolean pauseOnScroll = false; // or true
        //boolean pauseOnFling = true; // or false
        //PauseOnScrollListener listener = new PauseOnScrollListener(imageLoader, pauseOnScroll, pauseOnFling);
        //listView.setOnScrollListener(listener);


        runActionWithLocationPermissionCheck(PERMISSION_REQUEST_SETUP_FOREGROUND_LOCATION_REQUEST);


    }


    @Override
    public void onStart() {
        super.onStart();
        ServiceProxy.runOrBind(this, new Runnable() {
            @Override
            public void run() {
                ServiceProxy.getServiceNotification().clearNotificationMessages();
            }
        });
        EventBus.getDefault().registerSticky(this);
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fragment_contacts, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_report:
                runActionWithLocationPermissionCheck(PERMISSION_REQUEST_REPORT_LOCATION);
                return true;
            case R.id.menu_mylocation:
                runActionWithLocationPermissionCheck(PERMISSION_REQUEST_SHOW_CURRENT_LOCATION);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected  void onRunActionWithPermissionCheck(int action, boolean granted) {
        switch (action) {
            case PERMISSION_REQUEST_REPORT_LOCATION:
                Log.v(TAG, "request code: PERMISSION_REQUEST_REPORT_LOCATION " + granted);
                if (granted) {
                    ServiceProxy.runOrBind(this, new Runnable() {

                        @Override
                        public void run() {
                            if (ServiceProxy.getServiceLocator().getLastKnownLocation() == null)
                                Toasts.showCurrentLocationNotAvailable();
                            else
                                ServiceProxy.getServiceLocator().publishManualLocationMessage();
                        }
                    });
                } else {
                    Toasts.showLocationPermissionNotAvailable();
                }
                return;


            case PERMISSION_REQUEST_SHOW_CURRENT_LOCATION:
                Log.v(TAG, "request code: PERMISSION_REQUEST_SHOW_CURRENT_LOCATION");
                if (granted) {
                    Log.v(TAG, "PERMISSION_REQUEST_SHOW_CURRENT_LOCATION permission granted");
                    //TODO: zoom in on current location
                } else {
                    Toasts.showLocationPermissionNotAvailable();
                }
                return;
        }
    }



    @Override
    public void onResume() {

        super.onResume();
        Log.v(TAG, "restarting loader");
    }



    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }
    public void onEventMainThread(Events.FusedContactUpdated e){
        Log.v(TAG, "FusedContactUpdated. IDX: " + App.getFusedContacts().indexOfKey(e.getContact().getTopic()));
       // this.adapter.notifyItemInserted(App.getFusedContacts().indexOfKey(e.getContact().getTopic()));
      //  this.adapter.notifyDataSetChanged();
    }
    public void onEventMainThread(Events.FusedContactAdded e){
        Log.v(TAG, "FusedContactAdded. IDX: " + App.getFusedContacts().indexOfKey(e.getContact().getTopic()));

       // this.adapter.notifyItemChanged(App.getFusedContacts().indexOfKey(e.getContact().getTopic()));
      //  this.adapter.notifyDataSetChanged();

    }

    private static final int MENU_CONTACT_SHOW = 0;
    private static final int MENU_CONTACT_DETAILS = 1;
    private static final int MENU_CONTACT_NAVIGATE = 2;
    private static final int MENU_CONTACT_FOLLOW = 3;
    private static final int MENU_CONTACT_UNFOLLOW = 4;
    private static final int MENU_CONTACT_REQUEST_REPORT_LOCATION = 5;


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(Menu.NONE, MENU_CONTACT_SHOW, 1, R.string.menuContactShow);

            if(Preferences.getFollowingSelectedContact())
                menu.add(Menu.NONE, MENU_CONTACT_UNFOLLOW, 2, R.string.menuContactUnfollow);
            else
                menu.add(Menu.NONE, MENU_CONTACT_FOLLOW, 2, R.string.menuContactFollow);

            menu.add(Menu.NONE, MENU_CONTACT_DETAILS, 3, R.string.menuContactDetails);
            menu.add(Menu.NONE, MENU_CONTACT_NAVIGATE, 4, R.string.menuContactNavigate);
            menu.add(Menu.NONE, MENU_CONTACT_REQUEST_REPORT_LOCATION, 5, R.string.menuContactRequestReportLocation);


    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int index = info.position;
        View view = info.targetView;
        Log.v(TAG, "onContextItemSelected "  + view.getTag());
        return true;
    }


    @Override
    public void onClick(View v, Object viewModel) {
        Intent intent = new Intent(this, ActivityMap.class);
        Bundle b = new Bundle();
        b.putInt(ActivityMap.KEY_ACTION, ActivityMap.KEY_ACTION_VALUE_CENTER_CONTACT);
        b.putString(ActivityMap.KEY_TOPIC, ((FusedContact) viewModel).getTopic());
        intent.putExtras(b);

        //ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, v, "rowContact");


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            startActivity(intent);
//            startActivity(intent, options.toBundle());
        } else {
            startActivity(intent);
        }
    }

    @Override
    public void onLongClick(View v, Object viewModel) {
        registerForContextMenu(v);
        openContextMenu(v);
        unregisterForContextMenu(v);

    }

    @Override
    public <T> BindingRecyclerViewAdapter<T> create(RecyclerView recyclerView, ItemViewArg<T> arg) {
        return new RecyclerViewAdapter<>(this, this, arg);
    }
}
