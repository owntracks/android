package org.owntracks.android.activities;


import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.mikepenz.materialdrawer.Drawer;


import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.R;

import org.owntracks.android.databinding.ActivityContactsBinding;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.DrawerProvider;
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

        toolbar = (Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getTitle());
        Drawer drawer = DrawerProvider.buildDrawer(this, toolbar);


        //drawer = DrawerProvider.buildDrawer(this, toolbar, null, null, 0);
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
    @SuppressWarnings("unused")
    public void onEventMainThread(Events.FusedContactUpdated e){
        Log.v(TAG, "FusedContactUpdated. IDX: " + App.getFusedContacts().indexOfKey(e.getContact().getTopic()));
       // this.adapter.notifyItemInserted(App.getFusedContacts().indexOfKey(e.getContact().getTopic()));
      //  this.adapter.notifyDataSetChanged();
    }
    @SuppressWarnings("unused")
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
    public void onClick(View v, Object viewModel) {
        Bundle b = new Bundle();
        b.putInt(ActivityMap.INTENT_KEY_ACTION, ActivityMap.ACTION_FOLLOW_CONTACT);
        b.putString(ActivityMap.INTENT_KEY_TOPIC, ((FusedContact) viewModel).getTopic());
        Log.v(TAG, "onClick. ActivityMap.INTENT_KEY_ACTION: " + ActivityMap.ACTION_FOLLOW_CONTACT);
        Log.v(TAG, "onClick. ActivityMap.INTENT_KEY_TOPIC: " + b.getString(ActivityMap.INTENT_KEY_TOPIC));

        transitionToActivityMap(b);
    }


    private void transitionToActivityMap(Bundle extras) {
        Intent intent = new Intent(this, ActivityMap.class);
        intent.putExtras(extras);
       // intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition (R.anim.push_up_in,R.anim.push_down_out);

    }


    @Override
    public void onLongClick(View v, Object viewModel) {

   }

    @Override
    public <T> BindingRecyclerViewAdapter<T> create(RecyclerView recyclerView, ItemViewArg<T> arg) {
        return new RecyclerViewAdapter<>(this, this, arg);
    }
}
