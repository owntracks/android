package org.owntracks.android.support;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class DebugFileLogTree extends Timber.DebugTree {


        private Context context;

        public DebugFileLogTree(Context context) {
            this.context = context;
        }

        @SuppressLint("LogNotTimber")
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {

            try {

                File direct = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/owntracks_debug");

                if (!direct.exists()) {
                    direct.mkdir();
                }

                String fileNameTimeStamp = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
                String logTimeStamp = new SimpleDateFormat("E MMM dd yyyy 'at' hh:mm:ss:SSS aaa", Locale.getDefault()).format(new Date
                        ());

                String fileName = fileNameTimeStamp + ".html";

                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/owntracks_debug" + File.separator + fileName);

                file.createNewFile();

                if (file.exists()) {

                    OutputStream fileOutputStream = new FileOutputStream(file, true);

                    fileOutputStream.write(("<p style=\"background:lightgray;\"><strong style=\"background:lightblue;\">&nbsp&nbsp" + logTimeStamp + " :&nbsp&nbsp</strong>&nbsp&nbsp" + message + "</p>").getBytes());
                    fileOutputStream.close();

                }

                //if (context != null)
                //MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);

            } catch (Exception e) {
                Log.e("DebugFileLogTree", "Error while logging into file : " + e);
            }

        }

    @Override
    protected String createStackElementTag(@NonNull StackTraceElement element) {
        return super.createStackElementTag(element) + "/" + element.getMethodName() + "/" + element.getLineNumber();
    }

}
