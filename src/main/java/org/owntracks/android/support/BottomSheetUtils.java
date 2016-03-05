package org.owntracks.android.support;

import android.support.design.widget.BottomSheetBehavior;
import android.view.View;

public class BottomSheetUtils {

    public static void setState(View bottomSheet, int state) {
        BottomSheetBehavior sheetBehavior = BottomSheetBehavior.from(bottomSheet);
        sheetBehavior.setState(state);
    }

    public static void setBottomSheetCallback(View bottomSheet,
                                              BottomSheetBehavior.BottomSheetCallback callback) {
        BottomSheetBehavior sheetBehavior = BottomSheetBehavior.from(bottomSheet);
        sheetBehavior.setBottomSheetCallback(callback);
    }
}
