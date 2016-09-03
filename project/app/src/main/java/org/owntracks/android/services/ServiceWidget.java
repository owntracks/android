package org.owntracks.android.services;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.appwidget.AppWidgetManager;

import org.owntracks.android.App;
import org.owntracks.android.widget.LClocWidgetProvider;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.WaypointPair;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.R;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.support.MessageWaypointCollection;
import org.owntracks.android.activities.ActivityMap;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.util.List;
import java.util.ArrayList;

public class ServiceWidget extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private List<FusedContact> mBuzzes = new ArrayList<>();
    private Context mContext;
    private int mAppWidgetId;
    private static final String TAG = "StackRemoteViewsFactory";
    private Bitmap background;
    
    public StackRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        background = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_linear_scale_black);
    }

    public void onCreate() {
        Drawable d = mContext.getResources().getDrawable(R.drawable.ic_linear_scale_black);
        background = ContactImageProvider.drawableToBitmap(d);
    }

    public void onDestroy() {
        mBuzzes.clear();
    }

    public int getCount() {
        return mBuzzes.size();
    }

    public RemoteViews getViewAt(int position) {
        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.widget_stack_item);
        if (getCount() > 0 && position <= getCount()) {
            FusedContact c = mBuzzes.get(position);
            MessageLocation loc = c.getMessageLocation();
            MessageWaypointCollection wayps = App.getContactWaypoints(c);
            WaypointPair currWayp;
            int progress;

            if (wayps != null)
                currWayp = WaypointPair.getWaypointPair(loc, wayps);
            else
                currWayp = null;

            if (currWayp != null) {
                progress = currWayp.getProgress() / 10; // Reduce % to 1-9 range
                setImageForPlace(remoteViews, R.id.widget_from_view, currWayp.getFromLoc());
                setImageForPlace(remoteViews, R.id.widget_to_view, currWayp.getToLoc());
                setImageForProgress(remoteViews, c, progress); // Set new location
                remoteViews.setTextViewText(R.id.widget_item_wayps,
                                            currWayp.getFromLoc() + " - " + currWayp.getToLoc());
            } else {
                String msg;
                setImageForPlace(remoteViews, R.id.widget_from_view, "-");
                setImageForPlace(remoteViews, R.id.widget_to_view, "-");
                setImageForProgress(remoteViews, c, 5); // Set new location
                if (wayps != null && wayps.size() < 2)
                    msg = mContext.getResources().getString(R.string.no_waypoints);
                else
                    msg = mContext.getResources().getString(R.string.out_of_waypoints);
                remoteViews.setTextViewText(R.id.widget_item_wayps, msg);
            }

            String itemName = null; //c.getFusedLocation();
            if (itemName == null)
                itemName = c.getFusedName();
            
            remoteViews.setTextViewText(R.id.widget_item_name, itemName);

            // Fill-in intent to launch map
            Bundle extras = new Bundle();
            extras.putInt(ActivityMap.INTENT_KEY_ACTION, ActivityMap.ACTION_FOLLOW_CONTACT);
            extras.putString(ActivityMap.INTENT_KEY_TOPIC, c.getId());
            Intent fillInIntent = new Intent();
            fillInIntent.putExtras(extras);
            remoteViews.setOnClickFillInIntent(R.id.stack_widget_item, fillInIntent);
        }
        
        return remoteViews;
    }

    private void setImageForPlace(RemoteViews rv, int viewId, String place) {
        // Code from ContactImageProvider.getBitmapFromCache(), but not contact specific
        Bitmap d;
        d = ContactImageProvider.drawableToBitmap(TextDrawable.builder()
                                                  .beginConfig()
                                                  .withBorder(5)
                                                  .endConfig()
                                                  .buildRoundRect(String.valueOf(place.charAt(0)).toUpperCase(),
                                                                  ColorGenerator.MATERIAL.getColor(place),
                                                                  PLACE_DIMENSIONS));
        rv.setImageViewBitmap(viewId, d);
    }

    // progress is between 0 and 9, specifying position for contact image
    private void setImageForProgress(RemoteViews rv, FusedContact c, int progress) {
        final int canvWidth = (int)ContactImageProvider.convertDpToPixel(120);
        final int canvHeight = (int)ContactImageProvider.convertDpToPixel(28);
        final int itemWidth = background.getWidth(); //(int)ContactImageProvider.convertDpToPixel(40);
        final int itemHeight = background.getHeight(); //(int)ContactImageProvider.convertDpToPixel(40);
        final int centerY = (canvHeight / 2) - (itemHeight / 2);
        
        Bitmap contact = ContactImageProvider.getBitmapFromCache(c);
        Bitmap full = Bitmap.createBitmap(canvWidth, canvHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(full);

        Rect ref = new Rect(0, 0, itemWidth, itemHeight);
        Rect dest = new Rect(ref);
        Paint paint = new Paint();
        int forward = (int)ContactImageProvider.convertDpToPixel(progress * 9) + 10;

        paint.setColor(0xff424242);
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        
        // Replicate background
        for (int i = 0; i < 9; i++) {
            int shift = i * (itemWidth / 2);
            Rect dup = new Rect(ref);
            dup.offsetTo(shift, centerY);
            canvas.drawBitmap(background, null, dup, null);
        }

        // Draw progress
        dest.offsetTo(forward, centerY);
        canvas.drawBitmap(contact, null, dest, null);
        // Set image
        rv.setImageViewBitmap(R.id.widget_img_view, full);
    }

    private void unsetImageForProgress(RemoteViews rv) {
        // Clear all grids
    }
    
    public RemoteViews getLoadingView() {
        return null;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        ArrayList <FusedContact> contacts = new ArrayList<FusedContact>(App.getFusedContacts().values());

        mBuzzes.clear();
        // Add contacts who have waypoints defined
        for (FusedContact c : contacts) {
            MessageWaypointCollection wayps = App.getContactWaypoints(c);
            if (wayps != null) {
                if (wayps.size() > 1) {
                    mBuzzes.add(c);
                }
            }
        }
    }

    private static final int PLACE_DIMENSIONS = (int)ContactImageProvider.convertDpToPixel(5);
}
