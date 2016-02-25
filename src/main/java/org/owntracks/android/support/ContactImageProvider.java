package org.owntracks.android.support;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
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
import android.util.Log;
import android.widget.ImageView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

import org.owntracks.android.App;
import org.owntracks.android.model.FusedContact;
import java.lang.ref.WeakReference;

public class ContactImageProvider {
    private static final String TAG = "ContactImageProvider";
    private static ContactBitmapMemoryCache memoryCache;
    private static BitmapDrawable placeholder;


    public static void invalidateCacheLevelCard(String key) {
        memoryCache.clearLevelCard(key);
    }
    private  static abstract class ContactBitmapWorkerTask extends AsyncTask<FusedContact, Void, Bitmap> {

        public ContactBitmapWorkerTask() {
        }


        @Override
        protected Bitmap doInBackground(FusedContact... params) {

            Bitmap d;
            FusedContact contact = params[0];
            if(contact == null)
                return null;
                //throw new RuntimeException("no contact provided to ContactImageProvider");



            if(contact.hasCard()) {
                d = memoryCache.getLevelCard(contact.getTopic());
                if(d != null) {
                    return d;
                }

                if(contact.getMessageCard().hasFace()) {
                    byte[] imageAsBytes = Base64.decode(contact.getMessageCard().getFace().getBytes(), Base64.DEFAULT);
                    d = getRoundedShape(Bitmap.createScaledBitmap(BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length), FACE_DIMENSIONS, FACE_DIMENSIONS, true));
                    contact.getMessageCard().setFace(null);
                    memoryCache.putLevelCard(contact.getTopic(), d);
                    return d;
                }
            }

            d = memoryCache.getLevelTid(contact.getTopic());
            if(d != null) {
                return d;
            }
            d = drawableToBitmap(TextDrawable.builder().buildRoundRect(contact.getTrackerId(), ColorGenerator.MATERIAL.getColor(contact.getTopic()), FACE_DIMENSIONS));
            memoryCache.putLevelTid(contact.getTopic(), d);
            return d;
        }

        protected abstract void onPostExecute(Bitmap result);

    }
    private static class ContactDrawableWorkerTaskForImageView extends ContactBitmapWorkerTask {
        final WeakReference<ImageView> target;

        public ContactDrawableWorkerTaskForImageView(ImageView imageView) {
            target = new WeakReference<>(imageView);
        }

        protected void onPostExecute(Bitmap result) {
            if(result == null)
                return;

            ImageView imageView = target.get();
            if(imageView != null)
                imageView.setImageBitmap(result);
        }

    }
    private static class ContactDrawableWorkerTaskForMarker extends ContactBitmapWorkerTask {
        Marker target;

        public ContactDrawableWorkerTaskForMarker(Marker marker) {
            Log.v(TAG, "setting new weak reference for: " + marker);
            target = marker;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            Log.v(TAG, "ContactDrawableWorkerTaskForMarker onPostExecute() for marker: " + target);

            Marker marker = target;
            if(marker != null) {
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(result));
            }
        }
    }


    public static void setMarkerAsync(Marker marker, FusedContact contact) {
        Log.v(TAG, "setMarkerAsync() for " + contact + " and marker " + marker);
        (new ContactDrawableWorkerTaskForMarker(marker)).execute(contact);
    }

    public static void setImageViewAsync(ImageView imageView, FusedContact contact) {
        imageView.setImageDrawable(placeholder);
        (new ContactDrawableWorkerTaskForImageView(imageView)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, contact);
    }



    public static void initialize(Context c){
        memoryCache = new ContactBitmapMemoryCache();

        Rect rect = new Rect(0, 0, 1, 1);
        Bitmap image = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        int color = Color.argb(0, 0, 0, 255);
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawBitmap(image, 0, 0, paint);
        placeholder = new BitmapDrawable(c.getResources(), image);

    }

    private static class ContactBitmapMemoryCache {
        private ArrayMap<String, Bitmap> cacheLevelCard;
        private ArrayMap<String, Bitmap> cacheLevelTid;

        public ContactBitmapMemoryCache() {
            cacheLevelCard = new ArrayMap<>();
            cacheLevelTid = new ArrayMap<>();
        }

        public void putLevelCard(String key, Bitmap value) {
            cacheLevelCard.put(key, value);
            cacheLevelTid.remove(key);
        }
        public void putLevelTid(String key, Bitmap value) {
            cacheLevelTid.put(key, value);
        }
        public Bitmap getLevelCard(String key) {
            return cacheLevelCard.get(key);
        }
        public Bitmap getLevelTid(String key) {
            return cacheLevelTid.get(key);
        }
        public void clear() {
            cacheLevelCard.clear();
            cacheLevelTid.clear();
        }
        public void clearLevelCard(String key) {
            cacheLevelCard.remove(key);

        }
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

    private static final int FACE_DIMENSIONS = (int) convertDpToPixel(48);

    private static float convertDpToPixel(float dp) {
        return dp * (App.getContext().getResources().getDisplayMetrics().densityDpi / 160f);
    }

}
