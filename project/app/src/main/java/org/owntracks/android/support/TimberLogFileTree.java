package org.owntracks.android.support;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import androidx.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimberLogFileTree extends TimberLogTree {


    private File file;
    @SuppressLint("LogNotTimber")

        public TimberLogFileTree(Context context) {

            try {

                File direct = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/owntracks_debug");

                if (!direct.exists()) {
                    direct.mkdir();
                }

                String fileNameTimeStamp = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

                String fileName = fileNameTimeStamp + ".html";

                file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/owntracks_debug" + File.separator + fileName);
                if (!file.exists()) {
                    file.createNewFile();
                }

            } catch (Exception e) {
                Log.e("TimberLogFileTree", "Error while logging into file : " + e);
            }
        }

        @SuppressLint("LogNotTimber")
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
                super.log(priority, tag, message, t);
                try {

                    if (file.exists()) {
                        OutputStream fileOutputStream = new FileOutputStream(file, true);
                        String logTimeStamp = new SimpleDateFormat("E MMM dd yyyy 'at' hh:mm:ss:SSS aaa", Locale.getDefault()).format(new Date());
                        fileOutputStream.write(("<p style=\"background:lightgray;\"><strong style=\"background:lightblue;\">&nbsp&nbsp" + logTimeStamp + "/" + priority + "/" + tag + " :&nbsp&nbsp</strong>&nbsp&nbsp" +  message + "</p>").getBytes());
                        fileOutputStream.close();
                    }
                } catch (Exception e) {
                    Log.e("TimberLogFileTree", "Error while logging into file : " + e);
                }

         }

}
