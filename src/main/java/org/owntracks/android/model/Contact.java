package org.owntracks.android.model;

import java.io.InputStream;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.ServiceProxy;
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
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

public class Contact {

	private int uid;
	private String name;
	private String topic;
    private String trackerId;
    private GeocodableLocation location;
	private Bitmap userImage;
    private BitmapDescriptor userImageDescriptor;
	private static final int userImageHeightScale = (int) convertDpToPixel(48);
    private static Bitmap defaultUserImage = getRoundedShape(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(ServiceProxy
					.getInstance().getResources(), R.drawable.noimage),
					userImageHeightScale, userImageHeightScale, true));
    private static BitmapDescriptor defaultUserImageDescriptor = BitmapDescriptorFactory.fromBitmap(defaultUserImage);

	private Marker marker;
	private View view;
	private Drawable imageDrawable;
    private Uri lookupURI;

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

	public void updateMarkerPosition() {
		if ((this.marker != null) && (this.location.getLatLng() != null))
			this.marker.setPosition(this.location.getLatLng());
		else
			Log.e(this.toString(),
					"update of marker position requested, but no marker set");
	}

	public int getUid() {
		return this.uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public String getName() {
		return this.name;
	}

    public void setLookupURI(Uri lookupuri) {
        this.lookupURI = lookupuri;
    }

    public Uri getLookupUri(){
        return this.lookupURI;
    }

	public void setName(String name) {
		this.name = name;
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
		location.setTag(this.topic);// to find according contact once geocoder
									// resolving returns
	}

    public boolean isLinked() {
        return this.name != null;
    }

	@Override
	public String toString() {
		if (getName() != null)
			return this.name;
		else
			return this.topic;
	}

	public void setUserImage(Bitmap image) {
		this.userImage = image != null ? getRoundedShape(Bitmap.createScaledBitmap(image, userImageHeightScale, userImageHeightScale, true)) : null;
        this.userImageDescriptor = image != null ? BitmapDescriptorFactory.fromBitmap(userImage) : null;
	}

	public Bitmap getUserImage() {
		return this.userImage != null ? this.userImage : defaultUserImage;
	}

	public BitmapDescriptor getUserImageDescriptor() {
		return this.userImage != null ? userImageDescriptor : defaultUserImageDescriptor;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	private static float convertDpToPixel(float dp) {
		return dp
				* (App.getContext().getResources().getDisplayMetrics().densityDpi / 160f);
	}

	public String getTopic() {
		return this.topic;
	}

	public BitmapDescriptor getMarkerImageDescriptor() {
		return this.userImage != null ? BitmapDescriptorFactory
				.fromBitmap(getUserImage()) : BitmapDescriptorFactory
				.fromBitmap(defaultUserImage);
	}

	public static Bitmap resolveImage(ContentResolver cr, long id) {
		Uri uri = ContentUris.withAppendedId(
				ContactsContract.Contacts.CONTENT_URI, id);
		InputStream input = ContactsContract.Contacts
				.openContactPhotoInputStream(cr, uri);
		if (input == null) {
			return null;
		}
		return BitmapFactory.decodeStream(input);
	}

	private static Bitmap getRoundedShape(Bitmap bitmap) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);
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

	public Drawable getImageDrawable() {
		return imageDrawable;
	}
}
