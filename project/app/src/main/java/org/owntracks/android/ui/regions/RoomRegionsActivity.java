package org.owntracks.android.ui.regions;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.owntracks.android.R;
import org.owntracks.android.databinding.ArchUiRegionsBinding;
import org.owntracks.android.db.room.WaypointModel;
import org.owntracks.android.support.widgets.RecyclerView;
import org.owntracks.android.ui.base.BaseArchitectureActivity;

import java.util.ArrayList;

import timber.log.Timber;

public class RoomRegionsActivity extends BaseArchitectureActivity<ArchUiRegionsBinding, RoomRegionsViewModel> implements View.OnLongClickListener, View.OnClickListener {

    private RoomRecyclerviewAdapter recyclerViewAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        bindAndAttachContentView(R.layout.arch_ui_regions, savedInstanceState);
        setSupportToolbar(binding.toolbar);

        recyclerViewAdapter = new RoomRecyclerviewAdapter(new ArrayList<WaypointModel>(), this, this);
        RecyclerView.class.cast(findViewById(R.id.recyclerView)).setLayoutManager(new LinearLayoutManager(this));
        RecyclerView.class.cast(findViewById(R.id.recyclerView)).setAdapter(recyclerViewAdapter);
        RecyclerView.class.cast(findViewById(R.id.recyclerView)).setEmptyView(binding.placeholder);

        //viewModel = ViewModelProviders.of(this).get(RoomRegionsViewModel.class);
        viewModel.getWaypointsList().observe(RoomRegionsActivity.this, recyclerViewAdapter);
        viewModel.printId();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_waypoints, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                navigator.startActivity(RoomRegionActivity.class);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onLongClick(View v) {
        final WaypointModel model = (WaypointModel) v.getTag();
        Timber.v("model %s ", model.getDescription());

        //TODO: Refactor and make nicer
        AlertDialog myQuittingDialogBox =new AlertDialog.Builder(this)
                //set message, title, and icon
                .setTitle("Delete")
                .setMessage("Do you want to Delete")

                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        viewModel.delete(model);
                        dialog.dismiss();
                    }

                }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        myQuittingDialogBox.show();
        return true;
    }

    @Override
    public void onClick(View v) {
        WaypointModel model = (WaypointModel) v.getTag();
        Bundle b = new Bundle();
        b.putLong("waypointId", model.getId());
        navigator.startActivity(RoomRegionActivity.class, b);
    }
}
