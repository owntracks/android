package org.owntracks.android.ui.configuration;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiActivityConfigurationBinding;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.BaseActivity;
import org.owntracks.android.ui.load.LoadActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigurationActivity extends BaseActivity<UiActivityConfigurationBinding, ConfigurationMvvm.ViewModel> implements ConfigurationMvvm.View {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        setAndBindContentView(R.layout.ui_activity_configuration, savedInstanceState);

        setHasEventBus(false);
        setSupportToolbar(binding.toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_configuration, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.exportConfigurationFile:
                viewModel.onExportConfigurationToFileClicked();
                return true;
            case R.id.importConfigurationFile:
                showImportConfigurationFilePickerView();
                return true;
            case R.id.importConfigurationSingleValue:
                showEditorView();
                return true;
            default:
                return false;
        }
    }

    private void showImportConfigurationFilePickerView() {
        Intent intent = new Intent(this, LoadActivity.class);
        intent.putExtra(LoadActivity.FLAG_IN_APP, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(BaseActivity.FLAG_DISABLES_ANIMATION, true);
        startActivity(intent);
    }

    private void showEditorView() {
        MaterialDialog d = new MaterialDialog.Builder(this)
                .customView(R.layout.ui_activity_configuration_single_value, true)
                .title(R.string.preferencesEditor)
                .positiveText(R.string.accept)
                .negativeText(R.string.cancel)
                .autoDismiss(false)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        final MaterialAutoCompleteTextView inputKey = (MaterialAutoCompleteTextView) dialog.findViewById(R.id.inputKey);
                        final MaterialEditText inputValue = (MaterialEditText) dialog.findViewById(R.id.inputValue);

                        String key = inputKey.getText().toString();
                        String value = inputValue.getText().toString();

                        try {
                            Preferences.importKeyValue(key, value);
                            viewModel.onPreferencesValueForKeySetSuccessful();
                            dialog.dismiss();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                            displayPreferencesValueForKeySetFailedKey();

                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                            displayPreferencesValueForKeySetFailedValue();
                        }

                    }
                }).show();
        MaterialAutoCompleteTextView view = (MaterialAutoCompleteTextView) d.getCustomView().findViewById(R.id.inputKey);
        view.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, Preferences.getImportKeys()));
    }

    @Override
    public boolean exportConfigurationToFile(String exportStr) {
        File cDir = getBaseContext().getCacheDir();
        File tempFile = new File(cDir.getPath() + "/config.otrc") ;

        try {
            FileWriter writer = new FileWriter(tempFile);
            writer.write(exportStr);
            writer.close();
        } catch (IOException e) {
            displayExportToFileFailed();
            return false;
        }
        Uri configUri = FileProvider.getUriForFile(this, "org.owntracks.android.fileprovider", tempFile);

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, configUri);
        sendIntent.setType("text/plain");

        startActivity(Intent.createChooser(sendIntent, getString(R.string.exportConfiguration)));

        return true;
    }


    @Override
    public void displayLoadFailed() {
        Toast.makeText(this, R.string.preferencesLoadFailed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void displayExportToFileFailed() {
        Toast.makeText(this, R.string.preferencesExportFailed, Toast.LENGTH_SHORT).show();
    }

    public void displayExportToFileSuccessful() {
        Toast.makeText(this, R.string.preferencesExportSuccess, Toast.LENGTH_SHORT).show();
    }

    public void displayPreferencesValueForKeySetFailedKey() {
        Toast.makeText(this, R.string.preferencesEditorKeyError, Toast.LENGTH_SHORT).show();
    }
    public void displayPreferencesValueForKeySetFailedValue() {
        Toast.makeText(this, R.string.preferencesEditorValueError, Toast.LENGTH_SHORT).show();
    }
}