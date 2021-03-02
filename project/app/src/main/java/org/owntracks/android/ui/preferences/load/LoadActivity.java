package org.owntracks.android.ui.preferences.load;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
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
import org.owntracks.android.ui.base.navigator.Navigator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

import timber.log.Timber;

@SuppressLint("GoogleAppIndexingApiWarning")
public class LoadActivity extends BaseActivity<UiPreferencesLoadBinding, LoadMvvm.ViewModel<LoadMvvm.View>> implements LoadMvvm.View {
    private static final int REQUEST_CODE = 1;
    public static final String FLAG_IN_APP = "INAPP";

    @Inject
    Navigator navigator;

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
        int itemId = item.getItemId();
        if (itemId == R.id.save) {
            viewModel.saveConfiguration();
            return true;
        } else if (itemId == R.id.close || itemId == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);

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
            if (uri != null) {
                Timber.v("uri: %s", uri);
                if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                    try {
                        viewModel.extractPreferences(getContentFromURI(uri));
                    } catch (IOException e) {
                        Timber.e(e, "URI extract failure: %s", uri);
                        viewModel.setError(new Exception(String.format("Could not extract content from %s", uri), e));
                    }
                } else {
                    try {
                        viewModel.extractPreferences(new URI(uri.toString()));
                    } catch (URISyntaxException e) {
                        String msg = "Error parsing intent URI";
                        viewModel.setError(new Exception(msg, e));
                        Timber.e(e, msg);
                    }

                }
            } else {
                String msg = "No URI given for importing configuration";
                viewModel.setError(new Exception(msg));
                Timber.e(msg);
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
                byte[] content = new byte[0];
                try {
                    content = getContentFromURI(resultIntent.getData());
                } catch (IOException e) {
                    Timber.e(e, "Could not extract content from %s", resultIntent);
                }
                viewModel.extractPreferences(content);
            } else {
                finish();
            }
        }
    }

    private byte[] getContentFromURI(Uri uri) throws IOException {
        InputStream stream = getContentResolver().openInputStream(uri);
        byte[] output = new byte[stream.available()];
        int bytesRead = stream.read(output);
        Timber.d("Read %d bytes from content URI", bytesRead);
        return output;
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