package org.owntracks.android.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceGroup;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.owntracks.android.R;
import org.owntracks.android.messages.MessageConfiguration;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.receiver.Parser;

public class ActivityExport extends ActivityBase {
    private static final String TAG = "ActivityExport";

    private static final String TEMP_FILE_NAME = "config.otrc";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);
        Toolbar toolbar = (Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getResources().getString(R.string.export));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_export, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                export();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }



    private void export() {
        MessageConfiguration export = Preferences.export();
        String exportStr;
        try {
            exportStr = Parser.serializeSync(export);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return;
        }
        Log.v("Export", "Config: \n" + exportStr);

        File cDir = getBaseContext().getCacheDir();
        File tempFile = new File(cDir.getPath() + "/" + TEMP_FILE_NAME) ;

        try {
            FileWriter writer = new FileWriter(tempFile);

            writer.write(exportStr.toString());
            writer.close();

            Log.v(TAG, "Saved temporary config file for export to " + tempFile.getPath());

        } catch (IOException e) {
            e.printStackTrace();
        }
        Uri configUri = FileProvider.getUriForFile(this, "org.owntracks.android.fileprovider", tempFile);

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        //sendIntent.putExtra(Intent.EXTRA_TEXT, config.toString());
        sendIntent.putExtra(Intent.EXTRA_STREAM, configUri);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.exportConfiguration)));
    }

}
