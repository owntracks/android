package org.owntracks.android.model;

import java.io.InputStream;
import java.lang.ref.WeakReference;

import org.owntracks.android.App;
import org.owntracks.android.support.Preferences;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Base64;
import android.view.View;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.mapbox.mapboxsdk.overlay.Marker;

public class Contact {


    private static final String TAG = "Contact";

    private int uid;
    private String topic;
    private String trackerId;
    private GeocodableLocation location;
    private Uri linkLookupURI;
    private WeakReference<Marker> marker;
    private View view;

    private String cardName;
    private Bitmap cardFace;


	private static final int FACE_HEIGHT_SCALE = (int) convertDpToPixel(48);

    public Contact(String topic) {
		this.topic = topic;
    }


	public View getView() {
		return this.view;
	}

	public void setView(View view) {
		this.view = view;
	}

	public void setMarker(Marker marker) {
		this.marker = new WeakReference<>(marker);
	}

	public Marker getMarker() {
        return this.marker != null ? this.marker.get() : null;
	}

    public String getDisplayName() {

        if(getCardName() != null & getCardName() != "")
            return getCardName();

        if(getTrackerId() != null && getTrackerId() != "")
            return "Device-"+getTrackerId();

        return getTopic();
    }

    public String getCardName() {
        return this.cardName;
    }

    public void setLinkLookupURI(Uri lookupuri) {
        this.linkLookupURI = lookupuri;
    }

    public Uri getLinkLookupUri(){
        return this.linkLookupURI;
    }

    public void setCardName(String name) {
        this.cardName = name;
    }

    public String getTrackerId() {
        return trackerId;
    }

    public void setTrackerId(String tid) {
        this.trackerId = tid;
    }

    public GeocodableLocation getLocation() {
		return this.location;
	}


	public void setLocation(GeocodableLocation location) {
		this.location = location;
		location.setTag(this.topic);// to find according contact once geocoder  resolving returns
	}

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }


    public String getCommandTopic() {
        return this.topic+ Preferences.getPubTopicCommandsPart();
    }

    @Override
	public String toString() {
        return getDisplayName();
	}

    public void setCardFace(String base64Image) {
        if(base64Image == null)
            this.cardFace = null;
        else {
            byte[] imageAsBytes = Base64.decode(base64Image.getBytes(), Base64.DEFAULT);
            this.cardFace = getRoundedFace(BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length));
        }
    }

    private static Bitmap getRoundedFace(Bitmap image) {
        return getRoundedShape(Bitmap.createScaledBitmap(image, FACE_HEIGHT_SCALE, FACE_HEIGHT_SCALE, true));
    }

    public Drawable getFaceDrawable(Context c) {
        return new BitmapDrawable(c.getResources() ,getFace());
       //return new BitmapDrawable(c.getResources(), drawableToBitmap(TextDrawable.builder().buildRoundRect(getTrackerId(), ColorGenerator.MATERIAL.getColor(topic), FACE_HEIGHT_SCALE)));
       // return TextDrawable.builder().buildRoundRect(getTrackerId(), ColorGenerator.MATERIAL.getColor(topic), FACE_HEIGHT_SCALE);
    }




	public Bitmap getFace() {
        if (this.cardFace != null) {
            return this.cardFace;
        }

        return drawableToBitmap(TextDrawable.builder().buildRoundRect(getTrackerId(), ColorGenerator.MATERIAL.getColor(topic), FACE_HEIGHT_SCALE));

	}


    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : FACE_HEIGHT_SCALE;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : FACE_HEIGHT_SCALE;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }


	private static float convertDpToPixel(float dp) {
		return dp * (App.getContext().getResources().getDisplayMetrics().densityDpi / 160f);
	}

	public static Bitmap resolveImage(ContentResolver cr, long id) {
		Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
		InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
		if (input == null) {
			return null;
		}
		return BitmapFactory.decodeStream(input);
	}

	private static Bitmap getRoundedShape(Bitmap bitmap) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
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

		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}




}
