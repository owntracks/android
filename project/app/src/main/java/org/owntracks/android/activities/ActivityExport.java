package org.owntracks.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeWriter;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;

public class ActivityExport extends ActivityBase {
    private static final String TAG = "ActivityExport";

    private static final String TEMP_FILE_NAME = "config.otrc";
    private static Context context;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_export);

        setupSupportToolbar();
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        toolbar = (Toolbar)findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);

        getFragmentManager().beginTransaction().replace(R.id.content_frame, new FragmentPreferencesExport(), "exportOreferences").commit();

    }

    public static class FragmentPreferencesExport extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Activity a = getActivity();

            addPreferencesFromResource(R.xml.export);

            findPreference("exportToFile").setOnPreferenceClickListener(exportToFile);
            findPreference("exportToQRCode").setOnPreferenceClickListener(exportToQRCode);
            findPreference("exportWaypointsToEndpoint").setOnPreferenceClickListener(exportWaypointsToEndpoint);
            findPreference("importFromFile").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    Intent intent = new Intent(context, ActivityImport.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    intent.putExtra(ActivityBase.DISABLES_ANIMATION, true);
                    startActivity(intent);
                    return true;
                }
            });
            findPreference("importFromQRCode").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    IntentIntegrator integrator = new IntentIntegrator(FragmentPreferencesExport.this);
                    integrator.initiateScan();
                    return true;
                }
            });

        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == IntentIntegrator.REQUEST_CODE) {
                try {
                    IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                    Log.i(TAG, "QR code read: " + result.getContents());
                    if (!BarcodeFormat.QR_CODE.name().equals(result.getFormatName())) {
                        Toast.makeText(context, R.string.qrcode_error, Toast.LENGTH_SHORT).show();
                    } else {
                        String contents = result.getContents();
                        File outputDir = context.getCacheDir(); // context being the Activity pointer
                        File outputFile = File.createTempFile("qrcode-import", "json", outputDir);
                        OutputStreamWriter fout = new OutputStreamWriter(new FileOutputStream(outputFile));
                        try {
                            fout.write(contents);
                        } catch (IOException ex) {
                            Log.e(TAG, "Unable to write temp file", ex);
                            return;
                        } finally {
                            try {
                                fout.close();
                            } catch (IOException ex) {
                                // ignore
                            }
                        }
                        Log.i(TAG, "Importing URL: " + outputFile.toString());
                        Intent intent = new Intent(context, ActivityImport.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        intent.putExtra(ActivityBase.DISABLES_ANIMATION, true);
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setData(Uri.fromFile(outputFile));
                        startActivity(intent);
                    }
                    Log.i(TAG, "Clearing barcode");
                } catch (Exception e) {
                    Log.e(TAG, "Error calling barcode scanner onBarcodeScanResult: " + e);
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }

    }

    private static String generateExportString() {
        String exportStr;
        try {
            exportStr = Parser.serializeSync(Preferences.exportToMessage());
        } catch (IOException e) {
            Toast.makeText(context, R.string.preferencesExportFailed, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return null;
        } catch (Parser.EncryptionException e) {
            e.printStackTrace();
            return null;
        }

        Log.v("Export", "Config: \n" + exportStr);
        return exportStr;
    }



    private static Preference.OnPreferenceClickListener exportToFile = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {

            String exportStr = generateExportString();
            if(exportStr == null)
                return false;

            File cDir = App.getInstance().getBaseContext().getCacheDir();
            File tempFile = new File(cDir.getPath() + "/" + TEMP_FILE_NAME) ;

            try {
                FileWriter writer = new FileWriter(tempFile);

                writer.write(exportStr);
                writer.close();

                Log.v(TAG, "Saved temporary config file for exportToMessage to " + tempFile.getPath());

            } catch (IOException e) {
                e.printStackTrace();
            }
            Uri configUri = FileProvider.getUriForFile(App.getInstance(), "org.owntracks.android.fileprovider", tempFile);

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, configUri);
            sendIntent.setType("text/plain");

            context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.exportConfiguration)));
            Toast.makeText(context, R.string.preferencesExportSuccess, Toast.LENGTH_SHORT).show();

            return false;
        }
    };

    private static Preference.OnPreferenceClickListener exportToQRCode = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final String exportStr = generateExportString();
            if(exportStr == null)
                return false;

            new MaterialDialog.Builder(context)
                    .customView(R.layout.activity_export_qrcode, true)
                    .title(R.string.export_qrcode_title)
                    .positiveText(R.string.dialog_close)
                    .showListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialog) {
                            Toast.makeText(context, R.string.preferencesExportQRCodeGenerating, Toast.LENGTH_SHORT).show();
                            MaterialDialog d = MaterialDialog.class.cast(dialog);
                            View view = d.findViewById(R.id.qrcode_layout);
                            int size = view.getWidth();
                            view.setMinimumHeight(size);
                            new QRCodeWorkerTask((ImageView) d.findViewById(R.id.qrCode))
                                    .execute(new QRCodeWorkerParams(size, exportStr));
                        }
                    })
                    .show();
            return false;
        }
    };

    private static Preference.OnPreferenceClickListener exportWaypointsToEndpoint = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {

            ServiceProxy.runOrBind(context, new Runnable() {
                @Override
                public void run() {
                    if(ServiceProxy.getServiceApplication().publishWaypointsMessage()) {
                        Toast.makeText(context, R.string.preferencesExportQueued, Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(context, R.string.preferencesExportFailed, Toast.LENGTH_SHORT).show();

                    }
                }
            });
            return false;
        }
    };


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    static class QRCodeWorkerParams {
        int width;
        String exportStr;

        public QRCodeWorkerParams(int width, String exportStr) {
            this.width = width;
            this.exportStr = exportStr;
        }
    }

    static class QRCodeWorkerTask extends AsyncTask<QRCodeWorkerParams, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        public QRCodeWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(QRCodeWorkerParams... params) {
            int size = params[0].width;
            String exportStr = params[0].exportStr;
            QRCodeWriter writer = new QRCodeWriter();
            try {
                Log.i(TAG, "Building QR code from: " + exportStr);
                BitMatrix bitMatrix = writer.encode(exportStr, BarcodeFormat.QR_CODE, size, size);
                int width = bitMatrix.getWidth();
                int height = bitMatrix.getHeight();
                Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                    }
                }
                return bmp;
            } catch (WriterException e) {
                e.printStackTrace();
                return null;
            }
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

}
