package org.owntracks.android.ui.preferences.editor;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.databinding.UiPreferencesEditorBinding;
import org.owntracks.android.messages.MessageConfiguration;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.BaseActivity;
import org.owntracks.android.ui.preferences.load.LoadActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;

import javax.inject.Inject;

public class EditorActivity extends BaseActivity<UiPreferencesEditorBinding, EditorMvvm.ViewModel> implements EditorMvvm.View {
    @Inject
    Preferences preferences;

    @Inject
    WaypointsRepo waypointsRepo;

    @Inject
    Parser parser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindAndAttachContentView(R.layout.ui_preferences_editor, savedInstanceState);

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
                new ExportTask(this).execute();
                return true;
            case R.id.importConfigurationFile:
                showImportConfigurationFilePickerView();
                return true;
            case R.id.importConfigurationSingleValue:
                showEditorView();
                return true;
            case R.id.restart:
                App.restart();
            default:
                return false;
        }
    }

    private void showImportConfigurationFilePickerView() {
        Bundle b = new Bundle();
        b.putBoolean(LoadActivity.FLAG_IN_APP, true);
        navigator.startActivity(LoadActivity.class, b);
    }

    private void showEditorView() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        final View layout = inflater.inflate(R.layout.ui_preferences_editor_dialog,null);

        builder.setTitle(R.string.preferencesEditor)
                .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final MaterialAutoCompleteTextView inputKey = (MaterialAutoCompleteTextView) layout.findViewById(R.id.inputKey);
                        final MaterialEditText inputValue = (MaterialEditText) layout.findViewById(R.id.inputValue);

                        String key = inputKey.getText().toString();
                        String value = inputValue.getText().toString();

                        try {
                            preferences.importKeyValue(key, value);
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
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setView(layout);
        builder.show();

        // Set autocomplete items
        MaterialAutoCompleteTextView view = layout.findViewById(R.id.inputKey);
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

    public String getExportString() throws IOException {
        MessageConfiguration message = preferences.exportToMessage();
        message.setWaypoints(waypointsRepo.exportToMessage());
        return parser.toJsonPlain(message);

    }

    public static class ExportTask extends AsyncTask<Void, Void, Boolean> {
        WeakReference<EditorActivity> ref;
        ExportTask(EditorActivity activity) {
            ref = new WeakReference<>(activity);
        }
        @Override
        protected Boolean doInBackground(Void... voids) {
            String exportStr;
            try {
                exportStr = ref.get().getExportString();
            } catch (IOException e) {
                return false;
            }
            if(ref.get() != null)
                ref.get().exportConfigurationToFile(exportStr);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(ref.get() != null) {
                if (success) {
                    ref.get().displayExportToFileSuccessful();
                } else {
                    ref.get().displayExportToFileSuccessful();
                }
            }
        }
    }
}