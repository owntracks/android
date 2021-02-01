package org.owntracks.android.support;

import android.content.Context;
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
import android.util.Base64;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.databinding.BindingAdapter;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

import org.owntracks.android.injection.qualifier.AppContext;
import javax.inject.Singleton;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.widgets.TextDrawable;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import timber.log.Timber;

@Singleton
public class ContactImageProvider {
    private static ContactBitmapMemoryCache memoryCache;
    private static int FACE_DIMENSIONS;


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
            if (result == null)
                return;

            ImageView imageView = target.get();
            if (imageView != null)
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
            if (marker != null) {
                try {
                    marker.setIcon(result);
                    marker.setVisible(true);
                } catch (IllegalArgumentException e) {
                    Timber.e(e, "Error setting marker icon");
                }
            }
        }
    }

    public void setMarkerAsync(Marker marker, FusedContact contact) {
        (new ContactDrawableWorkerTaskForMarker(marker)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, contact);
    }

    public static void setImageViewAsync(ImageView imageView, FusedContact contact) {
        //imageView.setImageDrawable(placeholder);
        (new ContactDrawableWorkerTaskForImageView(imageView)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, contact);
    }

    @Nullable
    private static Bitmap getBitmapFromCache(@Nullable FusedContact contact) {
        Bitmap d;

        if (contact == null)
            return null;

        if (contact.hasCard()) {
            d = memoryCache.getLevelCard(contact.getId());
            if (d != null) {
                return d;
            }

            if (contact.getMessageCard().hasFace()) {
                byte[] imageAsBytes = Base64.decode(contact.getMessageCard().getFace().getBytes(), Base64.DEFAULT);
                Bitmap b = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);

                if (b == null) {
                    Timber.e("Decoding card bitmap failed");
                    Bitmap fallbackBitmap = Bitmap.createBitmap(FACE_DIMENSIONS, FACE_DIMENSIONS, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(fallbackBitmap);
                    Paint paint = new Paint();
                    paint.setColor(0xFFFFFFFF);
                    canvas.drawRect(0F, 0F, (float) FACE_DIMENSIONS, (float) FACE_DIMENSIONS, paint);
                    d = getRoundedShape(fallbackBitmap);
                } else {
                    d = getRoundedShape(Bitmap.createScaledBitmap(b, FACE_DIMENSIONS, FACE_DIMENSIONS, true));
                    memoryCache.putLevelCard(contact.getId(), d);
                }
                return d;
            }
        }

        TidBitmap td = memoryCache.getLevelTid(contact.getId());
        // if cache doesn't contain a bitmap for a contact or if the cached bitmap was for an old tid, create a new one and cache it
        if (td == null || !td.isBitmapFor(contact.getTrackerId())) {
            td = new TidBitmap(contact.getTrackerId(), drawableToBitmap(TextDrawable.builder().buildRoundRect(contact.getTrackerId(), TextDrawable.ColorGenerator.MATERIAL.getColor(contact.getId()), FACE_DIMENSIONS)));
            memoryCache.putLevelTid(contact.getId(), td);
        }

        return td.getBitmap();
    }

    @Inject
    public ContactImageProvider(@AppContext Context context) {
        memoryCache = new ContactBitmapMemoryCache();
        FACE_DIMENSIONS = (int) (48 * (context.getResources().getDisplayMetrics().densityDpi / 160f));
    }

    private static class ContactBitmapMemoryCache {
        private final ArrayMap<String, Bitmap> cacheLevelCard;
        private final ArrayMap<String, TidBitmap> cacheLevelTid;

        ContactBitmapMemoryCache() {
            cacheLevelCard = new ArrayMap<>();
            cacheLevelTid = new ArrayMap<>();
        }

        synchronized void putLevelCard(String key, Bitmap value) {
            cacheLevelCard.put(key, value);
            cacheLevelTid.remove(key);
        }

        synchronized void putLevelTid(String key, TidBitmap value) {
            cacheLevelTid.put(key, value);
        }

        synchronized Bitmap getLevelCard(String key) {
            return cacheLevelCard.get(key);
        }

        synchronized TidBitmap getLevelTid(String key) {
            return cacheLevelTid.get(key);
        }

        synchronized void clear() {
            cacheLevelCard.clear();
            cacheLevelTid.clear();
        }

        synchronized void clearLevelCard(String key) {
            cacheLevelCard.remove(key);
        }

    }

    public void invalidateCache() {
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
            return ((BitmapDrawable) drawable).getBitmap();
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

    @BindingAdapter({"imageProvider", "contact"})
    public static void displayFaceInViewAsync(ImageView view, Integer imageProvider, FusedContact c) {
        setImageViewAsync(view, c);
    }
}
