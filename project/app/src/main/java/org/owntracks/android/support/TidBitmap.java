package org.owntracks.android.support;

import android.graphics.Bitmap;

public class TidBitmap {
    private final String tid;
    private Bitmap bitmap;
    TidBitmap(String tid, Bitmap bitmap) {
        this.bitmap = bitmap;
        this.tid = tid;
    }

    boolean isBitmapFor(String compare) {
        return compare.equals(this.tid);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

}
