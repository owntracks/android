package org.owntracks.android.model;

import java.io.InputStream;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.Preferences;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

public class Contact {
	private int uid;
    private String topic;
    private String trackerId;
    private GeocodableLocation location;
    private Uri linkLookupURI;
    private Marker marker;
    private View view;

    private boolean hasLink;
    private String linkName;
    private Bitmap linkFace;

    private String cardName;
    private Bitmap cardFace;


	private static final int faceHeightScale = (int) convertDpToPixel(48);
    private static Bitmap defaultFace = getRoundedFace(BitmapFactory.decodeResource(ServiceProxy.getInstance().getResources(), R.drawable.noimage));


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
		this.marker = marker;
	}

	public Marker getMarker() {
		return this.marker;
	}

    public String getDisplayName() {
        if(getLinkName() != null)
            return getLinkName();

        if(getCardName() != null & getCardName() != "")
            return getCardName();

        if(getTrackerId() != null && getTrackerId() != "")
            return "Device-"+getTrackerId();

        return getTopic();
    }

	public String getLinkName() {
		return this.linkName;
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

	public void setLinkName(String name) {
		this.linkName = name;
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

	public void setLinkFace(Bitmap image) {
        if(image == null)
            this.linkFace = null;
        else
            this.linkFace = getRoundedFace(image);
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
        return getRoundedShape(Bitmap.createScaledBitmap(image, faceHeightScale, faceHeightScale, true));
    }

	public Bitmap getFace() {

        if (this.linkFace != null) {
            Log.v(this.toString(), "using linkFace for  " + this.getTopic());
            return this.linkFace;
        }

        if (this.cardFace != null) {
            Log.v(this.toString(), "using cardFace for  " + this.getTopic());
            return this.cardFace;
        }

        Log.v(this.toString(), "using defaultFace for  " + this.getTopic());
        return defaultFace;
	}

    public BitmapDescriptor getFaceDescriptor() {
        return BitmapDescriptorFactory.fromBitmap(getFace());
    }




	private static float convertDpToPixel(float dp) {
		return dp
				* (App.getContext().getResources().getDisplayMetrics().densityDpi / 160f);
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

    public boolean hasLink() {
        return hasLink;
    }

    public void setHasLink(boolean hasLink) {
        this.hasLink = hasLink;
    }


}
