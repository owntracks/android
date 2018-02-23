package org.owntracks.android.support;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.ArrayMap;
import android.util.Base64;
import android.widget.ImageView;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.App;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.widgets.TextDrawable;

import java.lang.ref.WeakReference;

public class ContactImageProvider {
    private static ContactBitmapMemoryCache memoryCache;
    private static final int FACE_DIMENSIONS = (int) (48 * (App.getContext().getResources().getDisplayMetrics().densityDpi / 160f));


    public void invalidateCacheLevelCard(String key) {
        memoryCache.clearLevelCard(key);
    }


    private static class ContactDrawableWorkerTaskForImageView extends AsyncTask<FusedContact, Void, Bitmap> {
        final WeakReference<ImageView> target;

        ContactDrawableWorkerTaskForImageView(ImageView imageView) {
            target = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(FusedContact... params) {
            return getBitmapFromCache(params[0]);
        }

        protected void onPostExecute(Bitmap result) {
            if(result == null)
                return;

            ImageView imageView = target.get();
            if(imageView != null)
                imageView.setImageBitmap(result);
        }

    }
    private static class ContactDrawableWorkerTaskForMarker extends AsyncTask<FusedContact, Void, BitmapDescriptor> {
        final WeakReference<Marker> target;

        ContactDrawableWorkerTaskForMarker(Marker marker) {
            target = new WeakReference<>(marker);
        }

        @Override
        protected BitmapDescriptor doInBackground(FusedContact... params) {
            return BitmapDescriptorFactory.fromBitmap(getBitmapFromCache(params[0]));
        }

        @Override
        protected void onPostExecute(BitmapDescriptor result) {

            Marker marker = target.get();
            if(marker != null) {
                marker.setIcon(result);
                marker.setVisible(true);
            }
        }
    }


    public void setMarkerAsync(Marker marker, FusedContact contact) {
        (new ContactDrawableWorkerTaskForMarker(marker)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, contact);
    }

    public void setImageViewAsync(ImageView imageView, FusedContact contact) {
        //imageView.setImageDrawable(placeholder);
        (new ContactDrawableWorkerTaskForImageView(imageView)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, contact);
    }


    private static Bitmap getBitmapFromCache(FusedContact contact) {
        Bitmap d;

        if(contact == null)
            return null;



        if(contact.hasCard()) {
            d = memoryCache.getLevelCard(contact.getId());
            if(d != null) {
                return d;
            }

            if(contact.getMessageCard().hasFace()) {
                byte[] imageAsBytes = Base64.decode(contact.getMessageCard().getFace().getBytes(), Base64.DEFAULT);
                d = getRoundedShape(Bitmap.createScaledBitmap(BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length), FACE_DIMENSIONS, FACE_DIMENSIONS, true));
                contact.getMessageCard().setFace(null);
                memoryCache.putLevelCard(contact.getId(), d);
                return d;
            }
        }

        d = memoryCache.getLevelTid(contact.getId());
        if(d != null) {
            return d;
        }
        d = drawableToBitmap(TextDrawable.builder().buildRoundRect(contact.getTrackerId(), TextDrawable.ColorGenerator.MATERIAL.getColor(contact.getId()), FACE_DIMENSIONS));
        memoryCache.putLevelTid(contact.getId(), d);
        return d;
    }

    public ContactImageProvider(){
        memoryCache = new ContactBitmapMemoryCache();
        App.getEventBus().register(this);
    }

    private static class ContactBitmapMemoryCache {
        private final ArrayMap<String, Bitmap> cacheLevelCard;
        private final ArrayMap<String, Bitmap> cacheLevelTid;

        ContactBitmapMemoryCache() {
            cacheLevelCard = new ArrayMap<>();
            cacheLevelTid = new ArrayMap<>();
        }

        synchronized void putLevelCard(String key, Bitmap value) {
            cacheLevelCard.put(key, value);
            cacheLevelTid.remove(key);
        }
        synchronized void putLevelTid(String key, Bitmap value) {
            cacheLevelTid.put(key, value);
        }
        synchronized Bitmap getLevelCard(String key) {
            return cacheLevelCard.get(key);
        }
        synchronized Bitmap getLevelTid(String key) {
            return cacheLevelTid.get(key);
        }
        public synchronized void clear() {
            cacheLevelCard.clear();
            cacheLevelTid.clear();
        }
        synchronized void clearLevelCard(String key) {
            cacheLevelCard.remove(key);
        }
    }

    private void invalidateCache() {
        memoryCache.clear();
    }
    private static Bitmap getRoundedShape(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = bitmap.getWidth();

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : FACE_DIMENSIONS;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : FACE_DIMENSIONS;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Events.ModeChanged e) {
        invalidateCache();
    }
}
