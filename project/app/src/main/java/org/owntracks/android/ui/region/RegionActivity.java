package org.owntracks.android.ui.region;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiRegionBinding;
import org.owntracks.android.ui.base.BaseActivity;

import androidx.annotation.Nullable;

public class RegionActivity extends BaseActivity<UiRegionBinding, RegionMvvm.ViewModel> implements RegionMvvm.View {

    private MenuItem saveButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasEventBus(false);
        bindAndAttachContentView(R.layout.ui_region, savedInstanceState);
        setSupportToolbar(binding.toolbar);

        Bundle b = navigator.getExtrasBundle(getIntent());
        if (b != null) {
            viewModel.loadWaypoint(b.getLong("waypointId", 0));
        }

        binding.description.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                conditionallyEnableSaveButton();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_waypoint, menu);
        this.saveButton = menu.findItem(R.id.save);
        conditionallyEnableSaveButton();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                viewModel.saveWaypoint();
                finish();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void conditionallyEnableSaveButton() {
        if (saveButton != null) {
            saveButton.setEnabled(viewModel.canSaveWaypoint());
            saveButton.getIcon().setAlpha(viewModel.canSaveWaypoint() ? 255 : 130);
        }
    }
}
