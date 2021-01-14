package org.owntracks.android.ui.preferences.load;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.Observable;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.R;
import org.owntracks.android.databinding.UiPreferencesLoadBinding;
import org.owntracks.android.support.Events;
import org.owntracks.android.ui.base.BaseActivity;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

import timber.log.Timber;

@SuppressLint("GoogleAppIndexingApiWarning")
public class LoadActivity extends BaseActivity<UiPreferencesLoadBinding, LoadMvvm.ViewModel> implements LoadMvvm.View {
    private static final int REQUEST_CODE = 1;
    public static final String FLAG_IN_APP = "INAPP";

    @Inject
    EventBus eventBus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindAndAttachContentView(R.layout.ui_preferences_load, savedInstanceState);

        setHasEventBus(false);
        setSupportToolbar(binding.toolbar, true, false);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title_activity_load);
        }
        binding.getVm().addOnPropertyChangedCallback(propertyChangedCallback);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setHasBack(false);
        handleIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                viewModel.saveConfiguration();
                return true;
            case R.id.close:
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void setHasBack(boolean hasBackArrow) {
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(hasBackArrow);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_load, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.close).setVisible(viewModel.getConfigurationImportStatus() != ImportStatus.LOADING);
        menu.findItem(R.id.save).setVisible(viewModel.getConfigurationImportStatus() == ImportStatus.SUCCESS);
        return true;
    }

    private final Observable.OnPropertyChangedCallback propertyChangedCallback = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable observable, int i) {
            invalidateOptionsMenu();
        }
    };

    private void handleIntent(@Nullable Intent intent) {
        if (intent == null) {
            Timber.e("no intent provided");
            return;
        }

        setHasBack(navigator.getExtrasBundle(getIntent()).getBoolean(FLAG_IN_APP, false));
        Timber.v("inApp %s", intent.getBooleanExtra(FLAG_IN_APP, false));


        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            Timber.v("uri: %s", uri);
            if (uri != null) {
                try {
                    viewModel.extractPreferences(new URI(uri.toString()));
                } catch (URISyntaxException e) {
                    Timber.e(e, "Error parsing intent URI");
                }
            }
        } else {
            Intent pickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
            pickerIntent.addCategory(Intent.CATEGORY_OPENABLE);
            pickerIntent.setType("*/*");

            try {
                Timber.v("loading picker");
                startActivityForResult(Intent.createChooser(pickerIntent, "Select a file"), LoadActivity.REQUEST_CODE);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No file explorer app found", Toast.LENGTH_SHORT).show();
            }

        }
    }

    // Return path from file picker
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
        Timber.v("RequestCode: %s resultCode: %s", requestCode, resultCode);
        if (requestCode == LoadActivity.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    InputStream stream = getContentResolver().openInputStream(resultIntent.getData());
                    byte[] output = new byte[stream.available()];
                    stream.read(output);
                    viewModel.extractPreferences(output);
                } catch (IOException e) {
                    Timber.e(e, "Error reading content");
                }
            } else {
                finish();
            }
        }
    }

    @Override
    public void showFinishDialog() {
        (new AlertDialog.Builder(this)
                .setTitle("Import successful")
                .setMessage("It is recommended to restart the app to apply all imported values")
                .setPositiveButton("Restart", (dialog, which) -> eventBus.post(new Events.RestartApp()))
                .setNegativeButton("Cancel", (dialog, which) -> finish())).show();
    }
}