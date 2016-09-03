package org.owntracks.android.widget;

import android.content.Intent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.widget.RemoteViews;
import android.content.Context;
import android.content.ComponentName;
import android.util.Log;
import android.net.Uri;
import android.os.Bundle;
import android.app.PendingIntent;

import org.owntracks.android.R;
import org.owntracks.android.services.ServiceWidget;

import org.owntracks.android.model.FusedContact;
import org.owntracks.android.activities.ActivityMap;

public class LClocWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "LClocWidgetProvider";
    private Context context;
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {

        this.context = context;

        for (int i = 0; i < appWidgetIds.length; ++i) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_stack);
            //Log.d("LClocWidgetProvider", "Updating widget " + i);
            // set intent for widget service that will create the views
            Intent serviceIntent = new Intent(context, ServiceWidget.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))); // embed extras so they don't get ignored
            remoteViews.setRemoteAdapter(R.id.stack_widget_view, serviceIntent);
            remoteViews.setEmptyView(R.id.stack_widget_view, R.id.stack_widget_empty_view);
            
            // set intent for item click (opens map activity)
            Intent viewIntent = new Intent(context, ActivityMap.class);
            viewIntent.setAction(Intent.ACTION_VIEW);
            viewIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            viewIntent.setData(Uri.parse(viewIntent.toUri(Intent.URI_INTENT_SCHEME)));
            
            PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);
            remoteViews.setPendingIntentTemplate(R.id.stack_widget_view, viewPendingIntent);
            
            // update widget
            appWidgetManager.updateAppWidget(appWidgetIds[i], remoteViews);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}
