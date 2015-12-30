package org.owntracks.android.model;

import java.io.InputStream;
import java.lang.ref.WeakReference;

import org.owntracks.android.App;
import org.owntracks.android.db.ContactLink;
import org.owntracks.android.db.ContactLinkDao;
import org.owntracks.android.db.Dao;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.mapbox.mapboxsdk.overlay.Marker;

import de.greenrobot.dao.query.Query;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.event.EventBus;

public class Contact {


    private static final String TAG = "Contact";

    private int uid;
    private String topic;
    private String trackerId;
    private GeocodableLocation location;
    private Uri linkLookupURI;
    private WeakReference<Marker> marker;
    private View view;

    private boolean hasLink;
    private String linkName;
    private Bitmap linkFace;

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
        return getRoundedShape(Bitmap.createScaledBitmap(image, FACE_HEIGHT_SCALE, FACE_HEIGHT_SCALE, true));
    }

    public Drawable getFaceDrawable(Context c) {
        return new BitmapDrawable(c.getResources() ,getFace());
       //return new BitmapDrawable(c.getResources(), drawableToBitmap(TextDrawable.builder().buildRoundRect(getTrackerId(), ColorGenerator.MATERIAL.getColor(topic), FACE_HEIGHT_SCALE)));
       // return TextDrawable.builder().buildRoundRect(getTrackerId(), ColorGenerator.MATERIAL.getColor(topic), FACE_HEIGHT_SCALE);
    }




	public Bitmap getFace() {

       if (this.linkFace != null) {
            return this.linkFace;
        }

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

    public boolean hasLink() {
        return hasLink;
    }

    public void setHasLink(boolean hasLink) {
        this.hasLink = hasLink;
    }


    /*
	 * Resolves username and image either from a locally saved mapping or from
	 * synced cloud contacts. If no mapping is found, no name is set and the
	 * default image is assumed
	 */
    public static void resolveContact(Context context, Contact c) {

        long contactId = getContactId(c);
        boolean found = false;

        if (contactId <= 0) {
            setContactImageAndName(c, null, null);
            c.setHasLink(false);
            return;
        }

        // Resolve image and name from contact id
        Cursor cursor = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, null,ContactsContract.Data.CONTACT_ID + " = ?", new String[] { contactId + "" }, null);
        if (!cursor.isAfterLast()) {

            while (cursor.moveToNext()) {
                Bitmap image = Contact.resolveImage(context.getContentResolver(), contactId);
                String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));


                setContactImageAndName(c, image, displayName);
                c.setHasLink(true);
                c.setLinkLookupURI(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_LOOKUP_URI, contactId));
                found = true;
                break;
            }
        }

        if (!found) {
            setContactImageAndName(c, null, null);
            c.setHasLink(false);
        }
        cursor.close();

    }

    public static void setContactImageAndName(Contact c, Bitmap image, String name) {
        c.setLinkName(name);
        c.setLinkFace(image);
    }

    private static long getContactId(Contact c) {

        ContactLink cl = queryContactLink( c);
        return cl != null ? cl.getContactId() : 0;
    }
    private static ContactLink queryContactLink(Contact c) {
        QueryBuilder qb = Dao.getContactLinkDao().queryBuilder();

        Query query = qb.where(
                qb.and(
                        ContactLinkDao.Properties.Topic.eq(c.getTopic()),
                        ContactLinkDao.Properties.ModeId.eq(Preferences.getModeId())
                )
        ).build();

        return (ContactLink)query.unique();
    }


    public static void linkContact(Context context, Contact c, long contactId) {
        ContactLink cl = new ContactLink(null, c.getTopic(), contactId, Preferences.getModeId());
        Dao.getContactLinkDao().insertOrReplace(cl);

        resolveContact(context, c);
        EventBus.getDefault().postSticky(new Events.ContactUpdated(c));
    }

    public static void unlinkContact(Contact c) {
        ContactLink cl = queryContactLink(c);
        if(cl != null)
            Dao.getContactLinkDao().delete(cl);
        c.setLinkName(null);
        c.setLinkFace(null);
        c.setHasLink(false);
        EventBus.getDefault().postSticky(new Events.ContactUpdated(c));
    }

}
