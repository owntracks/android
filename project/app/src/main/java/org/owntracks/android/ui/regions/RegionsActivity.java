package org.owntracks.android.ui.regions;

import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.owntracks.android.R;
import org.owntracks.android.data.WaypointModel;
import org.owntracks.android.databinding.UiRegionsBinding;
import org.owntracks.android.ui.base.BaseActivity;
import org.owntracks.android.ui.region.RegionActivity;

import io.objectbox.android.AndroidScheduler;
import io.objectbox.reactive.DataSubscription;
import timber.log.Timber;

public class RegionsActivity extends BaseActivity<UiRegionsBinding, RegionsMvvm.ViewModel> implements RegionsMvvm.View, RegionsAdapter.ClickListener {

    private RegionsAdapter recyclerViewAdapter;
    private DataSubscription subscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasEventBus(false);
        bindAndAttachContentView(R.layout.ui_regions, savedInstanceState);
        setSupportToolbar(binding.toolbar);
        setDrawer(binding.toolbar);

        recyclerViewAdapter = new RegionsAdapter(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(recyclerViewAdapter);
        binding.recyclerView.setEmptyView(binding.placeholder);

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
                navigator.startActivity(RegionActivity.class);
                return true;
            case R.id.exportWaypointsService:
                viewModel.exportWaypoints();
                return true;



        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onClick(@NonNull final WaypointModel model, @NonNull View view, boolean longClick) {
        if(longClick) {
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
        } else {
            Bundle b = new Bundle();
            b.putLong("waypointId", model.getTst());
            navigator.startActivity(RegionActivity.class, b);
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        if(this.subscription == null || this.subscription.isCanceled()) {
            this.subscription = viewModel.getWaypointsList().subscribe().on(AndroidScheduler.mainThread()).observer(recyclerViewAdapter) ;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        subscription.cancel();
    }
}
