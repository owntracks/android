package org.owntracks.android.ui.preferences.load;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Observer;

import com.fasterxml.jackson.core.JsonParseException;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.R;
import org.owntracks.android.databinding.UiPreferencesLoadBinding;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Parser;
import org.owntracks.android.ui.base.BaseActivity;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import javax.inject.Inject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

@SuppressLint("GoogleAppIndexingApiWarning")
public class LoadActivity extends BaseActivity<UiPreferencesLoadBinding, LoadMvvm.ViewModel> implements LoadMvvm.View {
    private static final int REQUEST_CODE = 1;
    public static final String FLAG_IN_APP = "INAPP";
    private MenuItem saveButton;

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

        viewModel.formattedEffectiveConfiguration().observe(this, (Observer<String>) configuration -> {
            ((TextView)findViewById(R.id.effectiveConfiguration)).setText(configuration);
        });

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

        saveButton = menu.findItem(R.id.save);
        viewModel.hasConfiguration().observe(this, (Observer<Boolean>) aBoolean -> {
            saveButton.setEnabled(aBoolean);
            saveButton.setVisible(aBoolean);
        });
        return true;
    }

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
                extractPreferences(uri);
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
        Timber.v("RequestCode: " + requestCode + " resultCode: " + resultCode);
        if (requestCode == LoadActivity.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                extractPreferences(resultIntent.getData());
            } else {
                finish();
            }
        }
    }

    private void extractPreferences(Uri uri) {
        try {
            BufferedReader r;
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                // Note: left here to avoid breaking compatibility.  May be removed
                // with sufficient testing. Will not work on Android >5 without granting READ_EXTERNAL_STORAGE permission
                Timber.v("using file:// uri");
                r = new BufferedReader(new InputStreamReader(new FileInputStream(uri.getPath())));
            } else if ("owntracks".equals(uri.getScheme()) && "/config".equals(uri.getPath())) {
                Timber.v("Importing config using owntracks: scheme");

                List<String> urlQueryParam = uri.getQueryParameters("url");
                List<String> configQueryParam = uri.getQueryParameters("inline");
                if (configQueryParam.size() == 1) {
                    byte[] config = Base64.decode(configQueryParam.get(0), Base64.DEFAULT);
                    r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(config)));
                } else if (urlQueryParam.size() == 1) {
                    URL remoteConfigUrl = new URL(urlQueryParam.get(0));
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(remoteConfigUrl)
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            importFailureException(new Exception("Failure fetching config from remote URL", e), getString(R.string.errorPreferencesImportFailed));
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            try (ResponseBody responseBody = response.body()) {
                                if (!response.isSuccessful()) {
                                    importFailureException(new IOException(String.format("Unexpected status code: %s", response)), getString(R.string.errorPreferencesImportFailed));
                                    return;
                                }

                                viewModel.setConfiguration(responseBody.string());
                            } catch (Parser.EncryptionException e) {
                                importFailureException(e, getString(R.string.errorPreferencesImportFailed));
                            }
                        }
                    });
                    // This is async, so result handled on the callback
                    return;
                } else {
                    throw new IOException("Invalid config URL");
                }
            } else if ("content".equals(uri.getScheme())) {
                Timber.v("using content:// uri");
                InputStream stream = getContentResolver().openInputStream(uri);
                r = new BufferedReader(new InputStreamReader(stream));
            } else {
                throw new IOException("Invalid config URL");
            }

            StringBuilder total = new StringBuilder();

            String content;
            while ((content = r.readLine()) != null) {
                total.append(content);
            }

            viewModel.setConfiguration(total.toString());
        } catch (JsonParseException e) {
            importFailureException(e, getString(R.string.errorPreferencesImportFailedParseException));
        } catch (OutOfMemoryError e) {
            importFailureException(e, getString(R.string.errorPreferencesImportFailedMemory));
        } catch (Exception e) {
            importFailureException(e, getString(R.string.errorPreferencesImportFailed));
        }
    }

    private void importFailureException(Throwable throwable, String toastContent) {
        Timber.e(throwable);
        finish();
        Toast.makeText(this, toastContent, Toast.LENGTH_SHORT).show();
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