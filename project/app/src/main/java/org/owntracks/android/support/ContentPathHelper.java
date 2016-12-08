package org.owntracks.android.support;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

// Credit goes to Paul Burke and his library aFileChooser (https://github.com/iPaulPro/aFileChooser)
public class ContentPathHelper {

    public static String uriToFilename(Context c, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = c.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

}
