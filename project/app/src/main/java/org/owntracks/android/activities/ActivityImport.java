package org.owntracks.android.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.owntracks.android.R;
import org.owntracks.android.messages.MessageConfiguration;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.Parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import timber.log.Timber;

public class  ActivityImport extends ActivityBase {
    private static final String TAG = "ActivityImport";
    public static final int REQUEST_CODE = 1;

    private TextView input;
    private MessageConfiguration configJSON = null;
    private MenuItem saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        setSupportToolbar();

        input = (TextView) findViewById(R.id.input);

        Log.v(TAG, "checking for import intent");
        // Look for Import Preference File Intent
        final Intent intent = getIntent();
        final String action = intent.getAction();
        Log.v(TAG, "action: " + intent.getAction());


        if(Intent.ACTION_VIEW.equals(action)) {
            Log.v(TAG, "action ok, getting data uri");

            Uri uri = intent.getData();
            Log.v(TAG, "uri: " + uri);

            if (uri != null) {
                extractPreferences(uri);

            }
        } else {
            Intent pickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
            pickerIntent.addCategory(Intent.CATEGORY_OPENABLE);
            pickerIntent.setType("*/*");

            try {
                startActivityForResult(Intent.createChooser(pickerIntent, "Select a file"), ActivityImport.REQUEST_CODE);
            } catch (android.content.ActivityNotFoundException ex) {
                // Potentially direct the user to the Market with a Dialog
            }

        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
        Log.v(TAG, "onActivityResult: RequestCode: " + requestCode + " resultCode: " + resultCode);
        switch(requestCode) {
            case ActivityImport.REQUEST_CODE: {
                if(resultCode == RESULT_OK) {
                    extractPreferences(resultIntent.getData());
                } else {
                    finish();
                }


                break;
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_export, menu);

        saveButton = menu.findItem(R.id.save);
        tintMenu();
        return true;
    }

    private void tintMenu() {
        if(saveButton != null) {
            saveButton.setEnabled(configJSON != null);
            saveButton.getIcon().setAlpha(configJSON != null ? 255 : 130);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                importAction();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void importAction() {
        Log.v(TAG, "Importing configuration. Brace for impact.");
        Preferences.importFromMessage(configJSON);

       // importPreferenceResultDialog("Success", "Preferences import successful.\nIt is recommended to restart the app.");
        Snackbar s = Snackbar.make(findViewById(R.id.frame), R.string.snackbarImportCompleted, Snackbar.LENGTH_LONG);
        s.setAction("Restart", new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e(TAG, "restarting app");
                Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0, i, 0);
                AlarmManager manager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                manager.set(AlarmManager.RTC, System.currentTimeMillis() + 1, intent);
                System.exit(2);

            }
        });
        s.show();


    }

    private void extractPreferences(Uri uri){

        try{
            BufferedReader r;
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                // Note: left here to avoid breaking compatibility.  May be removed
                // with sufficient testing. Will not work on Android >5 without granting READ_EXTERNAL_STORAGE permission
                Timber.v("using file:/ uri");
                r  = new BufferedReader(new InputStreamReader(new FileInputStream(uri.getPath())));
            } else {
                Timber.v("using content:/ uri");
                InputStream stream =  getContentResolver().openInputStream(uri);
                r = new BufferedReader(new InputStreamReader(stream));
            }

            StringBuilder total = new StringBuilder();

            try {
                String content;
                while ((content = r.readLine()) != null) {
                    total.append(content);
                }
            } catch (OutOfMemoryError e) {
                throw new Error("Unable to load content into memory");
            } catch (Exception e ) {
                e.printStackTrace();
            }

            Log.v(TAG, "file content: " + total);

            configJSON= (MessageConfiguration) Parser.deserializeSync(total.toString().getBytes());
            if(configJSON == null) {
                throw new Error("Unable to parse content");
            }



            input.setText(formatString(Parser.serializeSync(configJSON)));
            tintMenu();
        }catch(Exception e){
            Timber.e(e, "import exception ");
            finish();
            Toast.makeText(this, getString(R.string.errorPreferencesImportFailed), Toast.LENGTH_SHORT).show();

        }

    }

    private static String formatString(String text){

        StringBuilder json = new StringBuilder();
        String indentString = "";

        for (int i = 0; i < text.length(); i++) {
            char letter = text.charAt(i);
            switch (letter) {
                case '{':
                case '[':
                    json.append("\n").append(indentString).append(letter).append("\n");
                    indentString = indentString + "\t";
                    json.append(indentString);
                    break;
                case '}':
                case ']':
                    indentString = indentString.replaceFirst("\t", "");
                    json.append("\n").append(indentString).append(letter);
                    break;
                case ',':
                    json.append(letter).append("\n").append(indentString);
                    break;

                default:
                    json.append(letter);
                    break;
            }
        }

        return json.toString();
    }

}
